package eu.alefzero.owncloud;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;
import java.util.Date;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;

import eu.alefzero.owncloud.authenticator.AccountAuthenticator;
import eu.alefzero.webdav.HttpPropFind;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.FrameLayout;

public class FileDownloader extends Service {
  static final String EXTRA_ACCOUNT = "ACCOUNT";
  static final String EXTRA_FILE_PATH = "FILE_PATH";
  static final String TAG = "OC_FileDownloader";
  
  NotificationManager nm;
  
  @Override
  public IBinder onBind(Intent arg0) {
    return null;
  }
  
  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    if (!intent.hasExtra(EXTRA_ACCOUNT) && !intent.hasExtra(EXTRA_FILE_PATH)) {
      Log.e(TAG, "Not enough information provided in intent");
      return START_NOT_STICKY;
    }
    
    nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    
    Account account = intent.getParcelableExtra(EXTRA_ACCOUNT);
    String file_path = intent.getStringExtra(EXTRA_FILE_PATH);
    AccountManager am = (AccountManager)getSystemService(ACCOUNT_SERVICE);
    Uri oc_url = Uri.parse(am.getUserData(account, AccountAuthenticator.KEY_OC_URL));

    WebdavClient wdc = new WebdavClient(oc_url);
    
    String username = account.name.split("@")[0];
    String password = "";
    try {
      password = am.blockingGetAuthToken(account, AccountAuthenticator.AUTH_TOKEN_TYPE, true);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      return START_NOT_STICKY;
    }
    
    wdc.setCredentials(username, password);
    wdc.allowUnsignedCertificates();

    Notification n = new Notification(R.drawable.icon, "Downloading file", System.currentTimeMillis());
    PendingIntent pi = PendingIntent.getActivity(this, 1, new Intent(this, OwnCloudMainScreen.class), 0);
    n.setLatestEventInfo(this, "A", "B", pi);
    nm.notify(1, n);
        
    File sdCard = Environment.getExternalStorageDirectory();
    File dir = new File (sdCard.getAbsolutePath() + "/owncloud");
    dir.mkdirs();
    File file = new File(dir, "filename");
    
    wdc.downloadFile(file_path, file);
    
    return START_NOT_STICKY;
  }

}
