
/* ownCloud Android client application
 *   Copyright (C) 2012-2013 ownCloud Inc.
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
package com.owncloud.android.lib.operations.remote;

import java.io.IOException;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.owncloud.android.lib.network.OwnCloudClient;
import com.owncloud.android.lib.operations.common.RemoteOperation;
import com.owncloud.android.lib.operations.common.RemoteOperationResult;


/**
 * @author masensio
 *
 * Get the UserName for a SAML connection, from a JSON with the format:
 * 		id
 * 		display-name
 * 		email
 */

public class GetUserNameRemoteOperation extends RemoteOperation {
	
	private static final String TAG = GetUserNameRemoteOperation.class.getSimpleName();

	// HEADER
	private static final String HEADER_OCS_API = "OCS-APIREQUEST";
	private static final String HEADER_OCS_API_VALUE = "true";

	// OCS Route
	private static final String OCS_ROUTE ="/index.php/ocs/cloud/user?format=json"; 

	// JSON Node names
	private static final String NODE_OCS = "ocs";
	private static final String NODE_DATA = "data";
	private static final String NODE_ID = "id";
	private static final String NODE_DISPLAY_NAME= "display-name";
	private static final String NODE_EMAIL= "email";

	private String mUserName;

	public String getUserName() {
		return mUserName;
	}

	
	public GetUserNameRemoteOperation() {
	}

	@Override
	protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;
        int status = -1;
        
        // Get Method
        GetMethod get = new GetMethod(client.getBaseUri() + OCS_ROUTE);
        Log.d(TAG, "URL ------> " + client.getBaseUri() + OCS_ROUTE);
        // Add the Header
        get.addRequestHeader(HEADER_OCS_API, HEADER_OCS_API_VALUE);
        
        //Get the user
        try {
			status = client.executeMethod(get);
			if(isSuccess(status)) {
				 Log.d(TAG, "Obtain RESPONSE");
				 String response = get.getResponseBodyAsString();
				 
				 Log.d(TAG, "GET RESPONSE.................... " + response);

				 // Parse the response
				 JSONObject respJSON = new JSONObject(response);
				 JSONObject respOCS = respJSON.getJSONObject(NODE_OCS);
				 JSONObject respData = respOCS.getJSONObject(NODE_DATA);
				 String id = respData.getString(NODE_ID);
				 String displayName = respData.getString(NODE_DISPLAY_NAME);
				 String email = respData.getString(NODE_EMAIL);
				 
				 // Result
				 result = new RemoteOperationResult(isSuccess(status), status, (get != null ? get.getResponseHeaders() : null));
				 mUserName =  displayName;
				 
				 Log.d(TAG, "Response: " + id + " - " + displayName + " - " + email);
				 
			}
		} catch (HttpException e) {
			result = new RemoteOperationResult(e);
			e.printStackTrace();
		} catch (IOException e) {
			result = new RemoteOperationResult(e);
			e.printStackTrace();
		} catch (JSONException e) {
			result = new RemoteOperationResult(e);
			e.printStackTrace();
		} finally {
			get.releaseConnection();
		}
        
		return result;
	}

    private boolean isSuccess(int status) {
        return (status == HttpStatus.SC_OK);
    }
    
}
