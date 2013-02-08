/* ownCloud Android client application
 *   Copyright (C) 2012 Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
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

package com.owncloud.android.authenticator;

import java.net.URL;

import org.apache.commons.httpclient.HttpStatus;

import com.owncloud.android.R;
import com.owncloud.android.network.OwnCloudClientUtils;

import eu.alefzero.webdav.WebdavClient;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;

public class AuthenticationRunnable implements Runnable {

    private OnAuthenticationResultListener mListener;
    private Handler mHandler;
    private URL mUrl;
    private String mUsername;
    private String mPassword;
    private Context mContext;

    public AuthenticationRunnable(URL url, String username, String password, Context context) {
        mListener = null;
        mUrl = url;
        mUsername = username;
        mPassword = password;
        mContext = context;
    }

    public void setOnAuthenticationResultListener(
            OnAuthenticationResultListener listener, Handler handler) {
        mListener = listener;
        mHandler = handler;
    }

    @Override
    public void run() {
        Uri uri;
        uri = Uri.parse(mUrl.toString());
        WebdavClient wdc = OwnCloudClientUtils.createOwnCloudClient(uri, mUsername, mPassword, mContext);
        int login_result = wdc.tryToLogin();
        switch (login_result) {
        case HttpStatus.SC_OK:
            postResult(true, uri.toString());
            break;
        case HttpStatus.SC_UNAUTHORIZED:
            postResult(false, mContext.getString(R.string.auth_unauthorized));
            break;
        case HttpStatus.SC_NOT_FOUND:
            postResult(false, mContext.getString(R.string.auth_not_found));
            break;
        default:
            postResult(false, String.format(mContext.getString(R.string.auth_internal), login_result));
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
