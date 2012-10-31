package com.owncloud.android.authenticator.oauth2.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.owncloud.android.authenticator.oauth2.OAuth2Context;
import com.owncloud.android.authenticator.oauth2.connection.ConnectorOAuth2;

/**
 * Service class that implements the second communication with the oAuth2 server:
 * pooling for the token in an interval. It send a broadcast with the results when are positive;
 * otherwise, it continues asking to the server.
 * 
 * @author Solid Gear S.L.
 *
 */
public class OAuth2GetTokenService extends Service {

    public static final String TOKEN_RECEIVED_MESSAGE = "TOKEN_RECEIVED";
    public static final String TOKEN_RECEIVED_DATA = "TOKEN_DATA";
    public static final String TOKEN_BASE_URI = "baseURI";
    public static final String TOKEN_DEVICE_CODE = "device_code";
    public static final String TOKEN_INTERVAL = "interval";
    public static final String TOKEN_RECEIVED_ERROR = "error";
    public static final String TOKEN_RECEIVED_ERROR_AUTH_TOKEN = "authorization_pending";
    public static final String TOKEN_RECEIVED_ERROR_SLOW_DOWN = "slow_down";
    public static final String TOKEN_ACCESS_TOKEN = "access_token";
    public static final String TOKEN_TOKEN_TYPE = "token_type";
    public static final String TOKEN_EXPIRES_IN = "expires_in";
    public static final String TOKEN_REFRESH_TOKEN = "refresh_token";   
    
    private String requestDeviceCode;
    private int requestInterval = -1;
    private String requestBaseURI;
    private ConnectorOAuth2 connectorOAuth2;
    private static final String TAG = "OAuth2GetTokenService";
    private Timer timer = new Timer();

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Bundle param = intent.getExtras();

        if (param != null) {
            String mUrl = param.getString(TOKEN_BASE_URI);     
            if (!mUrl.startsWith("http://") || !mUrl.startsWith("https://")) {        
                requestBaseURI = "https://" + mUrl;            
            }     
            requestDeviceCode = param.getString(TOKEN_DEVICE_CODE);
            requestInterval = param.getInt(TOKEN_INTERVAL);
            
            Log.d(TAG, "onBind -> baseURI=" + requestBaseURI);
            Log.d(TAG, "onBind -> requestDeviceCode=" + requestDeviceCode);
            Log.d(TAG, "onBind -> requestInterval=" + requestInterval);                  
        } else  {
            Log.e(TAG, "onBind -> params could not be null");
        }
        startService();
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();       
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        shutdownService();
    }

    private void startService() {
        final UrlEncodedFormEntity params = prepareComm();
        timer.scheduleAtFixedRate(
                new TimerTask() {
                    public void run() {
                        requestToken(params);
                    }
                },  0, requestInterval * 1000);
        Log.d(TAG, "startService -> Timer started");
    }    

    private void shutdownService() {
        if (timer != null) timer.cancel();
        Log.d(TAG, "shutdownService -> Timer stopped");
    }


    private UrlEncodedFormEntity prepareComm() {

        UrlEncodedFormEntity params = null;
        connectorOAuth2 = new ConnectorOAuth2();

        if (requestBaseURI == null || requestBaseURI.trim().equals("")) {
            Log.e(TAG, "run -> request URI could not be null");
            postResult(null);
        }

        if (requestInterval == -1) {
            Log.e(TAG, "run -> request Interval must have valid positive value");
            postResult(null);
        }

        if (requestDeviceCode == null || requestDeviceCode.trim().equals("")) {
            Log.e(TAG, "run -> request DeviceCode could not be null");
            postResult(null);
        }        

        try{            
            connectorOAuth2.setConnectorOAuth2Url(requestBaseURI + OAuth2Context.OAUTH2_DEVICE_GETTOKEN_URL);

            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("client_id", OAuth2Context.OAUTH2_DEVICE_CLIENT_ID));
            nameValuePairs.add(new BasicNameValuePair("client_secret", OAuth2Context.OAUTH2_DEVICE_CLIENT_SECRET));
            nameValuePairs.add(new BasicNameValuePair("code",requestDeviceCode));            
            nameValuePairs.add(new BasicNameValuePair("grant_type",OAuth2Context.OAUTH_DEVICE_GETTOKEN_GRANT_TYPE));  

            params = new UrlEncodedFormEntity(nameValuePairs);
        }
        catch (Exception ex1){
            Log.w(TAG, ex1.toString());
            postResult(null);
        }

        return params;
    }

    protected void requestToken(UrlEncodedFormEntity params){
        JSONObject tokenJson = null;
        String error = null;
        HashMap<String, String> resultTokenMap;

        String tokenResponse = connectorOAuth2.connPost(params);

        try {
            tokenJson = new JSONObject(tokenResponse);
        } catch (JSONException e) {
            Log.e(TAG, "Exception converting to Json " + e.toString());
        }        

        try {
            // We try to get error string.
            if (tokenJson.has(TOKEN_RECEIVED_ERROR)) {
                error = tokenJson.getString(TOKEN_RECEIVED_ERROR);
                Log.d(TAG, "requestToken -> Obtained error "+ error);
            } else {
                //We have got the token. Parse the answer.
                resultTokenMap = parseResult(tokenJson);
                postResult(resultTokenMap);                
            }
        } catch (JSONException e) {
            Log.e(TAG, "Exception converting to Json " + e.toString());
        }
    }
    
    private HashMap<String, String> parseResult (JSONObject tokenJson) {
        HashMap<String, String> resultTokenMap=new HashMap<String, String>();
        
        try {
            resultTokenMap.put(TOKEN_ACCESS_TOKEN, tokenJson.getString(TOKEN_ACCESS_TOKEN)); 
            resultTokenMap.put(TOKEN_TOKEN_TYPE, tokenJson.getString(TOKEN_TOKEN_TYPE)); 
            resultTokenMap.put(TOKEN_EXPIRES_IN, tokenJson.getString(TOKEN_EXPIRES_IN)); 
            resultTokenMap.put(TOKEN_REFRESH_TOKEN, tokenJson.getString(TOKEN_REFRESH_TOKEN));             
        } catch (JSONException e) {
            Log.e(TAG, "parseResult: Exception converting to Json " + e.toString());
        }    
        return resultTokenMap;      
    }

    /**
     * Returns obtained values with a broadcast.
     * 
     * @param tokenResponse : obtained values.
     */
    private void postResult(HashMap<String, String> tokenResponse) {
        Intent intent = new Intent(TOKEN_RECEIVED_MESSAGE);       
        intent.putExtra(TOKEN_RECEIVED_DATA,tokenResponse);
        sendBroadcast(intent);
        shutdownService();
    }
}
