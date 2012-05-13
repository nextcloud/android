package eu.alefzero.owncloud;

import java.io.File;

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
import eu.alefzero.owncloud.authenticator.AccountAuthenticator;
import eu.alefzero.owncloud.db.ProviderMeta.ProviderTableMeta;
import eu.alefzero.owncloud.ui.activity.FileDisplayActivity;
import eu.alefzero.webdav.WebdavClient;

public class FileDownloader extends Service {
  public static final String DOWNLOAD_FINISH_MESSAGE = "DOWNLOAD_FINISH";
  public static final String EXTRA_ACCOUNT = "ACCOUNT";
  public static final String EXTRA_FILE_PATH = "FILE_PATH";
  private static final String TAG = "FileDownloader";
  
  private NotificationManager nm;
  private Looper mServiceLooper;
  private ServiceHandler mServiceHandler;
  private Account mAccount;
  private String mFilePath;
  
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
    nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    HandlerThread thread = new HandlerThread("FileDownladerThread", Process.THREAD_PRIORITY_BACKGROUND);
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
    if (!intent.hasExtra(EXTRA_ACCOUNT) && !intent.hasExtra(EXTRA_FILE_PATH)) {
      Log.e(TAG, "Not enough information provided in intent");
      return START_STICKY;
    }
    mAccount = intent.getParcelableExtra(EXTRA_ACCOUNT);
    mFilePath = intent.getStringExtra(EXTRA_FILE_PATH);
    Message msg = mServiceHandler.obtainMessage();
    msg.arg1 = startId;
    mServiceHandler.sendMessage(msg);

    return START_NOT_STICKY;
  }
  
  void downloadFile() {
    AccountManager am = (AccountManager)getSystemService(ACCOUNT_SERVICE);
    Uri oc_url = Uri.parse(am.getUserData(mAccount, AccountAuthenticator.KEY_OC_URL));

    WebdavClient wdc = new WebdavClient(oc_url);
    
    String username = mAccount.name.split("@")[0];
    String password = "";
    try {
      password = am.blockingGetAuthToken(mAccount, AccountAuthenticator.AUTH_TOKEN_TYPE, true);
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    wdc.setCredentials(username, password);
    wdc.allowUnsignedCertificates();

    Notification n = new Notification(R.drawable.icon, "Downloading file", System.currentTimeMillis());
    PendingIntent pi = PendingIntent.getActivity(this, 1, new Intent(this, FileDisplayActivity.class), 0);
    n.setLatestEventInfo(this, "Downloading file", "Downloading file " + mFilePath, pi);
    nm.notify(1, n);

    File sdCard = Environment.getExternalStorageDirectory();
    File dir = new File (sdCard.getAbsolutePath() + "/owncloud");
    dir.mkdirs();
    File file = new File(dir, mFilePath.replace('/', '.'));
    
    Log.e(TAG, file.getAbsolutePath() + " " + oc_url.toString());
    wdc.downloadFile(mFilePath, file);
    ContentValues cv = new ContentValues();
    cv.put(ProviderTableMeta.FILE_STORAGE_PATH, file.getAbsolutePath());
    getContentResolver().update(ProviderTableMeta.CONTENT_URI,
        cv,
        ProviderTableMeta.FILE_NAME +"=? AND "+ProviderTableMeta.FILE_ACCOUNT_OWNER+"=?",
        new String[]{mFilePath.substring(mFilePath.lastIndexOf('/')+1), mAccount.name});
    nm.cancel(1);
    Intent end = new Intent(DOWNLOAD_FINISH_MESSAGE);
    sendBroadcast(end);
  }
  
}
