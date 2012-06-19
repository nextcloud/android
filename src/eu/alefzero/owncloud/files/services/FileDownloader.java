package eu.alefzero.owncloud.files.services;

import java.io.File;
import java.io.IOException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;
import android.widget.RemoteViews;
import eu.alefzero.owncloud.AccountUtils;
import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.R.drawable;
import eu.alefzero.owncloud.authenticator.AccountAuthenticator;
import eu.alefzero.owncloud.db.ProviderMeta.ProviderTableMeta;
import eu.alefzero.owncloud.files.interfaces.OnDatatransferProgressListener;
import eu.alefzero.owncloud.ui.activity.FileDisplayActivity;
import eu.alefzero.owncloud.utils.OwnCloudVersion;
import eu.alefzero.webdav.WebdavClient;

public class FileDownloader extends Service implements OnDatatransferProgressListener {
    public static final String DOWNLOAD_FINISH_MESSAGE = "DOWNLOAD_FINISH";
    public static final String EXTRA_ACCOUNT = "ACCOUNT";
    public static final String EXTRA_FILE_PATH = "FILE_PATH";
    public static final String EXTRA_FILE_SIZE = "FILE_SIZE";
    private static final String TAG = "FileDownloader";

    private NotificationManager mNotificationMngr;
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private Account mAccount;
    private String mFilePath;
    private int mLastPercent;
    private long mTotalDownloadSize;
    private long mCurrentDownlodSize;
    private Notification mNotification;

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            downloadFile();
            stopSelf(msg.arg1);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mNotificationMngr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        HandlerThread thread = new HandlerThread("FileDownladerThread",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!intent.hasExtra(EXTRA_ACCOUNT)
                && !intent.hasExtra(EXTRA_FILE_PATH)) {
            Log.e(TAG, "Not enough information provided in intent");
            return START_STICKY;
        }
        mAccount = intent.getParcelableExtra(EXTRA_ACCOUNT);
        mFilePath = intent.getStringExtra(EXTRA_FILE_PATH);
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);
        mCurrentDownlodSize = mLastPercent = 0;
        mTotalDownloadSize = intent.getLongExtra(EXTRA_FILE_SIZE, -1);

        return START_NOT_STICKY;
    }

    void downloadFile() {
        AccountManager am = (AccountManager) getSystemService(ACCOUNT_SERVICE);
        String oc_base_url = am.getUserData(mAccount, AccountAuthenticator.KEY_OC_BASE_URL);
        OwnCloudVersion ocv = new OwnCloudVersion(am
                .getUserData(mAccount, AccountAuthenticator.KEY_OC_VERSION));
        String webdav_path = AccountUtils.getWebdavPath(ocv);
        Uri oc_url = Uri.parse(oc_base_url+webdav_path);

        WebdavClient wdc = new WebdavClient(Uri.parse(oc_base_url + webdav_path));
        
        String username = mAccount.name.split("@")[0];
        String password = "";
        try {
            password = am.blockingGetAuthToken(mAccount,
                    AccountAuthenticator.AUTH_TOKEN_TYPE, true);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        wdc.setCredentials(username, password);
        wdc.allowSelfsignedCertificates();
        wdc.setDataTransferProgressListener(this);

        mNotification = new Notification(R.drawable.icon, "Downloading file", System.currentTimeMillis());

        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        mNotification.contentView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.progressbar_layout);
        mNotification.contentView.setProgressBar(R.id.status_progress, 100, 0, mTotalDownloadSize == -1);
        mNotification.contentView.setImageViewResource(R.id.status_icon, R.drawable.icon);
        // dvelasco ; contentIntent MUST be assigned to avoid app crashes in versions previous to Android 4.x ;
        //              BUT an empty Intent is not a very elegant solution; something smart should happen when a user 'clicks' on a download in the notification bar
        mNotification.contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
        
        mNotificationMngr.notify(1, mNotification);

        File sdCard = Environment.getExternalStorageDirectory();
        File file = new File(sdCard.getAbsolutePath() + "/owncloud/" + mAccount.name + mFilePath);
        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.e(TAG, file.getAbsolutePath() + " " + oc_url.toString());
        Log.e(TAG, mFilePath+"");
        if (wdc.downloadFile(mFilePath, file)) {
            ContentValues cv = new ContentValues();
            cv.put(ProviderTableMeta.FILE_STORAGE_PATH, file.getAbsolutePath());
            getContentResolver().update(
                    ProviderTableMeta.CONTENT_URI,
                    cv,
                    ProviderTableMeta.FILE_NAME + "=? AND "
                            + ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?",
                    new String[] {
                            mFilePath.substring(mFilePath.lastIndexOf('/') + 1),
                            mAccount.name });            
        }
        mNotificationMngr.cancel(1);
        Intent end = new Intent(DOWNLOAD_FINISH_MESSAGE);
        end.putExtra(EXTRA_FILE_PATH, file.getAbsolutePath());
        sendBroadcast(end);
    }

    @Override
    public void transferProgress(long progressRate) {
        mCurrentDownlodSize += progressRate;
        int percent = (int)(100.0*((double)mCurrentDownlodSize)/((double)mTotalDownloadSize));
        if (percent != mLastPercent) {
          mNotification.contentView.setProgressBar(R.id.status_progress, 100, (int)(100*mCurrentDownlodSize/mTotalDownloadSize), mTotalDownloadSize == -1);
          mNotification.contentView.setTextViewText(R.id.status_text, percent+"%");
          mNotificationMngr.notify(1, mNotification);
        }
        
        mLastPercent = percent;
    }

}
