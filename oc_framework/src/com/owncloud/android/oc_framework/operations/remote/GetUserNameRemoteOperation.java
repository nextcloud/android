
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
package com.owncloud.android.oc_framework.operations.remote;

import java.io.IOException;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.owncloud.android.oc_framework.network.webdav.WebdavClient;
import com.owncloud.android.oc_framework.operations.RemoteOperation;
import com.owncloud.android.oc_framework.operations.RemoteOperationResult;


/**
 * 
 * @author masensio
 *
 * Get the UserName for a SAML connection, from a JSON with the format:
 * 		id
 * 		display_name
 * 		email
 */

public class GetUserNameRemoteOperation extends RemoteOperation {
	
	private static final String TAG = GetUserNameRemoteOperation.class.getSimpleName();

	// HEADER
	private static final String TAG_HEADER = "OCS-APIREQUEST";
	private static final String TAG_HEADER_VALUE = "true";

	// OCS Route
	private static final String TAG_OCS_ROUTE = "index.php/ocs/cloud/user?format=json";

	// JSON Node names
	private static final String TAG_ID = "id";
	private static final String TAG_DISPLAY_NAME= "display_name";
	private static final String TAG_EMAIL= "email";

	private String mUserName;


	public String getUserName() {
		return mUserName;
	}

	private String mUrl;
	
	public GetUserNameRemoteOperation(String url) {
		mUrl = url;
	}

	@Override
	protected RemoteOperationResult run(WebdavClient client) {
        RemoteOperationResult result = null;
        int status = -1;
        
        // Get Method
        GetMethod get = new GetMethod(mUrl);
        // Add the Header
        get.addRequestHeader("application/xml", "Content-Type");
        get.addRequestHeader(TAG_HEADER, TAG_HEADER_VALUE);
        
        //Get the user
        try {
			status = client.executeMethod(get);
			 if(isSuccess(status)) {
				 Log.d(TAG, "Obtain RESPONSE");
				 String response = get.getResponseBodyAsString();
				 
				 Log.d(TAG, response);

				 // Parse the response
				 JSONObject respJSON = new JSONObject(response);
				 String id = respJSON.getString(TAG_ID);
				 String display_name = respJSON.getString(TAG_DISPLAY_NAME);
				 String email = respJSON.getString(TAG_EMAIL);

				 Log.d(TAG, "Response: " + id + "-" + display_name + "-" + email);
				 
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
