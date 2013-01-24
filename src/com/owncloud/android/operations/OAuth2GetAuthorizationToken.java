package com.owncloud.android.operations;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.owncloud.android.authenticator.oauth2.OAuth2Context;
import com.owncloud.android.authenticator.oauth2.OnOAuth2GetCodeResultListener;
import com.owncloud.android.authenticator.oauth2.OnOAuth2GetCodeResultListener.ResultOAuthType;
import com.owncloud.android.authenticator.oauth2.connection.ConnectorOAuth2;

/**
 * Implements the communication with oAuth2 server to get User Code and other useful values.
 * 
 * @author SolidGear S.L.
 *
 */
public class OAuth2GetAuthorizationToken implements Runnable {

    public static final String CODE_USER_CODE  =  "user_code";
    public static final String CODE_CLIENT_ID  =  "client_id";
    public static final String CODE_SCOPE  =  "scope";    
    public static final String CODE_VERIFICATION_URL  =  "verification_url";
    public static final String CODE_EXPIRES_IN  =  "expires_in";
    public static final String CODE_DEVICE_CODE = "device_code";
    public static final String CODE_INTERVAL = "interval";

    private static final String CODE_RESPONSE_TYPE = "response_type";
    private static final String CODE_REDIRECT_URI = "redirect_uri";
    
    private String mGrantType = OAuth2Context.OAUTH2_AUTH_CODE_GRANT_TYPE;
    
    private static final String TAG = "OAuth2GetCodeRunnable";
    private OnOAuth2GetCodeResultListener mListener;
    private String mUrl;
    private Handler mHandler;
    private Context mContext;
    //private JSONObject codeResponseJson = null;
    ResultOAuthType mLatestResult;


    public void setListener(OnOAuth2GetCodeResultListener listener, Handler handler) {
        mListener = listener;
        mHandler = handler;
    }

    public OAuth2GetAuthorizationToken(String url, Context context) {
        mListener = null;
        mHandler = null;
        mUrl = url;
        mContext = context;
    }

    @Override
    public void run() {

        if (!isOnline()) {
            postResult(ResultOAuthType.NO_NETWORK_CONNECTION,null);
            return;
        }

        if (mUrl.startsWith("http://") || mUrl.startsWith("https://")) {        
            mLatestResult = (mUrl.startsWith("https://"))? ResultOAuthType.OK_SSL : ResultOAuthType.OK_NO_SSL;            
        } else {
            mUrl = "https://" + mUrl;
            mLatestResult = ResultOAuthType.OK_SSL;
        }

        if (mGrantType.equals(OAuth2Context.OAUTH2_AUTH_CODE_GRANT_TYPE)) {
            requestBrowserToGetAuthorizationCode();
            
        } /*else if (mGrantType.equals(OAuth2Context.OAUTH_G_DEVICE_GETTOKEN_GRANT_TYPE)) {
            getAuthorizationCode();
        }*/
    }

    /// open the authorization endpoint in a web browser!
    private void requestBrowserToGetAuthorizationCode() {
        Uri uri = Uri.parse(mUrl);
        Uri.Builder uriBuilder = uri.buildUpon();
        uriBuilder.appendQueryParameter(CODE_RESPONSE_TYPE, OAuth2Context.OAUTH2_CODE_RESPONSE_TYPE);
        uriBuilder.appendQueryParameter(CODE_REDIRECT_URI, OAuth2Context.MY_REDIRECT_URI);   
        uriBuilder.appendQueryParameter(CODE_CLIENT_ID, OAuth2Context.OAUTH2_F_CLIENT_ID);
        uriBuilder.appendQueryParameter(CODE_SCOPE, OAuth2Context.OAUTH2_F_SCOPE);
        //uriBuilder.appendQueryParameter(CODE_STATE, whateverwewant);
        
        uri = uriBuilder.build();
        Log.d(TAG, "Starting browser to view " + uri.toString());
        
        Intent i = new Intent(Intent.ACTION_VIEW, uri);
        mContext.startActivity(i);
        
        postResult(mLatestResult, null);
    }

    
    /*
    private void getAuthorizationCode() {
        ConnectorOAuth2 connectorOAuth2 = new ConnectorOAuth2(mUrl);
        try {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair(CODE_CLIENT_ID, OAuth2Context.OAUTH2_G_DEVICE_CLIENT_ID));
            nameValuePairs.add(new BasicNameValuePair(CODE_SCOPE,OAuth2Context.OAUTH2_G_DEVICE_GETCODE_SCOPES));
            UrlEncodedFormEntity params = new UrlEncodedFormEntity(nameValuePairs);        
            codeResponseJson = new JSONObject(connectorOAuth2.connPost(params));         
        } catch (JSONException e) {
            Log.e(TAG, "JSONException converting to Json: " + e.toString());
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "UnsupportedEncodingException encoding URL values: " + e.toString());
        } catch (Exception e) {
            Log.e(TAG, "Exception : " + e.toString());
        }

        if (codeResponseJson == null) {            
            mLatestResult = ResultOAuthType.HOST_NOT_AVAILABLE;
        }
        postResult(mLatestResult, codeResponseJson);
    }
    */

    
    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm != null && cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    private void postResult(final ResultOAuthType result,final JSONObject codeResponseJson) {
        if (mHandler != null && mListener != null) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onOAuth2GetCodeResult(result, codeResponseJson);
                }
            });
        }
    }

}