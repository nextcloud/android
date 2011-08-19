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

package eu.alefzero.owncloud.authenticator;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

public class AuthUtils {
  public static final String WEBDAV_PATH_1_2 = "/webdav/owncloud.php";
  public static final String WEBDAV_PATH_2_0 = "/files/webdav.php";
  
  private static String mResultMsg = "";
  
  public static boolean authenticate(URL url, String username, String password,
                                     Handler handler, Context context) {
    String strippedPath = url.toString().endsWith("/") ?
                          url.toString().substring(0, url.toString().length()-1) :
                          url.toString();
    String webdatPath = strippedPath + WEBDAV_PATH_2_0;
    URL complete_url = null;
    try {
      complete_url = new URL(webdatPath);
    } catch (MalformedURLException e) {
      // should never happend
      sendResult(false, handler, context, "URL error");
      return false;
    }
    
    // version 2.0 success
    if (tryGetWebdav(complete_url, username, password, handler, context)) {
      sendResult(true, handler, context, complete_url.toString());
      return true;
    }
    
    if (mResultMsg.equals("401")) {
       sendResult(false, handler, context, "Invalid login or/and password");
       return false;
    }
    
    if (!mResultMsg.equals("404")) {
      sendResult(false, handler, context, "Server error: " + mResultMsg);
      return false;
    }
    
    webdatPath = strippedPath + WEBDAV_PATH_1_2;
    try {
      complete_url = new URL(webdatPath);
    } catch (MalformedURLException e) {
      // should never happend
      sendResult(false, handler, context, "URL error");
      return false;
    }
    
    // version 1.2 success
    if (tryGetWebdav(complete_url, username, password, handler, context)) {
      sendResult(true, handler, context, complete_url.toString());
      return true;
    }
    
    if (mResultMsg.equals("401")) {
      sendResult(false, handler, context, "Invalid login or/and password");
      return false;
    }
    
    if (mResultMsg.equals("404")) {
      sendResult(false, handler, context, "Wrong path given");
      return false;
    }
    
    sendResult(false, handler, context, "Server error: " + mResultMsg);
    return false;
  }

  public static boolean tryGetWebdav(URL url, String username, String pwd,
                                     Handler handler, Context context) {
    DefaultHttpClient c = new DefaultHttpClient();
    c.getCredentialsProvider().setCredentials(
        new AuthScope(url.getHost(), (url.getPort() == -1)?80:url.getPort()), 
        new UsernamePasswordCredentials(username, pwd));
    
    BasicHttpContext localcontext  = new BasicHttpContext();
    BasicScheme basicAuth = new BasicScheme();

    localcontext.setAttribute("preemptive-auth", basicAuth);
    HttpHost targetHost = new HttpHost(url.getHost(), (url.getPort() == -1)
        ? 80
        : url.getPort(), (url.getProtocol() == "https") ? "https" : "http");
    HttpHead httpget = new HttpHead(url.toString());
    HttpResponse response = null;
    try {
      response = c.execute(targetHost, httpget, localcontext);
    } catch (ClientProtocolException e1) {
      sendResult(false, handler, context, "Protocol error: "
          + e1.getLocalizedMessage());
      return false;
    } catch (UnknownHostException e1) {
      mResultMsg = "Unknowh host: " + e1.getLocalizedMessage();
      return false;
    } catch (IOException e1) {
      mResultMsg = "Error: " + e1.getLocalizedMessage();
      return false;
    }
    String status = response.getStatusLine().toString();
    status = status.split(" ")[1];
    Log.i("AuthUtils", "Status returned: " + status);
    if (status.equals("200")) {
      return true;
    } else if (status.equals("404")) {
      mResultMsg = "404";
      return false;
    } else if (status.equals("401")) {
      mResultMsg = "401";
      return false;
    }
    mResultMsg = status;
    return false;
  }
  
  public static Thread performOnBackgroundThread(final Runnable r) {
    final Thread t = new Thread() {
      @Override
      public void run() {
        try {
          r.run();
        } finally {}
      }
    };
    t.start();
    return t;
  }
  
  public static void sendResult(final Boolean result,
                                final Handler handler,
                                final Context context,
                                final String message) {
    if (handler == null || context == null) {
      return;
    }
    handler.post(new Runnable() {
      public void run() {
        ((AuthenticatorActivity) context).onAuthenticationResult(result, message); 
      }
    });
  }
  
  public static Thread attemptAuth(final URL url, final String username,
                                   final String password, final Handler handler,
                                   final Context context) {
    final Runnable r = new Runnable() {
      
      public void run() {
        authenticate(url, username, password, handler, context);
      }
    };
    return performOnBackgroundThread(r);
  }
}
