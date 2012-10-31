package com.owncloud.android.authenticator.oauth2.connection;

import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import android.util.Log;

/**
 * Implements HTTP POST communications with an oAuth2 server.
 * 
 * @author SolidGear S.L.
 *
 */
public class ConnectorOAuth2 {

    private static final String TAG = "ConnectorOAuth2";
    /** Maximum time to wait for a response from the server when the connection is being tested, in MILLISECONDs.  */
    private static final int TRY_CONNECTION_TIMEOUT = 5000;    

    private DefaultHttpClient httpClient;
    private HttpContext localContext;
    private String ConnectorOAuth2Url;    

    public ConnectorOAuth2 (String destUrl) {
        prepareConn();
        setConnectorOAuth2Url(destUrl);        
    }

    public ConnectorOAuth2 () {
        prepareConn();    
    }      

    public String getConnectorOAuth2Url() {
        return ConnectorOAuth2Url;
    }

    public void setConnectorOAuth2Url(String connectorOAuth2Url) {
        ConnectorOAuth2Url = connectorOAuth2Url;
    }    
    
    /**
     * Starts the communication with the server.
     * 
     * @param UrlEncodedFormEntity : parameters included in the POST call.
     * @return String : data returned from the server in String format.
     */

    public String connPost(UrlEncodedFormEntity  data) {
        String dataOut = null;
        HttpPost httpPost = null;
        int responseCode = -1;
        HttpResponse response = null;

        httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.RFC_2109);

        if (ConnectorOAuth2Url == null) {
            Log.e(TAG, "connPost error: destination URI could not be null");
            return null;
        }

        if (data == null){
            Log.e(TAG, "connPost error: data to send to URI " + ConnectorOAuth2Url + "could not be null");            
            return null; 
        }

        httpPost = new HttpPost(ConnectorOAuth2Url);       

        httpPost.setHeader("Accept","text/html,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
        httpPost.setEntity((UrlEncodedFormEntity) data);

        try {
            response = httpClient.execute(httpPost,localContext);

            if (response == null) {
                Log.e(TAG, "connPost error: response from uri " + ConnectorOAuth2Url + " is null");
                return null;
            } 

            responseCode = response.getStatusLine().getStatusCode();

            if ((responseCode != 200)) {
                Log.e(TAG, "connPost error: response from uri "+ ConnectorOAuth2Url + " returns status " + responseCode);
                return null;
            }

            dataOut = EntityUtils.toString(response.getEntity());

        } catch (Exception e) {
            Log.e(TAG, "connPost Exception: " + e);
        }

        return dataOut;
    }

    private void prepareConn () {
        HttpParams localParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(localParams, TRY_CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(localParams, TRY_CONNECTION_TIMEOUT);
        httpClient = new DefaultHttpClient(localParams);
        localContext = new BasicHttpContext();    
    }
}
