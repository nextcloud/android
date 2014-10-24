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

import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.GetRemoteStatusOperation;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.operations.DetectAuthenticationMethodOperation.AuthenticationMethod;

import android.content.Context;

/**
 * Get basic information from an ownCloud server given its URL.
 * 
 * Checks the existence of a configured ownCloud server in the URL, gets its version 
 * and finds out what authentication method is needed to access files in it.
 * 
 * @author David A. Velasco
 * @author masensio
 */

public class GetServerInfoOperation extends RemoteOperation {
    
    private static final String TAG = GetServerInfoOperation.class.getSimpleName();
    
    private String mUrl;
    private Context mContext;
    
    private ServerInfo mResultData;

    /** 
     * Constructor.
     * 
     * @param url               URL to an ownCloud server.
     * @param context           Android context; needed to check network state
     *                          TODO ugly dependency, get rid of it. 
     */
    public GetServerInfoOperation(String url, Context context) {
        mUrl = trimWebdavSuffix(url);
        mContext = context;
        
        mResultData = new ServerInfo();
    }
    
    
    /**
     * Performs the operation
     * 
     * @return      Result of the operation. If successful, includes an instance of 
     *              {@link ServerInfo} with the information retrieved from the server. 
     *              Call {@link RemoteOperationResult#getData()}.get(0) to get it.
     */
	@Override
	protected RemoteOperationResult run(OwnCloudClient client) {
	    
	    // first: check the status of the server (including its version)
	    GetRemoteStatusOperation getStatus = new GetRemoteStatusOperation(mContext);
	    RemoteOperationResult result = getStatus.execute(client);
	    
        if (result.isSuccess()) {
            // second: get authentication method required by the server
            mResultData.mVersion = (OwnCloudVersion)(result.getData().get(0));
            mResultData.mIsSslConn = (result.getCode() == ResultCode.OK_SSL);
            mResultData.mBaseUrl = normalizeProtocolPrefix(mUrl, mResultData.mIsSslConn);
            RemoteOperationResult detectAuthResult = detectAuthorizationMethod(client);
            
            // third: merge results
            if (detectAuthResult.isSuccess()) {
                mResultData.mAuthMethod = 
                        (AuthenticationMethod)detectAuthResult.getData().get(0);
                ArrayList<Object> data = new ArrayList<Object>();
                data.add(mResultData);
                result.setData(data);
            } else {
                result = detectAuthResult;
            }
        }
        return result;
	}

	
    private RemoteOperationResult detectAuthorizationMethod(OwnCloudClient client) {
        Log_OC.d(TAG, "Trying empty authorization to detect authentication method");
        DetectAuthenticationMethodOperation operation = 
                new DetectAuthenticationMethodOperation(mContext);
        return operation.execute(client);
    }
    

    private String trimWebdavSuffix(String url) {
        if (url == null) {
            url = "";
        } else {
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }
            if(url.toLowerCase().endsWith(AccountUtils.WEBDAV_PATH_4_0)){
                url = url.substring(0, url.length() - AccountUtils.WEBDAV_PATH_4_0.length());
            } else if(url.toLowerCase().endsWith(AccountUtils.WEBDAV_PATH_2_0)){
                url = url.substring(0, url.length() - AccountUtils.WEBDAV_PATH_2_0.length());
            } else if (url.toLowerCase().endsWith(AccountUtils.WEBDAV_PATH_1_2)){
                url = url.substring(0, url.length() - AccountUtils.WEBDAV_PATH_1_2.length());
            }
        }
        return url;
    }

    
    private String normalizeProtocolPrefix(String url, boolean isSslConn) {
        if (!url.toLowerCase().startsWith("http://") &&
                !url.toLowerCase().startsWith("https://")) {
            if (isSslConn) {
                return "https://" + url;
            } else {
                return "http://" + url;
            }
        }
        return url;
    }
    
    
    public static class ServerInfo {
        public OwnCloudVersion mVersion = null;
        public String mBaseUrl = "";
        public AuthenticationMethod mAuthMethod = AuthenticationMethod.UNKNOWN;
        public boolean mIsSslConn = false;
    }
	
}
