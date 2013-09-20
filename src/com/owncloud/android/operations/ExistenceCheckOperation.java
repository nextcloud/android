/* ownCloud Android client application
 *   Copyright (C) 2012 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
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

package com.owncloud.android.operations;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.HeadMethod;

import com.owncloud.android.Log_OC;

import eu.alefzero.webdav.WebdavClient;
import android.content.Context;
import android.net.ConnectivityManager;

/**
 * Operation to check the existence or absence of a path in a remote server.
 * 
 * @author David A. Velasco
 */
public class ExistenceCheckOperation extends RemoteOperation {
    
    /** Maximum time to wait for a response from the server in MILLISECONDs.  */
    public static final int TIMEOUT = 10000;
    
    private static final String TAG = ExistenceCheckOperation.class.getSimpleName();
    
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
    public ExistenceCheckOperation(String path, Context context, boolean successIfAbsent) {
        mPath = (path != null) ? path : "";
        mContext = context;
        mSuccessIfAbsent = successIfAbsent;
    }
    

	@Override
	protected RemoteOperationResult run(WebdavClient client) {
        if (!isOnline()) {
            return new RemoteOperationResult(RemoteOperationResult.ResultCode.NO_NETWORK_CONNECTION);
        }
        RemoteOperationResult result = null;
        HeadMethod head = null;
        try {
            head = new HeadMethod(client.getBaseUri() + mPath);
            int status = client.executeMethod(head, TIMEOUT, TIMEOUT);
            client.exhaustResponse(head.getResponseBodyAsStream());
            boolean success = (status == HttpStatus.SC_OK && !mSuccessIfAbsent) || (status == HttpStatus.SC_NOT_FOUND && mSuccessIfAbsent);
            result = new RemoteOperationResult(success, status, head.getResponseHeaders());
            Log_OC.d(TAG, "Existence check for " + client.getBaseUri() + mPath + " targeting for " + (mSuccessIfAbsent ? " absence " : " existence ") + "finished with HTTP status " + status + (!success?"(FAIL)":""));
            
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Existence check for " + client.getBaseUri() + mPath + " targeting for " + (mSuccessIfAbsent ? " absence " : " existence ") + ": " + result.getLogMessage(), result.getException());
            
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
