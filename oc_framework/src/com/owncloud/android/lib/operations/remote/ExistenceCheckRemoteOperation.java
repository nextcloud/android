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

package com.owncloud.android.lib.operations.remote;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.HeadMethod;

import com.owncloud.android.lib.network.OwnCloudClient;
import com.owncloud.android.lib.network.webdav.WebdavUtils;
import com.owncloud.android.lib.operations.common.RemoteOperation;
import com.owncloud.android.lib.operations.common.RemoteOperationResult;

import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;

/**
 * Operation to check the existence or absence of a path in a remote server.
 * 
 * @author David A. Velasco
 */
public class ExistenceCheckRemoteOperation extends RemoteOperation {
    
    /** Maximum time to wait for a response from the server in MILLISECONDs.  */
    public static final int TIMEOUT = 10000;
    
    private static final String TAG = ExistenceCheckRemoteOperation.class.getSimpleName();
    
    private String mPath;
    private Context mContext;
    private boolean mSuccessIfAbsent;

    
    /**
     * Full constructor. Success of the operation will depend upon the value of successIfAbsent.
     * 
     * @param path              Path to append to the URL owned by the client instance.
     * @param context           Android application context.
     * @param successIfAbsent   When 'true', the operation finishes in success if the path does NOT exist in the remote server (HTTP 404).
     */
    public ExistenceCheckRemoteOperation(String path, Context context, boolean successIfAbsent) {
        mPath = (path != null) ? path : "";
        mContext = context;
        mSuccessIfAbsent = successIfAbsent;
    }
    

	@Override
	protected RemoteOperationResult run(OwnCloudClient client) {
        if (!isOnline()) {
            return new RemoteOperationResult(RemoteOperationResult.ResultCode.NO_NETWORK_CONNECTION);
        }
        RemoteOperationResult result = null;
        HeadMethod head = null;
        try {
            head = new HeadMethod(client.getBaseUri() + WebdavUtils.encodePath(mPath));
            int status = client.executeMethod(head, TIMEOUT, TIMEOUT);
            client.exhaustResponse(head.getResponseBodyAsStream());
            boolean success = (status == HttpStatus.SC_OK && !mSuccessIfAbsent) || (status == HttpStatus.SC_NOT_FOUND && mSuccessIfAbsent);
            result = new RemoteOperationResult(success, status, head.getResponseHeaders());
            Log.d(TAG, "Existence check for " + client.getBaseUri() + WebdavUtils.encodePath(mPath) + " targeting for " + (mSuccessIfAbsent ? " absence " : " existence ") + "finished with HTTP status " + status + (!success?"(FAIL)":""));
            
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log.e(TAG, "Existence check for " + client.getBaseUri() + WebdavUtils.encodePath(mPath) + " targeting for " + (mSuccessIfAbsent ? " absence " : " existence ") + ": " + result.getLogMessage(), result.getException());
            
        } finally {
            if (head != null)
                head.releaseConnection();
        }
        return result;
	}

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) mContext
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm != null && cm.getActiveNetworkInfo() != null
                && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }


}
