/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2014 ownCloud Inc.
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

package com.owncloud.android.operations;

import java.util.ArrayList;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.HeadMethod;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;

import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;

/**
 * Operation to find out what authentication method requires
 * the server to access files.
 * 
 * Basically, tries to access to the root folder without authorization
 * and analyzes the response.
 * 
 * When successful, the instance of {@link RemoteOperationResult} passed
 * through {@link OnRemoteOperationListener#onRemoteOperationFinish(RemoteOperation, 
 * RemoteOperationResult)} returns in {@link RemoteOperationResult#getData()}
 * a value of {@link AuthenticationMethod}. 
 * 
 * @author David A. Velasco
 */
public class DetectAuthenticationMethodOperation extends RemoteOperation {
    
    private static final String TAG = DetectAuthenticationMethodOperation.class.getSimpleName();
    
    public enum AuthenticationMethod {
        UNKNOWN,
        NONE,
        BASIC_HTTP_AUTH, 
        SAML_WEB_SSO,
        BEARER_TOKEN
    }
    
    private String mPath;
    private Context mContext;
    private boolean mSuccessIfAbsent;
    
    /**
     * Constructor
     */
    public DetectAuthenticationMethodOperation() {
    }
    

	@Override
	protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = new RemoteOperationResult(ResultCode.OK);
        ArrayList<Object> data = new ArrayList<Object>();
        data.add(AuthenticationMethod.UNKNOWN);
        result.setData(data);
	    /*
        if (!isOnline()) {
            return new RemoteOperationResult(RemoteOperationResult.ResultCode.NO_NETWORK_CONNECTION);
        }
        RemoteOperationResult result = null;
        HeadMethod head = null;
        try {
            head = new HeadMethod(client.getWebdavUri() + WebdavUtils.encodePath(mPath));
            int status = client.executeMethod(head, TIMEOUT, TIMEOUT);
            client.exhaustResponse(head.getResponseBodyAsStream());
            boolean success = (status == HttpStatus.SC_OK && !mSuccessIfAbsent) || (status == HttpStatus.SC_NOT_FOUND && mSuccessIfAbsent);
            result = new RemoteOperationResult(success, status, head.getResponseHeaders());
            Log.d(TAG, "Existence check for " + client.getWebdavUri() + WebdavUtils.encodePath(mPath) + " targeting for " + (mSuccessIfAbsent ? " absence " : " existence ") + "finished with HTTP status " + status + (!success?"(FAIL)":""));
            
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log.e(TAG, "Existence check for " + client.getWebdavUri() + WebdavUtils.encodePath(mPath) + " targeting for " + (mSuccessIfAbsent ? " absence " : " existence ") + ": " + result.getLogMessage(), result.getException());
            
        } finally {
            if (head != null)
                head.releaseConnection();
        }
        */
        return result;
	}

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm != null && cm.getActiveNetworkInfo() != null
                && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }


}
