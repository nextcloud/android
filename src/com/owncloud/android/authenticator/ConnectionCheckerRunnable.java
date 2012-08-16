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

package com.owncloud.android.authenticator;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONException;
import org.json.JSONObject;

import com.owncloud.android.AccountUtils;
import com.owncloud.android.authenticator.OnConnectCheckListener.ResultType;
import com.owncloud.android.utils.OwnCloudClientUtils;
import com.owncloud.android.utils.OwnCloudVersion;

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
            mLatestResult = (mUrl.startsWith("https://"))? ResultType.OK_SSL : ResultType.OK_NO_SSL;
            tryConnection(mUrl + AccountUtils.STATUS_PATH);
            postResult(mLatestResult);
        } else {
            if (tryConnection("https://" + mUrl + AccountUtils.STATUS_PATH)) {
                postResult(ResultType.OK_SSL);
                return;
            }
            Log.d(TAG,
                    "establishing secure connection failed, trying non secure connection");

            if (tryConnection("http://" + mUrl + AccountUtils.STATUS_PATH)) {
                postResult(ResultType.OK_NO_SSL);
                return;
            }
            postResult(mLatestResult);
        }
    }

    public OwnCloudVersion getDiscoveredVersion() {
        return mOCVersion;
    }

    private boolean tryConnection(String urlSt) {
        boolean retval = false;
        GetMethod get = null;
        try {
            WebdavClient wc = OwnCloudClientUtils.createOwnCloudClient(Uri.parse(urlSt));
            get = new GetMethod(urlSt);
            int status = wc.executeMethod(get, TRY_CONNECTION_TIMEOUT, TRY_CONNECTION_TIMEOUT);
            String response = get.getResponseBodyAsString();
            switch (status) {
            case HttpStatus.SC_OK: {
                JSONObject json = new JSONObject(response);
                if (!json.getBoolean("installed")) {
                    mLatestResult = ResultType.INSTANCE_NOT_CONFIGURED;
                    break;
                }
                mOCVersion = new OwnCloudVersion(json.getString("version"));
                if (!mOCVersion.isVersionValid()) {
                    mLatestResult = ResultType.BAD_OC_VERSION;
                    break;
                }
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

        } catch (JSONException e) {
            mLatestResult = ResultType.INSTANCE_NOT_CONFIGURED;
            Log.e(TAG, "JSON exception while trying connection (instance not configured) ", e);
            
        } catch (SocketException e) {  
            mLatestResult = ResultType.WRONG_CONNECTION;
            Log.e(TAG, "Socket exception while trying connection", e);
            
        } catch (SocketTimeoutException e) { 
            mLatestResult = ResultType.TIMEOUT;
            Log.e(TAG, "Socket timeout exception while trying connection", e);
            
        } catch (MalformedURLException e) {
            mLatestResult = ResultType.INCORRECT_ADDRESS;
            Log.e(TAG, "Connect exception while trying connection", e);
            
        } catch (UnknownHostException e) {
            mLatestResult = ResultType.HOST_NOT_AVAILABLE;
            Log.e(TAG, "Unknown host exception while trying connection", e);
            
        } catch (SSLPeerUnverifiedException e) { // specially meaningful SSLException
            mLatestResult = ResultType.SSL_UNVERIFIED_SERVER;
            Log.e(TAG, "SSL Peer Unverified exception while trying connection", e);
            
        } catch (SSLException e) {  
            mLatestResult = ResultType.SSL_INIT_ERROR;
            Log.e(TAG, "SSL exception while trying connection", e);
            
        } catch (ConnectTimeoutException e) {   // timeout specific exception from org.apache.commons.httpclient
            mLatestResult = ResultType.TIMEOUT;
            Log.e(TAG, "Socket timeout exception while trying connection", e);
            
        } catch (HttpException e) { // other specific exceptions from org.apache.commons.httpclient
            mLatestResult = ResultType.UNKNOWN_ERROR;
            Log.e(TAG, "HTTP exception while trying connection", e);
            
        } catch (IOException e) {   // UnkownsServiceException, and any other transport exceptions that could occur
            mLatestResult = ResultType.UNKNOWN_ERROR;
            Log.e(TAG, "I/O exception while trying connection", e);
            
        } catch (Exception e) {
            mLatestResult = ResultType.UNKNOWN_ERROR;
            Log.e(TAG, "Unexpected exception while trying connection", e);
            
        } finally {
            if (get != null)
                get.releaseConnection();
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
