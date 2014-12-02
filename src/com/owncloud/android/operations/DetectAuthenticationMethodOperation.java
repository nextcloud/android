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

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation;

import android.content.Context;
import android.net.Uri;

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
    
    private Context mContext;
    
    /**
     * Constructor
     * 
     * @param context       Android context of the caller.
     * @param webdavUrl
     */
    public DetectAuthenticationMethodOperation(Context context) {
        mContext = context;
    }
    

    /**
     *  Performs the operation.
     * 
     *  Triggers a check of existence on the root folder of the server, granting
     *  that the request is not authenticated.
     *  
     *  Analyzes the result of check to find out what authentication method, if
     *  any, is requested by the server.
     */
	@Override
	protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;
        AuthenticationMethod authMethod = AuthenticationMethod.UNKNOWN;
        
        RemoteOperation operation = new ExistenceCheckRemoteOperation("", mContext, false);
        client.clearCredentials();
        client.setFollowRedirects(false);
        
        // try to access the root folder, following redirections but not SAML SSO redirections
        result = operation.execute(client);
        String redirectedLocation = result.getRedirectedLocation(); 
        while (redirectedLocation != null && redirectedLocation.length() > 0 && 
                !result.isIdPRedirection()) {
            client.setBaseUri(Uri.parse(result.getRedirectedLocation()));
            result = operation.execute(client);
            redirectedLocation = result.getRedirectedLocation();
        } 

        // analyze response  
        if (result.getCode() == ResultCode.UNAUTHORIZED) {
            String authRequest = ((result.getAuthenticateHeader()).trim()).toLowerCase();
            if (authRequest.startsWith("basic")) {
                authMethod = AuthenticationMethod.BASIC_HTTP_AUTH;
                
            } else if (authRequest.startsWith("bearer")) {
                authMethod = AuthenticationMethod.BEARER_TOKEN;
            }
            // else - fall back to UNKNOWN
                    
        } else if (result.isSuccess()) {
            authMethod = AuthenticationMethod.NONE;
            
        } else if (result.isIdPRedirection()) {
            authMethod = AuthenticationMethod.SAML_WEB_SSO;
        }
        // else - fall back to UNKNOWN
        Log_OC.d(TAG, "Authentication method found: " + authenticationMethodToString(authMethod));
        
        if (!authMethod.equals(AuthenticationMethod.UNKNOWN)) {
            result = new RemoteOperationResult(true, result.getHttpCode(), null);
        }
        ArrayList<Object> data = new ArrayList<Object>();
        data.add(authMethod);
        result.setData(data);
        return result;  // same result instance, so that other errors can be handled by the caller transparently
	}
	
	
	private String authenticationMethodToString(AuthenticationMethod value) {
	    switch (value){
	    case NONE:
	        return "NONE";
	    case BASIC_HTTP_AUTH:
	        return "BASIC_HTTP_AUTH";
	    case BEARER_TOKEN:
	        return "BEARER_TOKEN";
	    case SAML_WEB_SSO:
	        return "SAML_WEB_SSO";
	    default:
            return "UNKNOWN";
	    }
    }

}
