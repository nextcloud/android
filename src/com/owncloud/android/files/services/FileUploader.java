package com.owncloud.android.files.services;

import java.io.File;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.owncloud.android.authenticator.AccountAuthenticator;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.InstantUploadBroadcastReceiver;
import com.owncloud.android.operations.ChunkedUploadFileOperation;
import com.owncloud.android.operations.RemoteOperationResult;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.ui.activity.FileDetailActivity;
import com.owncloud.android.ui.fragment.FileDetailFragment;
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
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.webkit.MimeTypeMap;
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
    
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private IBinder mBinder;
    private WebdavClient mUploadClient = null;
    private Account mLastAccount = null, mLastAccountWhereInstantFolderWasCreated = null;
    private FileDataStorageManager mStorageManager;

    private ConcurrentMap<String, UploadFileOperation> mPendingUploads = new ConcurrentHashMap<String, UploadFileOperation>();
    private UploadFileOperation mCurrentUpload = null;
    
    private NotificationManager mNotificationManager;
    private Notification mNotification;
    private int mLastPercent;
    private RemoteViews mDefaultNotificationContentView;
    
    
    /**
     * Builds a key for mPendingUploads from the account and file to upload
     * 
     * @param account   Account where the file to download is stored
     * @param file      File to download
     */
    private String buildRemoteName(Account account, OCFile file) {
        return account.name + file.getRemotePath();
    }

    private String buildRemoteName(Account account, String remotePath) {
        return account.name + remotePath;
    }

    
    /**
     * Checks if an ownCloud server version should support chunked uploads.
     * 
     * @param version   OwnCloud version instance corresponding to an ownCloud server.
     * @return          'True' if the ownCloud server with version supports chunked uploads.
     */
    private static boolean chunkedUploadIsSupported(OwnCloudVersion version) {
        return (version != null && version.compareTo(OwnCloudVersion.owncloud_v4_5) >= 0);
    }

    

    /**
     * Service initialization
     */
    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        HandlerThread thread = new HandlerThread("FileUploaderThread",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper, this);
        mBinder = new FileUploaderBinder();
    }

    
    /**
     * Entry point to add one or several files to the queue of uploads.
     * 
     * New uploads are added calling to startService(), resulting in a call to this method. This ensures the service will keep on working 
     * although the caller activity goes away.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!intent.hasExtra(KEY_ACCOUNT) || !intent.hasExtra(KEY_UPLOAD_TYPE)) {
            Log.e(TAG, "Not enough information provided in intent");
            return Service.START_NOT_STICKY;
        }
        int uploadType = intent.getIntExtra(KEY_UPLOAD_TYPE, -1);
        if (uploadType == -1) {
            Log.e(TAG, "Incorrect upload type provided");
            return Service.START_NOT_STICKY;
        }
        Account account = intent.getParcelableExtra(KEY_ACCOUNT);
        
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

        if (localPaths == null) {
            Log.e(TAG, "Incorrect array for local paths provided in upload intent");
            return Service.START_NOT_STICKY;
        }
        if (remotePaths == null) {
            Log.e(TAG, "Incorrect array for remote paths provided in upload intent");
            return Service.START_NOT_STICKY;
        }
            
        if (localPaths.length != remotePaths.length) {
            Log.e(TAG, "Different number of remote paths and local paths!");
            return Service.START_NOT_STICKY;
        }
        
        boolean isInstant = intent.getBooleanExtra(KEY_INSTANT_UPLOAD, false); 
        boolean forceOverwrite = intent.getBooleanExtra(KEY_FORCE_OVERWRITE, false);
        
        OwnCloudVersion ocv = new OwnCloudVersion(AccountManager.get(this).getUserData(account, AccountAuthenticator.KEY_OC_VERSION));
        boolean chunked = FileUploader.chunkedUploadIsSupported(ocv);
        AbstractList<String> requestedUploads = new Vector<String>();
        String uploadKey = null;
        UploadFileOperation newUpload = null;
        OCFile file = null;
        FileDataStorageManager storageManager = new FileDataStorageManager(account, getContentResolver());
        try {
            for (int i=0; i < localPaths.length; i++) {
                uploadKey = buildRemoteName(account, remotePaths[i]);
                file = obtainNewOCFileToUpload(remotePaths[i], localPaths[i], ((mimeTypes!=null)?mimeTypes[i]:(String)null), forceOverwrite, storageManager);
                if (chunked) {
                    newUpload = new ChunkedUploadFileOperation(account, file, isInstant, forceOverwrite);
                } else {
                    newUpload = new UploadFileOperation(account, file, isInstant, forceOverwrite);
                }
                mPendingUploads.putIfAbsent(uploadKey, newUpload);
                newUpload.addDatatransferProgressListener(this);
                requestedUploads.add(uploadKey);
            }
            
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Not enough information provided in intent: " + e.getMessage());
            return START_NOT_STICKY;
        }
        
        if (requestedUploads.size() > 0) {
            Message msg = mServiceHandler.obtainMessage();
            msg.arg1 = startId;
            msg.obj = requestedUploads;
            mServiceHandler.sendMessage(msg);
        }

        return Service.START_NOT_STICKY;
    }

    
    /**
     * Provides a binder object that clients can use to perform operations on the queue of uploads, excepting the addition of new files. 
     * 
     * Implemented to perform cancellation, pause and resume of existing uploads.
     */
    @Override
    public IBinder onBind(Intent arg0) {
        return mBinder;
    }

    /**
     *  Binder to let client components to perform operations on the queue of uploads.
     * 
     *  It provides by itself the available operations.
     */
    public class FileUploaderBinder extends Binder {
        
        /**
         * Cancels a pending or current upload of a remote file.
         * 
         * @param account       Owncloud account where the remote file will be stored.
         * @param file          A file in the queue of pending uploads
         */
        public void cancel(Account account, OCFile file) {
            UploadFileOperation upload = null;
            synchronized (mPendingUploads) {
                upload = mPendingUploads.remove(buildRemoteName(account, file));
            }
            if (upload != null) {
                upload.cancel();
            }
        }
        
        
        /**
         * Returns True when the file described by 'file' is being uploaded to the ownCloud account 'account' or waiting for it
         * 
         * @param account       Owncloud account where the remote file will be stored.
         * @param file          A file that could be in the queue of pending uploads
         */
        public boolean isUploading(Account account, OCFile file) {
            synchronized (mPendingUploads) {
                return (mPendingUploads.containsKey(buildRemoteName(account, file)));
            }
        }
    }
    
    
    
    
    /** 
     * Upload worker. Performs the pending uploads in the order they were requested. 
     * 
     * Created with the Looper of a new thread, started in {@link FileUploader#onCreate()}. 
     */
    private static class ServiceHandler extends Handler {
        // don't make it a final class, and don't remove the static ; lint will warn about a possible memory leak
        FileUploader mService;
        public ServiceHandler(Looper looper, FileUploader service) {
            super(looper);
            if (service == null)
                throw new IllegalArgumentException("Received invalid NULL in parameter 'service'");
            mService = service;
        }

        @Override
        public void handleMessage(Message msg) {
            @SuppressWarnings("unchecked")
            AbstractList<String> requestedUploads = (AbstractList<String>) msg.obj;
            if (msg.obj != null) {
                Iterator<String> it = requestedUploads.iterator();
                while (it.hasNext()) {
                    mService.uploadFile(it.next());
                }
            }
            mService.stopSelf(msg.arg1);
        }
    }

    
    
    
    /**
     * Core upload method: sends the file(s) to upload
     * 
     * @param uploadKey   Key to access the upload to perform, contained in mPendingUploads
     */
    public void uploadFile(String uploadKey) {

        synchronized(mPendingUploads) {
            mCurrentUpload = mPendingUploads.get(uploadKey);
        }
        
        if (mCurrentUpload != null) {
            
            notifyUploadStart(mCurrentUpload);

            
            /// prepare client object to send requests to the ownCloud server
            if (mUploadClient == null || !mLastAccount.equals(mCurrentUpload.getAccount())) {
                mLastAccount = mCurrentUpload.getAccount();
                mStorageManager = new FileDataStorageManager(mLastAccount, getContentResolver());
                mUploadClient = OwnCloudClientUtils.createOwnCloudClient(mLastAccount, getApplicationContext());
            }
            
            /// create remote folder for instant uploads, "if necessary" (would be great that HEAD to a folder worked as with files, we should check; but it's not WebDAV standard, anyway
            if (mCurrentUpload.isInstant() && !mLastAccountWhereInstantFolderWasCreated.equals(mCurrentUpload.getAccount())) {
                mLastAccountWhereInstantFolderWasCreated = mCurrentUpload.getAccount();
                createRemoteFolderForInstantUploads(mUploadClient, mStorageManager);
            }
        
            /// perform the upload
            RemoteOperationResult uploadResult = null;
            try {
                uploadResult = mCurrentUpload.execute(mUploadClient);
                if (uploadResult.isSuccess()) {
                    saveUploadedFile(mCurrentUpload.getFile(), mStorageManager);
                }
                
            } finally {
                synchronized(mPendingUploads) {
                    mPendingUploads.remove(uploadKey);
                }
            }
        
            /// notify result
            notifyUploadResult(uploadResult, mCurrentUpload);
            
            sendFinalBroadcast(mCurrentUpload, uploadResult);
            
        }
        
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
        OCFile instantUploadDir = storageManager.getFileByPath(InstantUploadBroadcastReceiver.INSTANT_UPLOAD_DIR);
        if (instantUploadDir == null) {
            result = client.createDirectory(InstantUploadBroadcastReceiver.INSTANT_UPLOAD_DIR);    // fail could just mean that it already exists, but local database is not synchronized; the upload will be started anyway
            OCFile newDir = new OCFile(InstantUploadBroadcastReceiver.INSTANT_UPLOAD_DIR);
            newDir.setMimetype("DIR");
            newDir.setParentId(storageManager.getFileByPath(OCFile.PATH_SEPARATOR).getFileId());
            storageManager.saveFile(newDir);
        }
        return result;
    }

    /**
     * Saves a new OC File after a successful upload.
     * 
     * @param file              OCFile describing the uploaded file
     * @param storageManager    Interface to the database where the new OCFile has to be stored.
     * @param parentDirId       Id of the parent OCFile.
     */
    private void saveUploadedFile(OCFile file, FileDataStorageManager storageManager) {
        file.setModificationTimestamp(System.currentTimeMillis());
        storageManager.saveFile(file);
    }
    
    
    private OCFile obtainNewOCFileToUpload(String remotePath, String localPath, String mimeType, boolean forceOverwrite, FileDataStorageManager storageManager) {
        OCFile newFile = new OCFile(remotePath);
        newFile.setStoragePath(localPath);
        newFile.setLastSyncDate(0);
        newFile.setKeepInSync(forceOverwrite);
        
        // size
        if (localPath != null && localPath.length() > 0) {
            File localFile = new File(localPath);
            newFile.setFileLength(localFile.length());
        }   // don't worry about not assigning size, the problems with localPath are checked when the UploadFileOperation instance is created
        
        // MIME type
        if (mimeType == null || mimeType.length() <= 0) {
            try {
                mimeType = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(
                            remotePath.substring(remotePath.lastIndexOf('.') + 1));
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "Trying to find out MIME type of a file without extension: " + remotePath);
            }
        }
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        newFile.setMimetype(mimeType);
        
        // parent dir
        String parentPath = new File(remotePath).getParent();
        parentPath = parentPath.endsWith("/")?parentPath:parentPath+"/" ;
        long parentDirId = storageManager.getFileByPath(parentPath).getFileId();
        newFile.setParentId(parentDirId);
        
        return newFile;
    }
    

    /**
     * Creates a status notification to show the upload progress
     * 
     * @param upload    Upload operation starting.
     */
    private void notifyUploadStart(UploadFileOperation upload) {
        /// create status notification with a progress bar
        mLastPercent = 0;
        mNotification = new Notification(R.drawable.icon, getString(R.string.uploader_upload_in_progress_ticker), System.currentTimeMillis());
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        mDefaultNotificationContentView = mNotification.contentView;
        mNotification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.progressbar_layout);
        mNotification.contentView.setProgressBar(R.id.status_progress, 100, 0, false);
        mNotification.contentView.setTextViewText(R.id.status_text, String.format(getString(R.string.uploader_upload_in_progress_content), 0, new File(upload.getStoragePath()).getName()));
        mNotification.contentView.setImageViewResource(R.id.status_icon, R.drawable.icon);
        
        /// includes a pending intent in the notification showing the details view of the file
        Intent showDetailsIntent = new Intent(this, FileDetailActivity.class);
        showDetailsIntent.putExtra(FileDetailFragment.EXTRA_FILE, upload.getFile());
        showDetailsIntent.putExtra(FileDetailFragment.EXTRA_ACCOUNT, upload.getAccount());
        showDetailsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mNotification.contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, showDetailsIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        
        mNotificationManager.notify(R.string.uploader_upload_in_progress_ticker, mNotification);
    }

    
    /**
     * Callback method to update the progress bar in the status notification
     */
    @Override
    public void onTransferProgress(long progressRate, long totalTransferredSoFar, long totalToTransfer, String fileName) {
        int percent = (int)(100.0*((double)totalTransferredSoFar)/((double)totalToTransfer));
        if (percent != mLastPercent) {
            mNotification.contentView.setProgressBar(R.id.status_progress, 100, percent, false);
            String text = String.format(getString(R.string.uploader_upload_in_progress_content), percent, fileName);
            mNotification.contentView.setTextViewText(R.id.status_text, text);
            mNotificationManager.notify(R.string.uploader_upload_in_progress_ticker, mNotification);
        }
        mLastPercent = percent;
    }
    
    
    /**
     * Callback method to update the progress bar in the status notification  (old version)
     */
    @Override
    public void onTransferProgress(long progressRate) {
        // NOTHING TO DO HERE ANYMORE
    }

    
    /**
     * Updates the status notification with the result of an upload operation.
     * 
     * @param uploadResult    Result of the upload operation.
     * @param upload          Finished upload operation
     */
    private void notifyUploadResult(RemoteOperationResult uploadResult, UploadFileOperation upload) {
        if (uploadResult.isCancelled()) {
            /// cancelled operation -> silent removal of progress notification
            mNotificationManager.cancel(R.string.uploader_upload_in_progress_ticker);
            
        } else if (uploadResult.isSuccess()) {
            /// success -> silent update of progress notification to success message 
            mNotification.flags ^= Notification.FLAG_ONGOING_EVENT; // remove the ongoing flag
            mNotification.flags |= Notification.FLAG_AUTO_CANCEL;
            mNotification.contentView = mDefaultNotificationContentView;
            
            /// includes a pending intent in the notification showing the details view of the file
            Intent showDetailsIntent = new Intent(this, FileDetailActivity.class);
            showDetailsIntent.putExtra(FileDetailFragment.EXTRA_FILE, upload.getFile());
            showDetailsIntent.putExtra(FileDetailFragment.EXTRA_ACCOUNT, upload.getAccount());
            showDetailsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            mNotification.contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, showDetailsIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            
            mNotification.setLatestEventInfo(   getApplicationContext(), 
                                                getString(R.string.uploader_upload_succeeded_ticker), 
                                                String.format(getString(R.string.uploader_upload_succeeded_content_single), (new File(upload.getStoragePath())).getName()), 
                                                mNotification.contentIntent);
            
            mNotificationManager.notify(R.string.uploader_upload_in_progress_ticker, mNotification);    // NOT AN ERROR; uploader_upload_in_progress_ticker is the target, not a new notification
            
            /* Notification about multiple uploads: pending of update
            mNotification.setLatestEventInfo(   getApplicationContext(), 
                                                    getString(R.string.uploader_upload_succeeded_ticker), 
                                                    String.format(getString(R.string.uploader_upload_succeeded_content_multiple), mSuccessCounter), 
                                                    mNotification.contentIntent);
             */
            
        } else {
            /// fail -> explicit failure notification
            mNotificationManager.cancel(R.string.uploader_upload_in_progress_ticker);
            Notification finalNotification = new Notification(R.drawable.icon, getString(R.string.uploader_upload_failed_ticker), System.currentTimeMillis());
            finalNotification.flags |= Notification.FLAG_AUTO_CANCEL;
            // TODO put something smart in the contentIntent below
            finalNotification.contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
            finalNotification.setLatestEventInfo(   getApplicationContext(), 
                                                    getString(R.string.uploader_upload_failed_ticker), 
                                                    String.format(getString(R.string.uploader_upload_failed_content_single), (new File(upload.getStoragePath())).getName()), 
                                                    finalNotification.contentIntent);
            
            mNotificationManager.notify(R.string.uploader_upload_failed_ticker, finalNotification);
            
            /* Notification about multiple uploads failure: pending of update
            finalNotification.setLatestEventInfo(   getApplicationContext(), 
                                                        getString(R.string.uploader_upload_failed_ticker), 
                                                        String.format(getString(R.string.uploader_upload_failed_content_multiple), mSuccessCounter, mTotalFilesToSend), 
                                                        finalNotification.contentIntent);
            } */                
        }
        
    }
    
    
    /**
     * Sends a broadcast in order to the interested activities can update their view
     * 
     * @param upload          Finished upload operation
     * @param uploadResult    Result of the upload operation
     */
    private void sendFinalBroadcast(UploadFileOperation upload, RemoteOperationResult uploadResult) {
        Intent end = new Intent(UPLOAD_FINISH_MESSAGE);
        end.putExtra(EXTRA_REMOTE_PATH, upload.getRemotePath());    // real remote path, after possible automatic renaming
        end.putExtra(EXTRA_FILE_PATH, upload.getStoragePath());
        end.putExtra(ACCOUNT_NAME, upload.getAccount().name);
        end.putExtra(EXTRA_UPLOAD_RESULT, uploadResult.isSuccess());
        end.putExtra(EXTRA_PARENT_DIR_ID, upload.getFile().getParentId());
        sendBroadcast(end);
    }


}
