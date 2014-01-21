/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2014 ownCloud (http://www.owncloud.org/)
 *   
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *   
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *   
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS 
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN 
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.oc_framework.operations.remote;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONException;
import org.json.JSONObject;

import com.owncloud.android.oc_framework.accounts.AccountUtils;
import com.owncloud.android.oc_framework.network.webdav.WebdavClient;
import com.owncloud.android.oc_framework.operations.RemoteOperation;
import com.owncloud.android.oc_framework.operations.RemoteOperationResult;
import com.owncloud.android.oc_framework.utils.OwnCloudVersion;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.util.Log;

/**
 * Checks if the server is valid and if the server supports the Share API
 * 
 * @author David A. Velasco
 * @author masensio
 *
 */

public class OwnCloudServerCheckOperation extends RemoteOperation {
    
    /** Maximum time to wait for a response from the server when the connection is being tested, in MILLISECONDs.  */
    public static final int TRY_CONNECTION_TIMEOUT = 5000;
    
    private static final String TAG = OwnCloudServerCheckOperation.class.getSimpleName();
    
    private static final String OCVERSION_SHARED_SUPPORTED = "5.0.13";
    
    private static final String NODE_INSTALLED = "installed";
    private static final String NODE_VERSION = "version";
    private static final String NODE_VERSIONSTRING = "versionstring";
    
    private String mUrl;
    private RemoteOperationResult mLatestResult;
    private Context mContext;
    private OwnCloudVersion mOCVersion;
    private OwnCloudVersion mOCVersionString;

    public OwnCloudServerCheckOperation(String url, Context context) {
        mUrl = url;
        mContext = context;
        mOCVersion = null;
        mOCVersionString = null;
    }
    
    public OwnCloudVersion getDiscoveredVersion() {
        return mOCVersion;
    }
    public boolean isSharedSupported() {
        OwnCloudVersion shareServer = new OwnCloudVersion(OCVERSION_SHARED_SUPPORTED);
        if (mOCVersionString != null) {
        	return mOCVersionString.compareTo(shareServer) >= 0;
        } else {
        	return false;
        }
        	
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
                if (!json.getBoolean(NODE_INSTALLED)) {
                    mLatestResult = new RemoteOperationResult(RemoteOperationResult.ResultCode.INSTANCE_NOT_CONFIGURED);
                } else {
                    mOCVersion = new OwnCloudVersion(json.getString(NODE_VERSION));
                    mOCVersionString = new OwnCloudVersion(json.getString(NODE_VERSIONSTRING), true);
                    if (!mOCVersion.isVersionValid()) {
                        mLatestResult = new RemoteOperationResult(RemoteOperationResult.ResultCode.BAD_OC_VERSION);
                        
                    } else {
                        mLatestResult = new RemoteOperationResult(urlSt.startsWith("https://") ? 
                                                                    RemoteOperationResult.ResultCode.OK_SSL : 
                                                                    RemoteOperationResult.ResultCode.OK_NO_SSL
                            );

                        retval = true;
                    }
                }
                
            } else {
                mLatestResult = new RemoteOperationResult(false, status, get.getResponseHeaders());
            }

        } catch (JSONException e) {
            mLatestResult = new RemoteOperationResult(RemoteOperationResult.ResultCode.INSTANCE_NOT_CONFIGURED);
            
        } catch (Exception e) {
            mLatestResult = new RemoteOperationResult(e);
            
        } finally {
            if (get != null)
                get.releaseConnection();
        }
        
        if (mLatestResult.isSuccess()) {
            Log.i(TAG, "Connection check at " + urlSt + ": " + mLatestResult.getLogMessage());
            
        } else if (mLatestResult.getException() != null) {
            Log.e(TAG, "Connection check at " + urlSt + ": " + mLatestResult.getLogMessage(), mLatestResult.getException());
            
        } else {
            Log.e(TAG, "Connection check at " + urlSt + ": " + mLatestResult.getLogMessage());
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
            tryConnection(client, mUrl + AccountUtils.STATUS_PATH);
            
        } else {
            client.setBaseUri(Uri.parse("https://" + mUrl + AccountUtils.STATUS_PATH));
            boolean httpsSuccess = tryConnection(client, "https://" + mUrl + AccountUtils.STATUS_PATH); 
            if (!httpsSuccess && !mLatestResult.isSslRecoverableException()) {
                Log.d(TAG, "establishing secure connection failed, trying non secure connection");
                client.setBaseUri(Uri.parse("http://" + mUrl + AccountUtils.STATUS_PATH));
                tryConnection(client, "http://" + mUrl + AccountUtils.STATUS_PATH);
            }
        }
        return mLatestResult;
	}
	
}
