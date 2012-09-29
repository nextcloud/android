package com.owncloud.android.files.services;

import java.io.File;
import java.util.AbstractList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import com.owncloud.android.authenticator.AccountAuthenticator;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.PhotoTakenBroadcastReceiver;
import com.owncloud.android.operations.ChunkedUploadFileOperation;
import com.owncloud.android.operations.RemoteOperationResult;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.utils.OwnCloudVersion;

import eu.alefzero.webdav.OnDatatransferProgressListener;

import com.owncloud.android.network.OwnCloudClientUtils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.widget.RemoteViews;

import com.owncloud.android.R;
import eu.alefzero.webdav.WebdavClient;

public class FileUploader extends Service implements OnDatatransferProgressListener {

    public static final String UPLOAD_FINISH_MESSAGE = "UPLOAD_FINISH";
    public static final String EXTRA_PARENT_DIR_ID = "PARENT_DIR_ID";
    public static final String EXTRA_UPLOAD_RESULT = "RESULT";
    public static final String EXTRA_REMOTE_PATH = "REMOTE_PATH";
    public static final String EXTRA_FILE_PATH = "FILE_PATH";
    
    public static final String KEY_LOCAL_FILE = "LOCAL_FILE";
    public static final String KEY_REMOTE_FILE = "REMOTE_FILE";
    public static final String KEY_ACCOUNT = "ACCOUNT";
    public static final String KEY_UPLOAD_TYPE = "UPLOAD_TYPE";
    public static final String KEY_FORCE_OVERWRITE = "KEY_FORCE_OVERWRITE";
    public static final String ACCOUNT_NAME = "ACCOUNT_NAME";    
    public static final String KEY_MIME_TYPE = "MIME_TYPE";
    public static final String KEY_INSTANT_UPLOAD = "INSTANT_UPLOAD";

    public static final int UPLOAD_SINGLE_FILE = 0;
    public static final int UPLOAD_MULTIPLE_FILES = 1;

    private static final String TAG = FileUploader.class.getSimpleName();
    
    private NotificationManager mNotificationManager;
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private AbstractList<Account> mAccounts = new Vector<Account>();
    private AbstractList<UploadFileOperation> mUploads = new Vector<UploadFileOperation>(); 
    private Notification mNotification;
    private long mTotalDataToSend, mSendData;
    private int mTotalFilesToSend;
    private int mCurrentIndexUpload, mPreviousPercent;
    private int mSuccessCounter;
    private RemoteViews mDefaultNotificationContentView;
    
    /**
     * Static map with the files being download and the path to the temporal file were are download
     */
    private static Map<String, String> mUploadsInProgress = Collections.synchronizedMap(new HashMap<String, String>());
    
    /**
     * Returns True when the file referred by 'remotePath' in the ownCloud account 'account' is downloading
     */
    public static boolean isUploading(Account account, String remotePath) {
        return (mUploadsInProgress.get(buildRemoteName(account.name, remotePath)) != null);
    }
    
    /**
     * Builds a key for mUplaodsInProgress from the accountName and remotePath
     */
    private static String buildRemoteName(String accountName, String remotePath) {
        return accountName + remotePath;
    }

    
    /**
     * Checks if an ownCloud server version should support chunked uploads.
     * 
     * @param version   OwnCloud version instance corresponding to an ownCloud server.
     * @return          'True' if the ownCloud server with version supports chunked uploads.
     */
    private static boolean chunkedUploadIsSupported(OwnCloudVersion version) {
        return (version != null && version.compareTo(OwnCloudVersion.owncloud_v4_5) >= 0);    // TODO uncomment when feature is full in server
        //return false;   
    }

    

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            uploadFile();
            stopSelf(msg.arg1);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        HandlerThread thread = new HandlerThread("FileUploaderThread",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!intent.hasExtra(KEY_ACCOUNT) && !intent.hasExtra(KEY_UPLOAD_TYPE)) {
            Log.e(TAG, "Not enough information provided in intent");
            return Service.START_NOT_STICKY;
        }
        Account account = intent.getParcelableExtra(KEY_ACCOUNT);
        if (account == null) {
            Log.e(TAG, "Bad account information provided in upload intent");
            return Service.START_NOT_STICKY;
        }
        
