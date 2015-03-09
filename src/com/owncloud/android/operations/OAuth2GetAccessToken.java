/**
 *   ownCloud Android client application
 *
 *   Copyright (C) 2015 ownCloud Inc.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import com.owncloud.android.authentication.OAuth2Constants;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;


public class OAuth2GetAccessToken extends RemoteOperation {
    
    private static final String TAG = OAuth2GetAccessToken.class.getSimpleName();
    
    private String mClientId;
    private String mRedirectUri;
    private String mGrantType;
    
    private String mOAuth2AuthorizationResponse;
    private Map<String, String> mOAuth2ParsedAuthorizationResponse;
    private Map<String, String> mResultTokenMap;

    
    public OAuth2GetAccessToken(String clientId, String redirectUri, String grantType, String oAuth2AuthorizationResponse) {
        mClientId = clientId;
        mRedirectUri = redirectUri;
        mGrantType = grantType;
        mOAuth2AuthorizationResponse = oAuth2AuthorizationResponse;
        mOAuth2ParsedAuthorizationResponse = new HashMap<String, String>();
        mResultTokenMap = null;
    }

    /*
    public Map<String, String> getResultTokenMap() {
        return mResultTokenMap;
    }
    */
    
    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;
        PostMethod postMethod = null;
        
        try {
            parseAuthorizationResponse();
            if (mOAuth2ParsedAuthorizationResponse.keySet().contains(OAuth2Constants.KEY_ERROR)) {
                if (OAuth2Constants.VALUE_ERROR_ACCESS_DENIED.equals(mOAuth2ParsedAuthorizationResponse.get(OAuth2Constants.KEY_ERROR))) {
                    result = new RemoteOperationResult(ResultCode.OAUTH2_ERROR_ACCESS_DENIED);
                } else {
                    result = new RemoteOperationResult(ResultCode.OAUTH2_ERROR);
                }
            }
            
            if (result == null) { 
                NameValuePair[] nameValuePairs = new NameValuePair[4];
                nameValuePairs[0] = new NameValuePair(OAuth2Constants.KEY_GRANT_TYPE, mGrantType);
                nameValuePairs[1] = new NameValuePair(OAuth2Constants.KEY_CODE, mOAuth2ParsedAuthorizationResponse.get(OAuth2Constants.KEY_CODE));            
                nameValuePairs[2] = new NameValuePair(OAuth2Constants.KEY_REDIRECT_URI, mRedirectUri);       
                nameValuePairs[3] = new NameValuePair(OAuth2Constants.KEY_CLIENT_ID, mClientId);
                //nameValuePairs[4] = new NameValuePair(OAuth2Constants.KEY_SCOPE, mOAuth2ParsedAuthorizationResponse.get(OAuth2Constants.KEY_SCOPE));         
                
                postMethod = new PostMethod(client.getWebdavUri().toString());
                postMethod.setRequestBody(nameValuePairs);
                int status = client.executeMethod(postMethod);
                
                String response = postMethod.getResponseBodyAsString();
                if (response != null && response.length() > 0) {
                    JSONObject tokenJson = new JSONObject(response);
                    parseAccessTokenResult(tokenJson);
                    if (mResultTokenMap.get(OAuth2Constants.KEY_ERROR) != null || mResultTokenMap.get(OAuth2Constants.KEY_ACCESS_TOKEN) == null) {
                        result = new RemoteOperationResult(ResultCode.OAUTH2_ERROR);
                    
                    } else {
                        result = new RemoteOperationResult(true, status, postMethod.getResponseHeaders());
                        ArrayList<Object> data = new ArrayList<Object>();
                        data.add(mResultTokenMap);
                        result.setData(data);
                    }
                    
                } else {
                    client.exhaustResponse(postMethod.getResponseBodyAsStream());
                    result = new RemoteOperationResult(false, status, postMethod.getResponseHeaders());
                }
            }
            
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            
        } finally {
            if (postMethod != null)
                postMethod.releaseConnection();    // let the connection available for other methods
            
            if (result.isSuccess()) {
                Log_OC.i(TAG, "OAuth2 TOKEN REQUEST with auth code " + mOAuth2ParsedAuthorizationResponse.get("code") + " to " + client.getWebdavUri() + ": " + result.getLogMessage());
            
            } else if (result.getException() != null) {
                Log_OC.e(TAG, "OAuth2 TOKEN REQUEST with auth code " + mOAuth2ParsedAuthorizationResponse.get("code") + " to " + client.getWebdavUri() + ": " + result.getLogMessage(), result.getException());
                
            } else if (result.getCode() == ResultCode.OAUTH2_ERROR) {
                Log_OC.e(TAG, "OAuth2 TOKEN REQUEST with auth code " + mOAuth2ParsedAuthorizationResponse.get("code") + " to " + client.getWebdavUri() + ": " + ((mResultTokenMap != null) ? mResultTokenMap.get(OAuth2Constants.KEY_ERROR) : "NULL"));
                    
            } else {
                Log_OC.e(TAG, "OAuth2 TOKEN REQUEST with auth code " + mOAuth2ParsedAuthorizationResponse.get("code") + " to " + client.getWebdavUri() + ": " + result.getLogMessage());
            }
        }
        
        return result;
    }
    
    
    private void parseAuthorizationResponse() {
        String[] pairs = mOAuth2AuthorizationResponse.split("&");
        int i = 0;
        String key = "";
        String value = "";
        StringBuilder sb = new StringBuilder();
        while (pairs.length > i) {
            int j = 0;
            String[] part = pairs[i].split("=");
            while (part.length > j) {
                String p = part[j];
                if (j == 0) {
                    key = p;
                    sb.append(key + " = ");
                } else if (j == 1) {
                    value = p;
                    mOAuth2ParsedAuthorizationResponse.put(key, value);
                    sb.append(value + "\n");
                }

                Log_OC.v(TAG, "[" + i + "," + j + "] = " + p);
                j++;
            }
            i++;
        }
    }


    private void parseAccessTokenResult (JSONObject tokenJson) throws JSONException {
        mResultTokenMap = new HashMap<String, String>();
        
        if (tokenJson.has(OAuth2Constants.KEY_ACCESS_TOKEN)) {
            mResultTokenMap.put(OAuth2Constants.KEY_ACCESS_TOKEN, tokenJson.getString(OAuth2Constants.KEY_ACCESS_TOKEN));
        }
        if (tokenJson.has(OAuth2Constants.KEY_TOKEN_TYPE)) {
            mResultTokenMap.put(OAuth2Constants.KEY_TOKEN_TYPE, tokenJson.getString(OAuth2Constants.KEY_TOKEN_TYPE));
        }
        if (tokenJson.has(OAuth2Constants.KEY_EXPIRES_IN)) {
            mResultTokenMap.put(OAuth2Constants.KEY_EXPIRES_IN, tokenJson.getString(OAuth2Constants.KEY_EXPIRES_IN));
        }
        if (tokenJson.has(OAuth2Constants.KEY_REFRESH_TOKEN)) {
            mResultTokenMap.put(OAuth2Constants.KEY_REFRESH_TOKEN, tokenJson.getString(OAuth2Constants.KEY_REFRESH_TOKEN));
        }
        if (tokenJson.has(OAuth2Constants.KEY_SCOPE)) {
            mResultTokenMap.put(OAuth2Constants.KEY_SCOPE, tokenJson.getString(OAuth2Constants.KEY_SCOPE));
        }
        if (tokenJson.has(OAuth2Constants.KEY_ERROR)) {
            mResultTokenMap.put(OAuth2Constants.KEY_ERROR, tokenJson.getString(OAuth2Constants.KEY_ERROR));
        }
        if (tokenJson.has(OAuth2Constants.KEY_ERROR_DESCRIPTION)) {
            mResultTokenMap.put(OAuth2Constants.KEY_ERROR_DESCRIPTION, tokenJson.getString(OAuth2Constants.KEY_ERROR_DESCRIPTION));
        }
        if (tokenJson.has(OAuth2Constants.KEY_ERROR_URI)) {
            mResultTokenMap.put(OAuth2Constants.KEY_ERROR_URI, tokenJson.getString(OAuth2Constants.KEY_ERROR_URI));
        }
    }

}
