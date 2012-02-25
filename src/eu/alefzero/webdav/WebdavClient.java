/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package eu.alefzero.webdav;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

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
  
  public DefaultHttpClient getHttpClient() {
    return mHttpClient;
  }
  public HttpHost getTargetHost() {
    return mTargetHost;
  }
  
  public WebdavClient(Uri uri) {
    mUri = uri;
    mSchemeRegistry = new SchemeRegistry();
    setupHttpClient();
  }
  
  public void setCredentials(String username, String password) {
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
  
  public void allowUnsignedCertificates() {
    // https
    mSchemeRegistry.register(new Scheme("https", new EasySSLSocketFactory(), 443));
  }
  
  public boolean downloadFile(String filepath, File targetPath) {
    HttpGet get = new HttpGet(mUri.toString() + filepath.replace(" ", "%20"));
    get.setHeader("Host", mUri.getHost());
    get.setHeader("User-Agent", "Android-ownCloud");
    
    try {
      HttpResponse response = mHttpClient.execute(mTargetHost, get, mHttpContext);
      if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
        return false;
      }
      BufferedInputStream bis = new BufferedInputStream(response.getEntity().getContent());
      FileOutputStream fos = new FileOutputStream(targetPath);
      
      byte[] bytes = new byte[512];
      int readResult;
      while ((readResult = bis.read(bytes)) != -1) fos.write(bytes, 0, readResult);
      
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
    return true;
  }
  
  public boolean putFile(String localFile,
                  String remoteTarget,
                  String contentType) {
    boolean result = true;
    HttpPut method = new HttpPut(mUri.toString() + remoteTarget.replace(" ", "%20"));
    method.setHeader("Content-type", contentType);
    method.setHeader("Host", mUri.getHost());
    method.setHeader("User-Agent", "Android-ownCloud");

    try {
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
      Log.i(TAG, ""+e.getMessage());
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