        int uploadType = intent.getIntExtra(KEY_UPLOAD_TYPE, -1);
        if (uploadType == -1) {
            Log.e(TAG, "Incorrect upload type provided");
            return Service.START_NOT_STICKY;
        }
        String[] localPaths, remotePaths, mimeTypes; 
        if (uploadType == UPLOAD_SINGLE_FILE) {
            localPaths = new String[] { intent.getStringExtra(KEY_LOCAL_FILE) };
            remotePaths = new String[] { intent
                    .getStringExtra(KEY_REMOTE_FILE) };
            mimeTypes = new String[] { intent.getStringExtra(KEY_MIME_TYPE) };
            
        } else { // mUploadType == UPLOAD_MULTIPLE_FILES
            localPaths = intent.getStringArrayExtra(KEY_LOCAL_FILE);
            remotePaths = intent.getStringArrayExtra(KEY_REMOTE_FILE);
            mimeTypes = intent.getStringArrayExtra(KEY_MIME_TYPE);
        }
        
        if (localPaths.length != remotePaths.length) {
            Log.e(TAG, "Different number of remote paths and local paths!");
            return Service.START_NOT_STICKY;
        }
        
        boolean isInstant = intent.getBooleanExtra(KEY_INSTANT_UPLOAD, false); 
        boolean forceOverwrite = intent.getBooleanExtra(KEY_FORCE_OVERWRITE, false);
        
        for (int i=0; i < localPaths.length; i++) {
            OwnCloudVersion ocv = new OwnCloudVersion(AccountManager.get(this).getUserData(account, AccountAuthenticator.KEY_OC_VERSION));
            if (FileUploader.chunkedUploadIsSupported(ocv)) {
                mUploads.add(new ChunkedUploadFileOperation(localPaths[i], remotePaths[i], ((mimeTypes!=null)?mimeTypes[i]:""), isInstant, forceOverwrite, this));
            } else {
                mUploads.add(new UploadFileOperation(localPaths[i], remotePaths[i], (mimeTypes!=null?mimeTypes[i]:""), isInstant, forceOverwrite, this));
            }
            mAccounts.add(account);
        }

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

