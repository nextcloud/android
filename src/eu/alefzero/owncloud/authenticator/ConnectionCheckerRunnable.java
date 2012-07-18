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

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLHandshakeException;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONException;
import org.json.JSONObject;

import eu.alefzero.owncloud.AccountUtils;
import eu.alefzero.owncloud.authenticator.OnConnectCheckListener.ResultType;
import eu.alefzero.owncloud.utils.OwnCloudVersion;
import eu.alefzero.webdav.WebdavClient;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

public class ConnectionCheckerRunnable implements Runnable {
    
    /** Maximum time to wait for a response from the server when the connection is being tested, in MILLISECONDs.  */
    public static final int TRY_CONNECTION_TIMEOUT = 5000;
    
    private static final String TAG = "ConnectionCheckerRunnable";
    private OnConnectCheckListener mListener;
    private String mUrl;
    private Handler mHandler;
    private ResultType mLatestResult;
    private Context mContext;
    private OwnCloudVersion mOCVersion;

    public void setListener(OnConnectCheckListener listener, Handler handler) {
        mListener = listener;
        mHandler = handler;
    }

    public ConnectionCheckerRunnable(String url, Context context) {
        mListener = null;
        mHandler = null;
        mUrl = url;
        mContext = context;
        mOCVersion = null;
    }

    @Override
    public void run() {

        if (!isOnline()) {
            postResult(ResultType.NO_NETWORK_CONNECTION);
            return;
        }
        if (mUrl.startsWith("http://") || mUrl.startsWith("https://")) {
            mLatestResult = ResultType.OK;
            tryConnection(Uri.parse(mUrl + AccountUtils.STATUS_PATH));
            postResult(mLatestResult);
        } else {
            Uri uri = Uri.parse("https://" + mUrl + AccountUtils.STATUS_PATH);
            if (tryConnection(uri)) {
                postResult(ResultType.OK);
                return;
            }
            Log.d(TAG,
                    "establishing secure connection failed, trying non secure connection");
            uri = Uri.parse("http://" + mUrl + AccountUtils.STATUS_PATH);

            if (tryConnection(uri)) {
                postResult(ResultType.OK_NO_SSL);
                return;
            }
            postResult(mLatestResult);
        }
    }

    public OwnCloudVersion getDiscoveredVersion() {
        return mOCVersion;
    }

    private boolean tryConnection(Uri uri) {
        WebdavClient wc = new WebdavClient();
        wc.allowSelfsignedCertificates();
        GetMethod get = new GetMethod(uri.toString());
        boolean retval = false;
        try {
            int status = wc.executeMethod(get, TRY_CONNECTION_TIMEOUT);
            switch (status) {
            case HttpStatus.SC_OK: {
                String response = get.getResponseBodyAsString();
                JSONObject json = new JSONObject(response);
                if (!json.getBoolean("installed")) {
                    mLatestResult = ResultType.INSTANCE_NOT_CONFIGURED;
                    break;
                }
                mOCVersion = new OwnCloudVersion(json.getString("version"));
                if (!mOCVersion.isVersionValid())
                    break;
                retval = true;
                break;
            }
            case HttpStatus.SC_NOT_FOUND:
                mLatestResult = ResultType.FILE_NOT_FOUND;
                break;
            case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                mLatestResult = ResultType.INSTANCE_NOT_CONFIGURED;
                break;
            default:
                mLatestResult = ResultType.UNKNOWN_ERROR;
                Log.e(TAG, "Not handled status received from server: " + status);
            }

        } catch (Exception e) {
            if (e instanceof UnknownHostException
                    || e instanceof ConnectException 
                    || e instanceof SocketTimeoutException) {
                mLatestResult = ResultType.HOST_NOT_AVAILABLE;
            } else if (e instanceof JSONException) {
                mLatestResult = ResultType.INSTANCE_NOT_CONFIGURED;
            } else if (e instanceof SSLHandshakeException) {
                mLatestResult = ResultType.SSL_INIT_ERROR;
            } else {
                mLatestResult = ResultType.UNKNOWN_ERROR;
            }
            e.printStackTrace();
        }

        return retval;
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm != null && cm.getActiveNetworkInfo() != null
                && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    private void postResult(final ResultType result) {
        if (mHandler != null && mListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onConnectionCheckResult(result);
                }
            });
        }
    }

}
