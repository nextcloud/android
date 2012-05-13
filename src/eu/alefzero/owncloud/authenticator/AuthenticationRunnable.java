/* ownCloud Android client application
 *   Copyright (C) 2012 Bartek Przybylski
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

import java.net.URL;

import org.apache.commons.httpclient.HttpStatus;

import eu.alefzero.webdav.WebdavClient;

import android.net.Uri;
import android.os.Handler;

public class AuthenticationRunnable implements Runnable {

  private OnAuthenticationResultListener mListener;
  private Handler mHandler;
  private URL mUrl;
  private String mUsername;
  private String mPassword;

  public AuthenticationRunnable(URL url, String username, String password) {
    mListener = null;
    mUrl = url;
    mUsername = username;
    mPassword = password;
  }
  
  public void setOnAuthenticationResultListener(OnAuthenticationResultListener listener, Handler handler) {
    mListener = listener;
    mHandler = handler;
  }
  
  @Override
  public void run() {
    Uri uri;
    uri = Uri.parse(mUrl.toString());
    WebdavClient client = new WebdavClient(uri);
    client.setCredentials(mUsername, mPassword);
    int login_result = client.tryToLogin(); 
    switch (login_result) {
      case HttpStatus.SC_OK:
        postResult(true, uri.toString());
        break;
      case HttpStatus.SC_UNAUTHORIZED:
        postResult(false, "Invalid login or/and password");
        break;
      case HttpStatus.SC_NOT_FOUND:
        postResult(false, "Wrong path given");
        break;
      default:
        postResult(false, "Internal server error, code: " + login_result);
    }
  }

  private void postResult(final boolean success, final String message) {
    if (mHandler != null && mListener != null) {
      mHandler.post(new Runnable() {
        @Override
        public void run() {
          mListener.onAuthenticationResult(success, message);
        }
      });
    }
  }
}
