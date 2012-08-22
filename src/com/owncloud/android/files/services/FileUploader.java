package com.owncloud.android.files.services;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.PhotoTakenBroadcastReceiver;

import eu.alefzero.webdav.OnDatatransferProgressListener;
import com.owncloud.android.utils.OwnCloudClientUtils;

import android.accounts.Account;
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
    public static final String ACCOUNT_NAME = "ACCOUNT_NAME";    
    public static final String KEY_MIME_TYPE = "MIME_TYPE";
    public static final String KEY_INSTANT_UPLOAD = "INSTANT_UPLOAD";

    public static final int UPLOAD_SINGLE_FILE = 0;
    public static final int UPLOAD_MULTIPLE_FILES = 1;

    private static final String TAG = "FileUploader";
    
    private NotificationManager mNotificationManager;
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private Account mAccount;
    private String[] mLocalPaths, mRemotePaths, mMimeTypes;
    private int mUploadType;
    private Notification mNotification;
    private long mTotalDataToSend, mSendData;
    private int mCurrentIndexUpload, mPreviousPercent;
    private int mSuccessCounter;
    private boolean mIsInstant;
    
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
        mAccount = intent.getParcelableExtra(KEY_ACCOUNT);
        mUploadType = intent.getIntExtra(KEY_UPLOAD_TYPE, -1);
        if (mUploadType == -1) {
            Log.e(TAG, "Incorrect upload type provided");
            return Service.START_NOT_STICKY;
        }
        if (mUploadType == UPLOAD_SINGLE_FILE) {
            mLocalPaths = new String[] { intent.getStringExtra(KEY_LOCAL_FILE) };
            mRemotePaths = new String[] { intent
                    .getStringExtra(KEY_REMOTE_FILE) };
            mMimeTypes = new String[] { intent.getStringExtra(KEY_MIME_TYPE) };
            
        } else { // mUploadType == UPLOAD_MULTIPLE_FILES
            mLocalPaths = intent.getStringArrayExtra(KEY_LOCAL_FILE);
            mRemotePaths = intent.getStringArrayExtra(KEY_REMOTE_FILE);
            mMimeTypes = intent.getStringArrayExtra(KEY_MIME_TYPE);
        }
        
        if (mLocalPaths.length != mRemotePaths.length) {
            Log.e(TAG, "Different number of remote paths and local paths!");
            return Service.START_NOT_STICKY;
        }

        mIsInstant = intent.getBooleanExtra(KEY_INSTANT_UPLOAD, false); 
                
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

        return Service.START_NOT_STICKY;
    }

    
    /**
     * Core upload method: sends the file(s) to upload
     */
    public void uploadFile() {
        FileDataStorageManager storageManager = new FileDataStorageManager(mAccount, getContentResolver());

        mTotalDataToSend = mSendData = mPreviousPercent = 0;
        
        /// prepare client object to send the request to the ownCloud server
        WebdavClient wc = OwnCloudClientUtils.createOwnCloudClient(mAccount, getApplicationContext());
        wc.setDataTransferProgressListener(this);

        /// create status notification to show the upload progress
        mNotification = new Notification(R.drawable.icon, getString(R.string.uploader_upload_in_progress_ticker), System.currentTimeMillis());
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        RemoteViews oldContentView = mNotification.contentView;
        mNotification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.progressbar_layout);
        mNotification.contentView.setProgressBar(R.id.status_progress, 100, 0, false);
        mNotification.contentView.setImageViewResource(R.id.status_icon, R.drawable.icon);
        // dvelasco ; contentIntent MUST be assigned to avoid app crashes in versions previous to Android 4.x ;
        //              BUT an empty Intent is not a very elegant solution; something smart should happen when a user 'clicks' on an upload in the notification bar
        mNotification.contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
        mNotificationManager.notify(R.string.uploader_upload_in_progress_ticker, mNotification);

        /// create remote folder for instant uploads if necessary
        if (mIsInstant) {
            OCFile instantUploadDir = storageManager.getFileByPath(PhotoTakenBroadcastReceiver.INSTANT_UPLOAD_DIR);
            if (instantUploadDir == null) {
                wc.createDirectory(PhotoTakenBroadcastReceiver.INSTANT_UPLOAD_DIR);    // fail could just mean that it already exists, but local database is not synchronized; the upload will be started anyway
                OCFile newDir = new OCFile(PhotoTakenBroadcastReceiver.INSTANT_UPLOAD_DIR);
                newDir.setMimetype("DIR");
                newDir.setParentId(storageManager.getFileByPath(OCFile.PATH_SEPARATOR).getFileId());
                storageManager.saveFile(newDir);
            }
        }
        
        /// perform the upload
        File [] localFiles = new File[mLocalPaths.length];
        for (int i = 0; i < mLocalPaths.length; ++i) {
            localFiles[i] = new File(mLocalPaths[i]);
            mTotalDataToSend += localFiles[i].length();
        }
        Log.d(TAG, "Will upload " + mTotalDataToSend + " bytes, with " + mLocalPaths.length + " files");
        mSuccessCounter = 0;
        for (int i = 0; i < mLocalPaths.length; ++i) {
            String mimeType = (mMimeTypes != null) ? mMimeTypes[i] : null;
            if (mimeType == null) {
                try {
                    mimeType = MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(
                                mLocalPaths[i].substring(mLocalPaths[i]
                                    .lastIndexOf('.') + 1));
                } catch (IndexOutOfBoundsException e) {
                    Log.e(TAG, "Trying to find out MIME type of a file without extension: " + mLocalPaths[i]);
                }
            }
            if (mimeType == null)
                mimeType = "application/octet-stream";
            mCurrentIndexUpload = i;
            long parentDirId = -1;
            boolean uploadResult = false;
            String availablePath = getAvailableRemotePath(wc, mRemotePaths[i]);
            try {
                File f = new File(mRemotePaths[i]);
                parentDirId = storageManager.getFileByPath(f.getParent().endsWith("/")?f.getParent():f.getParent()+"/").getFileId();
                if(availablePath != null) {
                    mRemotePaths[i] = availablePath;
                    mUploadsInProgress.put(buildRemoteName(mAccount.name, mRemotePaths[i]), mLocalPaths[i]);
                    if (wc.putFile(mLocalPaths[i], mRemotePaths[i], mimeType)) {
                        OCFile new_file = new OCFile(mRemotePaths[i]);
                        new_file.setMimetype(mimeType);
                        new_file.setFileLength(localFiles[i].length());
                        new_file.setModificationTimestamp(System.currentTimeMillis());
                        new_file.setLastSyncDate(0);
                        new_file.setStoragePath(mLocalPaths[i]);         
                        new_file.setParentId(parentDirId);
                        storageManager.saveFile(new_file);
                        mSuccessCounter++;
                        uploadResult = true;
                    }
                }
            } finally {
                mUploadsInProgress.remove(buildRemoteName(mAccount.name, mRemotePaths[i]));
                
                /// notify upload (or fail) of EACH file to activities interested
                Intent end = new Intent(UPLOAD_FINISH_MESSAGE);
                end.putExtra(EXTRA_PARENT_DIR_ID, parentDirId);
                end.putExtra(EXTRA_UPLOAD_RESULT, uploadResult);
                end.putExtra(EXTRA_REMOTE_PATH, mRemotePaths[i]);
                end.putExtra(EXTRA_FILE_PATH, mLocalPaths[i]);
                end.putExtra(ACCOUNT_NAME, mAccount.name);
                sendBroadcast(end);
            }
            
        }
        
        /// notify final result
        if (mSuccessCounter == mLocalPaths.length) {    // success
            //Notification finalNotification = new Notification(R.drawable.icon, getString(R.string.uploader_upload_succeeded_ticker), System.currentTimeMillis());
            mNotification.flags ^= Notification.FLAG_ONGOING_EVENT; // remove the ongoing flag
            mNotification.flags |= Notification.FLAG_AUTO_CANCEL;
            mNotification.contentView = oldContentView;
            // TODO put something smart in the contentIntent below
            mNotification.contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
            if (mLocalPaths.length == 1) {
                mNotification.setLatestEventInfo(   getApplicationContext(), 
                                                    getString(R.string.uploader_upload_succeeded_ticker), 
                                                    String.format(getString(R.string.uploader_upload_succeeded_content_single), localFiles[0].getName()), 
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
            if (mLocalPaths.length == 1) {
                finalNotification.setLatestEventInfo(   getApplicationContext(), 
                                                        getString(R.string.uploader_upload_failed_ticker), 
                                                        String.format(getString(R.string.uploader_upload_failed_content_single), localFiles[0].getName()), 
                                                        finalNotification.contentIntent);
            } else {
                finalNotification.setLatestEventInfo(   getApplicationContext(), 
                                                        getString(R.string.uploader_upload_failed_ticker), 
                                                        String.format(getString(R.string.uploader_upload_failed_content_multiple), mSuccessCounter, mLocalPaths.length), 
                                                        finalNotification.contentIntent);
            }                
            mNotificationManager.notify(R.string.uploader_upload_failed_ticker, finalNotification);
        }
        
    }

    /**
     * Checks if remotePath does not exist in the server and returns it, or adds a suffix to it in order to avoid the server
     * file is overwritten.
     * 
     * @param string
     * @return
     */
    private String getAvailableRemotePath(WebdavClient wc, String remotePath) {
        Boolean check = wc.existsFile(remotePath);
        if (check == null) {    // null means fail
            return null;
        } else if (!check) {
            return remotePath;
        }
    
        int pos = remotePath.lastIndexOf(".");
        String suffix = "";
        String extension = "";
        if (pos >= 0) {
            extension = remotePath.substring(pos+1);
            remotePath = remotePath.substring(0, pos);
        }
        int count = 2;
        while (check != null && check) {
            suffix = " (" + count + ")";
            if (pos >= 0)
                check = wc.existsFile(remotePath + suffix + "." + extension);
            else
                check = wc.existsFile(remotePath + suffix);
            count++;
        }
        if (check == null) {
            return null;
        } else if (pos >=0) {
            return remotePath + suffix + "." + extension;
        } else {
            return remotePath + suffix;
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
            String text = String.format(getString(R.string.uploader_upload_in_progress_content), percent, new File(mLocalPaths[mCurrentIndexUpload]).getName());
            mNotification.contentView.setProgressBar(R.id.status_progress, 100, percent, false);
            mNotification.contentView.setTextViewText(R.id.status_text, text);
            mNotificationManager.notify(R.string.uploader_upload_in_progress_ticker, mNotification);
        }
        mPreviousPercent = percent;
    }
}
