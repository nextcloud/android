package com.owncloud.android.operations;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import com.owncloud.android.authenticator.oauth2.OAuth2Context;
import com.owncloud.android.operations.RemoteOperationResult.ResultCode;

import android.util.Log;

import eu.alefzero.webdav.WebdavClient;

public class OAuth2GetAccessToken extends RemoteOperation {
    
    private static final String TAG = OAuth2GetAccessToken.class.getSimpleName();
    
    private Map<String, String> mOAuth2AuthorizationResponse;
    private Map<String, String> mResultTokenMap;

    
    public OAuth2GetAccessToken(Map<String, String> oAuth2AuthorizationResponse) {
        mOAuth2AuthorizationResponse = oAuth2AuthorizationResponse;
        mResultTokenMap = null;
    }
    
    
    public Map<String, String> getOauth2AutorizationResponse() {
        return mOAuth2AuthorizationResponse;
    }

    public Map<String, String> getResultTokenMap() {
        return mResultTokenMap;
    }
    
    @Override
    protected RemoteOperationResult run(WebdavClient client) {
        RemoteOperationResult result = null;
        PostMethod postMethod = null;
        
        try {
            NameValuePair[] nameValuePairs = new NameValuePair[5];
            nameValuePairs[0] = new NameValuePair(OAuth2Context.KEY_CLIENT_ID, OAuth2Context.OAUTH2_F_CLIENT_ID);
            nameValuePairs[1] = new NameValuePair(OAuth2Context.KEY_CODE, mOAuth2AuthorizationResponse.get(OAuth2Context.KEY_CODE));            
            nameValuePairs[2] = new NameValuePair(OAuth2Context.KEY_SCOPE, mOAuth2AuthorizationResponse.get(OAuth2Context.KEY_SCOPE));            
            nameValuePairs[3] = new NameValuePair(OAuth2Context.KEY_REDIRECT_URI, OAuth2Context.MY_REDIRECT_URI);            
            nameValuePairs[4] = new NameValuePair(OAuth2Context.KEY_GRANT_TYPE, OAuth2Context.OAUTH2_AUTH_CODE_GRANT_TYPE);
            
            postMethod = new PostMethod(client.getBaseUri().toString());
            postMethod.setRequestBody(nameValuePairs);
            int status = client.executeMethod(postMethod);
            if (status >= 300) {
                client.exhaustResponse(postMethod.getResponseBodyAsStream());
                result = new RemoteOperationResult(false, status);
                
            } else {
                JSONObject tokenJson = new JSONObject(postMethod.getResponseBodyAsString());
                parseResult(tokenJson);
                if (mResultTokenMap.get(OAuth2Context.OAUTH2_TOKEN_RECEIVED_ERROR) != null) {
                    result = new RemoteOperationResult(ResultCode.OAUTH2_ERROR);
                    
                } else {
                    result = new RemoteOperationResult(true, status);
                }
            }

        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            
        } finally {
            if (postMethod != null)
                postMethod.releaseConnection();    // let the connection available for other methods
            
            if (result.isSuccess()) {
                Log.i(TAG, "OAuth2 TOKEN REQUEST with code " + mOAuth2AuthorizationResponse.get("code") + " to " + client.getBaseUri() + ": " + result.getLogMessage());
            
            } else if (result.getException() != null) {
                Log.e(TAG, "OAuth2 TOKEN REQUEST with code " + mOAuth2AuthorizationResponse.get("code") + " to " + client.getBaseUri() + ": " + result.getLogMessage(), result.getException());
                
            } else if (result.getCode() == ResultCode.OAUTH2_ERROR) {
                    Log.e(TAG, "OAuth2 TOKEN REQUEST with code " + mOAuth2AuthorizationResponse.get("code") + " to " + client.getBaseUri() + ": " + mResultTokenMap.get(OAuth2Context.OAUTH2_TOKEN_RECEIVED_ERROR));
                    
            } else {
                Log.e(TAG, "OAuth2 TOKEN REQUEST with code " + mOAuth2AuthorizationResponse.get("code") + " to " + client.getBaseUri() + ": " + result.getLogMessage());
            }
        }
        
        return result;
    }
    
    
    private void parseResult (JSONObject tokenJson) throws JSONException {
        mResultTokenMap = new HashMap<String, String>();
        
        if (tokenJson.has(OAuth2Context.KEY_ACCESS_TOKEN)) {
            mResultTokenMap.put(OAuth2Context.KEY_ACCESS_TOKEN, tokenJson.getString(OAuth2Context.KEY_ACCESS_TOKEN));
        }
        if (tokenJson.has(OAuth2Context.KEY_TOKEN_TYPE)) {
            mResultTokenMap.put(OAuth2Context.KEY_TOKEN_TYPE, tokenJson.getString(OAuth2Context.KEY_TOKEN_TYPE));
        }
        if (tokenJson.has(OAuth2Context.KEY_EXPIRES_IN)) {
            mResultTokenMap.put(OAuth2Context.KEY_EXPIRES_IN, tokenJson.getString(OAuth2Context.KEY_EXPIRES_IN));
        }
        if (tokenJson.has(OAuth2Context.KEY_REFRESH_TOKEN)) {
            mResultTokenMap.put(OAuth2Context.KEY_REFRESH_TOKEN, tokenJson.getString(OAuth2Context.KEY_REFRESH_TOKEN));
        }
        if (tokenJson.has(OAuth2Context.KEY_SCOPE)) {
            mResultTokenMap.put(OAuth2Context.KEY_SCOPE, tokenJson.getString(OAuth2Context.KEY_SCOPE));
        }
        if (tokenJson.has(OAuth2Context.KEY_ERROR)) {
            mResultTokenMap.put(OAuth2Context.KEY_ERROR, tokenJson.getString(OAuth2Context.KEY_ERROR));
        }
        if (tokenJson.has(OAuth2Context.KEY_ERROR_DESCRIPTION)) {
            mResultTokenMap.put(OAuth2Context.KEY_ERROR_DESCRIPTION, tokenJson.getString(OAuth2Context.KEY_ERROR_DESCRIPTION));
        }
        if (tokenJson.has(OAuth2Context.KEY_ERROR_URI)) {
            mResultTokenMap.put(OAuth2Context.KEY_ERROR_URI, tokenJson.getString(OAuth2Context.KEY_ERROR_URI));
        }
    }

}
