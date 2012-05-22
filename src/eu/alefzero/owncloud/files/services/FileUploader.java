package eu.alefzero.owncloud.files.services;

import java.io.File;

import eu.alefzero.owncloud.AccountUtils;
import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.authenticator.AccountAuthenticator;
import eu.alefzero.owncloud.utils.OwnCloudVersion;
import eu.alefzero.webdav.OnUploadProgressListener;
import eu.alefzero.webdav.WebdavClient;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
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

public class FileUploader extends Service implements OnUploadProgressListener {

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
    private AccountManager mAccountManager;
    private Account mAccount;
    private String[] mLocalPaths, mRemotePaths;
    private boolean mResult;
    private int mUploadType;
    private Notification mNotification;
    private int mTotalDataToSend, mSendData;
    private int mCurrentIndexUpload, mPreviousPercent;

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
        mAccountManager = AccountManager.get(this);
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
        if (mResult) {
            Toast.makeText(this, "Upload successfull", Toast.LENGTH_SHORT)
                    .show();
        } else {
            Toast.makeText(this, "No i kupa", Toast.LENGTH_SHORT).show();
        }
    }

    public void uploadFile() {
        String baseUrl = mAccountManager.getUserData(mAccount,
                AccountAuthenticator.KEY_OC_BASE_URL), ocVerStr = mAccountManager
                .getUserData(mAccount, AccountAuthenticator.KEY_OC_VERSION);
        OwnCloudVersion ocVer = new OwnCloudVersion(ocVerStr);
        String webdav_path = AccountUtils.getWebdavPath(ocVer);
        Uri ocUri = Uri.parse(baseUrl + webdav_path);
        String username = mAccount.name.substring(0,
                mAccount.name.lastIndexOf('@'));
        String password = mAccountManager.getPassword(mAccount);
        
        mTotalDataToSend = mSendData = mPreviousPercent = 0;
        
        mNotification = new Notification(
                eu.alefzero.owncloud.R.drawable.icon, "Uploading...",
                System.currentTimeMillis());
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        mNotification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.progressbar_layout);
        mNotification.contentView.setProgressBar(R.id.status_progress, 100, 0, false);
        mNotification.contentView.setImageViewResource(R.id.status_icon, R.drawable.icon);

        mNotificationManager.notify(42, mNotification);

        WebdavClient wc = new WebdavClient(ocUri);
        wc.allowUnsignedCertificates();
        wc.setUploadListener(this);
        wc.setCredentials(username, password);

        for (int i = 0; i < mLocalPaths.length; ++i) {
            File f = new File(mLocalPaths[i]);
            mTotalDataToSend += f.length();
        }
        
        Log.d(TAG, "Will upload " + mTotalDataToSend + " bytes, with " + mLocalPaths.length + " files");
        
        for (int i = 0; i < mLocalPaths.length; ++i) {
            String mimeType = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(
                            mLocalPaths[i].substring(mLocalPaths[i]
                                    .lastIndexOf('.') + 1));
            mResult = false;
            mCurrentIndexUpload = i;
            if (wc.putFile(mLocalPaths[i], mRemotePaths[i], mimeType)) {
                mResult |= true;
            }
        }
        // notification.contentView.setProgressBar(R.id.status_progress,
        // mLocalPaths.length-1, mLocalPaths.length-1, false);
        mNotificationManager.cancel(42);
        run();
    }

    @Override
    public void OnUploadProgress(long currentProgress) {
        mSendData += currentProgress;
        int percent = (int)(100*mSendData/mTotalDataToSend);
        if (percent != mPreviousPercent) {
            String text = String.format("%d%% Uploading %s file", percent, new File(mLocalPaths[mCurrentIndexUpload]).getName());
            mNotification.contentView.setProgressBar(R.id.status_progress, 100, percent, false);
            mNotification.contentView.setTextViewText(R.id.status_text, text);
            mNotificationManager.notify(42, mNotification);
        }
        mPreviousPercent = percent;
    }
}
