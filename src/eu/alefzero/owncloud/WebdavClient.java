package eu.alefzero.owncloud;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerPNames;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;

import eu.alefzero.owncloud.authenticator.EasySSLSocketFactory;
import eu.alefzero.webdav.HttpMkCol;

import android.net.Uri;
import android.util.Log;

public class WebdavClient {
  private DefaultHttpClient mHttpClient;
  private BasicHttpContext mHttpContext;
  private HttpHost mTargetHost;
  private SchemeRegistry mSchemeRegistry;
  private Uri mUri;
  final private static String TAG = "WebdavClient";
  
  WebdavClient(Uri uri) {
    mUri = uri;
    mSchemeRegistry = new SchemeRegistry();
    setupHttpClient();
  }
  
  void setCredentials(String username, String password) {
    // determine default port for http or https
    int targetPort = mTargetHost.getPort() == -1 ? 
                        ( mUri.getScheme().equals("https") ? 443 : 80)
                        : mUri.getPort();

    mHttpClient.getCredentialsProvider().setCredentials(
        new AuthScope(mUri.getHost(), targetPort), 
        new UsernamePasswordCredentials(username, password));
    BasicScheme basicAuth = new BasicScheme();
    mHttpContext.setAttribute("preemptive-auth", basicAuth);
  }
  
  void allowUnsignedCertificates() {
    // https
    mSchemeRegistry.register(new Scheme("https", new EasySSLSocketFactory(), 443));
  }
  
  boolean downloadFile(String filepath, File targetPath) {
    HttpGet get = new HttpGet(mUri.toString() + filepath.replace(" ", "%20"));
    get.setHeader("Host", mUri.getHost());
    get.setHeader("User-Agent", "Android-ownCloud");
    
    try {
      HttpResponse response = mHttpClient.execute(mTargetHost, get, mHttpContext);
      if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
        return false;
      }
      InputStreamReader isr = new InputStreamReader(response.getEntity().getContent());
      FileOutputStream fos = new FileOutputStream(targetPath);
      int oneByte;
      while ((oneByte = isr.read()) != -1) fos.write(oneByte);
      
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }
  
  void getFileList(String dirPath) {
    
  }
  
  boolean putFile(String localFile,
                  String remoteTarget,
                  String contentType) {
    boolean result = true;
    HttpPut method = new HttpPut(mUri.toString() + remoteTarget.replace(" ", "%20"));
    method.setHeader("Content-type", contentType);
    method.setHeader("Host", mUri.getHost());
    method.setHeader("User-Agent", "Android-ownCloud");

    try {
      FileBody fb = new FileBody(new File(localFile, contentType));
      final FileEntity fileEntity = new FileEntity(new File(localFile), contentType);

      method.setEntity(fileEntity);
      Log.i(TAG, "executing:" + method.getRequestLine().toString());

      mHttpClient.execute(mTargetHost, method, mHttpContext);
      /*mHandler.post(new Runnable() {
      public void run() {
        Uploader.this.PartialupdateUpload(c.getString(c.getColumnIndex(Media.DATA)),
                                                  c.getString(c.getColumnIndex(Media.DISPLAY_NAME)),
                                                  mUploadPath + (mUploadPath.equals("/")?"":"/"),
                                                  fileEntity.getContentType().getValue(),
                                                  fileEntity.getContentLength()+"");
      }
    });
    Log.i(TAG, "Uploading, done");
*/
      Log.i(TAG, "Uploading, done");
    } catch (final Exception e) {
      Log.i(TAG, e.getLocalizedMessage());
      result = false;
    }
    
    return result;
  }
  
  public boolean createDirectory(String path) {
    HttpMkCol method = new HttpMkCol(mUri.toString() + path + "/");
    method.setHeader("User-Agent", "Android-ownCloud");
    
    try {
      mHttpClient.execute(mTargetHost, method, mHttpContext);
      Log.i(TAG, "Creating dir completed");
    } catch (final Exception e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }
  
  private void setupHttpClient() {
    // http scheme
    mSchemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
    mSchemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
    
    HttpParams params = new BasicHttpParams();
    params.setParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS, 30);
    params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRouteBean(30));
    params.setParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);
    HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);

    mHttpContext = new BasicHttpContext();
    ClientConnectionManager cm = new ThreadSafeClientConnManager(params, mSchemeRegistry);

    int port = mUri.getPort() == -1 ? 
                 mUri.getScheme().equals("https") ? 443 : 80
               : mUri.getPort();
    
    mTargetHost = new HttpHost(mUri.getHost(), port, mUri.getScheme());
    
    mHttpClient = new DefaultHttpClient(cm, params);
  }
}