        return Service.START_NOT_STICKY;
    }

    
    /**
     * Core upload method: sends the file(s) to upload
     */
    public void uploadFile() {

        /// prepare upload statistics
        mTotalDataToSend = mSendData = mPreviousPercent = 0;
        Iterator<UploadFileOperation> it = mUploads.iterator();
        while (it.hasNext()) {
            mTotalDataToSend += new File(it.next().getLocalPath()).length();
        }
        mTotalFilesToSend = mUploads.size();
        Log.d(TAG, "Will upload " + mTotalDataToSend + " bytes, with " + mUploads.size() + " files");

        
        notifyUploadStart();

        UploadFileOperation currentUpload;
        Account currentAccount, lastAccount = null;
        FileDataStorageManager storageManager = null;
        WebdavClient wc = null;
        mSuccessCounter = 0;
        boolean createdInstantDir = false;
        
        for (mCurrentIndexUpload = 0; mCurrentIndexUpload < mUploads.size(); mCurrentIndexUpload++) {
            currentUpload = mUploads.get(mCurrentIndexUpload);
            currentAccount =  mAccounts.get(mCurrentIndexUpload);
            
            /// prepare client object to send request(s) to the ownCloud server
            if (lastAccount == null || !lastAccount.equals(currentAccount)) {
                storageManager = new FileDataStorageManager(currentAccount, getContentResolver());
                wc = OwnCloudClientUtils.createOwnCloudClient(currentAccount, getApplicationContext());
                wc.setDataTransferProgressListener(this);
            }
            
            if (currentUpload.isInstant() && !createdInstantDir) {
                createdInstantDir = createRemoteFolderForInstantUploads(wc, storageManager);
            }
        
            /// perform the upload
            long parentDirId = -1;
            RemoteOperationResult uploadResult = null;
            boolean updateResult = false;
            try {
                File remote = new File(currentUpload.getRemotePath());
                parentDirId = storageManager.getFileByPath(remote.getParent().endsWith("/")?remote.getParent():remote.getParent()+"/").getFileId();
                File local = new File(currentUpload.getLocalPath());
                long size = local.length();
                mUploadsInProgress.put(buildRemoteName(currentAccount.name, currentUpload.getRemotePath()), currentUpload.getLocalPath());
                uploadResult = currentUpload.execute(wc);
                if (uploadResult.isSuccess()) {
                    saveNewOCFile(currentUpload, storageManager, parentDirId, size);
                    mSuccessCounter++;
                    updateResult = true;
                }
                
            } finally {
                mUploadsInProgress.remove(buildRemoteName(currentAccount.name, currentUpload.getRemotePath()));
                broadcastUploadEnd(currentUpload, currentAccount, updateResult, parentDirId);
            }
        }
        
        notifyUploadEndOverview();
        
    }

    /**
     * Create remote folder for instant uploads if necessary.
     * 
     * @param client            WebdavClient to the ownCloud server.
     * @param storageManager    Interface to the local database caching the data in the server.
     * @return                  'True' if the folder exists when the methods finishes.
     */
    private boolean createRemoteFolderForInstantUploads(WebdavClient client, FileDataStorageManager storageManager) {
        boolean result = true;
        OCFile instantUploadDir = storageManager.getFileByPath(PhotoTakenBroadcastReceiver.INSTANT_UPLOAD_DIR);
        if (instantUploadDir == null) {
            result = client.createDirectory(PhotoTakenBroadcastReceiver.INSTANT_UPLOAD_DIR);    // fail could just mean that it already exists, but local database is not synchronized; the upload will be started anyway
            OCFile newDir = new OCFile(PhotoTakenBroadcastReceiver.INSTANT_UPLOAD_DIR);
            newDir.setMimetype("DIR");
            newDir.setParentId(storageManager.getFileByPath(OCFile.PATH_SEPARATOR).getFileId());
            storageManager.saveFile(newDir);
        }
        return result;
    }

    /**
     * Saves a new OC File after a successful upload.
     * 
     * @param upload            Upload operation completed.
     * @param storageManager    Interface to the database where the new OCFile has to be stored.
     * @param parentDirId       Id of the parent OCFile.
     * @param size              Size of the file.
     */
    private void saveNewOCFile(UploadFileOperation upload, FileDataStorageManager storageManager, long parentDirId, long size) {
        OCFile newFile = new OCFile(upload.getRemotePath());
        newFile.setMimetype(upload.getMimeType());
        newFile.setFileLength(size);
        newFile.setModificationTimestamp(System.currentTimeMillis());
        newFile.setLastSyncDate(0);
        newFile.setStoragePath(upload.getLocalPath());         
        newFile.setParentId(parentDirId);
        if (upload.getForceOverwrite())
            newFile.setKeepInSync(true);
        storageManager.saveFile(newFile);
    }

    /**
     * Creates a status notification to show the upload progress
     */
    private void notifyUploadStart() {
        mNotification = new Notification(R.drawable.icon, getString(R.string.uploader_upload_in_progress_ticker), System.currentTimeMillis());
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        mDefaultNotificationContentView = mNotification.contentView;
        mNotification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.progressbar_layout);
        mNotification.contentView.setProgressBar(R.id.status_progress, 100, 0, false);
        mNotification.contentView.setImageViewResource(R.id.status_icon, R.drawable.icon);
        // dvelasco ; contentIntent MUST be assigned to avoid app crashes in versions previous to Android 4.x ;
        //              BUT an empty Intent is not a very elegant solution; something smart should happen when a user 'clicks' on an upload in the notification bar
        mNotification.contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
        mNotificationManager.notify(R.string.uploader_upload_in_progress_ticker, mNotification);
    }

    
    /**
     * Notifies upload (or fail) of a file to activities interested
     */
    private void broadcastUploadEnd(UploadFileOperation upload, Account account, boolean success, long parentDirId) {
        /// 
        Intent end = new Intent(UPLOAD_FINISH_MESSAGE);
        end.putExtra(EXTRA_REMOTE_PATH, upload.getRemotePath());
        end.putExtra(EXTRA_FILE_PATH, upload.getLocalPath());
        end.putExtra(ACCOUNT_NAME, account.name);
        end.putExtra(EXTRA_UPLOAD_RESULT, success);
        end.putExtra(EXTRA_PARENT_DIR_ID, parentDirId);
        sendBroadcast(end);
    }


    /**
     * Updates the status notification with the results of a batch of uploads.
     */
    private void notifyUploadEndOverview() {
        /// notify final result
        if (mSuccessCounter == mTotalFilesToSend) {    // success
            mNotification.flags ^= Notification.FLAG_ONGOING_EVENT; // remove the ongoing flag
            mNotification.flags |= Notification.FLAG_AUTO_CANCEL;
            mNotification.contentView = mDefaultNotificationContentView;
            // TODO put something smart in the contentIntent below
            mNotification.contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
            if (mTotalFilesToSend == 1) {
                mNotification.setLatestEventInfo(   getApplicationContext(), 
                                                    getString(R.string.uploader_upload_succeeded_ticker), 
                                                    String.format(getString(R.string.uploader_upload_succeeded_content_single), (new File(mUploads.get(0).getLocalPath())).getName()), 
                                                    mNotification.contentIntent);
            } else {
                mNotification.setLatestEventInfo(   getApplicationContext(), 
                                                    getString(R.string.uploader_upload_succeeded_ticker), 
                                                    String.format(getString(R.string.uploader_upload_succeeded_content_multiple), mSuccessCounter), 
                                                    mNotification.contentIntent);
            }                
            mNotificationManager.notify(R.string.uploader_upload_in_progress_ticker, mNotification);    // NOT AN ERROR; uploader_upload_in_progress_ticker is the target, not a new notification
            
        } else {
            mNotificationManager.cancel(R.string.uploader_upload_in_progress_ticker);
            Notification finalNotification = new Notification(R.drawable.icon, getString(R.string.uploader_upload_failed_ticker), System.currentTimeMillis());
            finalNotification.flags |= Notification.FLAG_AUTO_CANCEL;
            // TODO put something smart in the contentIntent below
            finalNotification.contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
            if (mTotalFilesToSend == 1) {
                finalNotification.setLatestEventInfo(   getApplicationContext(), 
                                                        getString(R.string.uploader_upload_failed_ticker), 
                                                        String.format(getString(R.string.uploader_upload_failed_content_single), (new File(mUploads.get(0).getLocalPath())).getName()), 
                                                        finalNotification.contentIntent);
            } else {
                finalNotification.setLatestEventInfo(   getApplicationContext(), 
                                                        getString(R.string.uploader_upload_failed_ticker), 
                                                        String.format(getString(R.string.uploader_upload_failed_content_multiple), mSuccessCounter, mTotalFilesToSend), 
                                                        finalNotification.contentIntent);
            }                
            mNotificationManager.notify(R.string.uploader_upload_failed_ticker, finalNotification);
        }
        
    }
    
    
    /**
     * Callback method to update the progress bar in the status notification.
     */
    @Override
    public void transferProgress(long progressRate) {
        mSendData += progressRate;
        int percent = (int)(100*((double)mSendData)/((double)mTotalDataToSend));
        if (percent != mPreviousPercent) {
            String text = String.format(getString(R.string.uploader_upload_in_progress_content), percent, new File(mUploads.get(mCurrentIndexUpload).getLocalPath()).getName());
            mNotification.contentView.setProgressBar(R.id.status_progress, 100, percent, false);
            mNotification.contentView.setTextViewText(R.id.status_text, text);
            mNotificationManager.notify(R.string.uploader_upload_in_progress_ticker, mNotification);
        }
        mPreviousPercent = percent;
    }
}
