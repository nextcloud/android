package eu.alefzero.owncloud.files.services;

import java.io.File;

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
import android.webkit.MimeTypeMap;
import android.widget.RemoteViews;
import android.widget.Toast;
import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.datamodel.FileDataStorageManager;
import eu.alefzero.owncloud.datamodel.OCFile;
import eu.alefzero.owncloud.files.interfaces.OnDatatransferProgressListener;
import eu.alefzero.webdav.WebdavClient;

public class FileUploader extends Service implements OnDatatransferProgressListener {

    public static final String UPLOAD_FINISH_MESSAGE = "UPLOAD_FINISH";
    public static final String EXTRA_PARENT_DIR_ID = "PARENT_DIR_ID";
    
    public static final String KEY_LOCAL_FILE = "LOCAL_FILE";
    public static final String KEY_REMOTE_FILE = "REMOTE_FILE";
    public static final String KEY_ACCOUNT = "ACCOUNT";
    public static final String KEY_UPLOAD_TYPE = "UPLOAD_TYPE";

    public static final int UPLOAD_SINGLE_FILE = 0;
    public static final int UPLOAD_MULTIPLE_FILES = 1;

    private static final String TAG = "FileUploader";
    private NotificationManager mNotificationManager;
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private Account mAccount;
    private String[] mLocalPaths, mRemotePaths;
    private int mUploadType;
    private Notification mNotification;
    private int mTotalDataToSend, mSendData;
    private int mCurrentIndexUpload, mPreviousPercent;
    private int mSuccessCounter;

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
            Log.e(TAG, "Not enought data in intent provided");
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
        } else { // mUploadType == UPLOAD_MULTIPLE_FILES
            mLocalPaths = intent.getStringArrayExtra(KEY_LOCAL_FILE);
            mRemotePaths = intent.getStringArrayExtra(KEY_REMOTE_FILE);
        }

        for (int i = 0; i < mRemotePaths.length; ++i)
            mRemotePaths[i] = mRemotePaths[i].replace(' ', '+');

        if (mLocalPaths.length != mRemotePaths.length) {
            Log.e(TAG, "Remote paths and local paths are not equal!");
            return Service.START_NOT_STICKY;
        }

        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);

        return Service.START_NOT_STICKY;
    }

    public void run() {
        String message;
        if (mSuccessCounter == mLocalPaths.length) {
            message = getString(R.string.uploader_upload_succeed); 
        } else {
            message = getString(R.string.uploader_upload_failed); 
            if (mLocalPaths.length > 1)
                message += " (" + mSuccessCounter + " / " + mLocalPaths.length + getString(R.string.uploader_files_uploaded_suffix) + ")";
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void uploadFile() {
        FileDataStorageManager storageManager = new FileDataStorageManager(mAccount, getContentResolver());
        
        mTotalDataToSend = mSendData = mPreviousPercent = 0;
        
        mNotification = new Notification(
                eu.alefzero.owncloud.R.drawable.icon, "Uploading...",
                System.currentTimeMillis());
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        mNotification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.progressbar_layout);
        mNotification.contentView.setProgressBar(R.id.status_progress, 100, 0, false);
        mNotification.contentView.setImageViewResource(R.id.status_icon, R.drawable.icon);
        // dvelasco ; contentIntent MUST be assigned to avoid app crashes in versions previous to Android 4.x ;
        //              BUT an empty Intent is not a very elegant solution; something smart should happen when a user 'clicks' on an upload in the notification bar
        mNotification.contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
        
        mNotificationManager.notify(42, mNotification);

        WebdavClient wc = new WebdavClient(mAccount, getApplicationContext());
        wc.setDataTransferProgressListener(this);

        for (int i = 0; i < mLocalPaths.length; ++i) {
            File f = new File(mLocalPaths[i]);
            mTotalDataToSend += f.length();
        }
        
        Log.d(TAG, "Will upload " + mTotalDataToSend + " bytes, with " + mLocalPaths.length + " files");
        
        mSuccessCounter = 0;
        
        for (int i = 0; i < mLocalPaths.length; ++i) {
            
            String mimeType = null;
            try {
                mimeType = MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(
                                mLocalPaths[i].substring(mLocalPaths[i]
                                    .lastIndexOf('.') + 1));
            } catch (IndexOutOfBoundsException e) {
                Log.e(TAG, "Trying to find out MIME type of a file without extension: " + mLocalPaths[i]);
            }
            if (mimeType == null)
                mimeType = "application/octet-stream";
            
            mCurrentIndexUpload = i;
            if (wc.putFile(mLocalPaths[i], mRemotePaths[i], mimeType)) {
                mSuccessCounter++;
                OCFile new_file = new OCFile(mRemotePaths[i]);
                new_file.setMimetype(mimeType);
                new_file.setFileLength(new File(mLocalPaths[i]).length());
                new_file.setModificationTimestamp(System.currentTimeMillis());
                new_file.setLastSyncDate(0);
                new_file.setStoragePath(mLocalPaths[i]);         
                File f = new File(mRemotePaths[i]);
                long parentDirId = storageManager.getFileByPath(f.getParent().endsWith("/")?f.getParent():f.getParent()+"/").getFileId();
                new_file.setParentId(parentDirId);
                storageManager.saveFile(new_file);
                
                Intent end = new Intent(UPLOAD_FINISH_MESSAGE);
                end.putExtra(EXTRA_PARENT_DIR_ID, parentDirId);
                sendBroadcast(end);
            }
            
        }
        mNotificationManager.cancel(42);
        run();
    }

    @Override
    public void transferProgress(long progressRate) {
        mSendData += progressRate;
        int percent = (int)(100*((double)mSendData)/((double)mTotalDataToSend));
        if (percent != mPreviousPercent) {
            String text = String.format("%d%% Uploading %s file", percent, new File(mLocalPaths[mCurrentIndexUpload]).getName());
            mNotification.contentView.setProgressBar(R.id.status_progress, 100, percent, false);
            mNotification.contentView.setTextViewText(R.id.status_text, text);
            mNotificationManager.notify(42, mNotification);
        }
        mPreviousPercent = percent;
    }
}
