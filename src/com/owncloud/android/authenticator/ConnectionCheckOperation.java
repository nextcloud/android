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

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONException;
import org.json.JSONObject;

import com.owncloud.android.AccountUtils;
import com.owncloud.android.operations.RemoteOperation;
import com.owncloud.android.operations.RemoteOperationResult;
import com.owncloud.android.utils.OwnCloudVersion;

import eu.alefzero.webdav.WebdavClient;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.util.Log;

public class ConnectionCheckOperation extends RemoteOperation {
    
    /** Maximum time to wait for a response from the server when the connection is being tested, in MILLISECONDs.  */
    public static final int TRY_CONNECTION_TIMEOUT = 5000;
    
    private static final String TAG = ConnectionCheckOperation.class.getCanonicalName();
    
    private String mUrl;
    private RemoteOperationResult mLatestResult;
    private Context mContext;
    private OwnCloudVersion mOCVersion;

    public ConnectionCheckOperation(String url, Context context) {
        mUrl = url;
        mContext = context;
        mOCVersion = null;
    }
    
    public OwnCloudVersion getDiscoveredVersion() {
        return mOCVersion;
    }

    private boolean tryConnection(WebdavClient wc, String urlSt) {
        boolean retval = false;
        GetMethod get = null;
        try {
            get = new GetMethod(urlSt);
            int status = wc.executeMethod(get, TRY_CONNECTION_TIMEOUT, TRY_CONNECTION_TIMEOUT);
            String response = get.getResponseBodyAsString();
            if (status == HttpStatus.SC_OK) {
                JSONObject json = new JSONObject(response);
                if (!json.getBoolean("installed")) {
                    mLatestResult = new RemoteOperationResult(RemoteOperationResult.ResultCode.INSTANCE_NOT_CONFIGURED);
                } else {
                    mOCVersion = new OwnCloudVersion(json.getString("version"));
                    if (!mOCVersion.isVersionValid()) {
                        mLatestResult = new RemoteOperationResult(RemoteOperationResult.ResultCode.BAD_OC_VERSION);
                        
                    } else {
                        retval = true;
                    }
                }
                
            } else {
                mLatestResult = new RemoteOperationResult(false, status);
            }

        } catch (JSONException e) {
            mLatestResult = new RemoteOperationResult(RemoteOperationResult.ResultCode.INSTANCE_NOT_CONFIGURED);
            //Log.e(TAG, "JSON exception while trying connection (instance not configured) ", e);
            
        } catch (Exception e) {
            mLatestResult = new RemoteOperationResult(e);
            //Log.e(TAG, "Unexpected exception while trying connection", e);
            
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

	@Override
	protected RemoteOperationResult run(WebdavClient client) {
        if (!isOnline()) {
        	return new RemoteOperationResult(RemoteOperationResult.ResultCode.NO_NETWORK_CONNECTION);
        }
        if (mUrl.startsWith("http://") || mUrl.startsWith("https://")) {
            mLatestResult = new RemoteOperationResult(
                                mUrl.startsWith("https://") ? RemoteOperationResult.ResultCode.OK_SSL : RemoteOperationResult.ResultCode.OK_NO_SSL
                            );
            tryConnection(client, mUrl + AccountUtils.STATUS_PATH);
            return mLatestResult;
            
        } else {
            client.setBaseUri(Uri.parse("https://" + mUrl + AccountUtils.STATUS_PATH));
            if (tryConnection(client, "https://" + mUrl + AccountUtils.STATUS_PATH)) {
                return new RemoteOperationResult(RemoteOperationResult.ResultCode.OK_SSL);
        			
            } else if (!mLatestResult.isSslRecoverableException()) {
                
                Log.d(TAG, "establishing secure connection failed, trying non secure connection");
                client.setBaseUri(Uri.parse("http://" + mUrl + AccountUtils.STATUS_PATH));
                if (tryConnection(client, "http://" + mUrl + AccountUtils.STATUS_PATH)) {
                    return new RemoteOperationResult(RemoteOperationResult.ResultCode.OK_NO_SSL);
                }
            }
            return mLatestResult;
        }
	}
	
}
