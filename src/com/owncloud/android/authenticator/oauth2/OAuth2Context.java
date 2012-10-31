package com.owncloud.android.authenticator.oauth2;

/** 
 *  Class used to store data from the app registration in oAuth2 server.
 *  THIS VALUES ARE ORIENTATIVE.
 *  MUST BE CHANGED WITH THE CORRECT ONES.
 *  
 * @author SolidGear S.L.
 *
 */

public class OAuth2Context {
    
    public static final String OAUTH2_DEVICE_CLIENT_ID = "0000000000000.apps.googleusercontent.com";  
    public static final String OAUTH2_DEVICE_CLIENT_SECRET = "XXXXXXXXXXXXXXXXXXXXXXXXX";
    public static final String OAUTH_DEVICE_GETTOKEN_GRANT_TYPE = "http://oauth.net/grant_type/device/1.0";
    public static final String OAUTH2_DEVICE_GETCODE_URL = "/o/oauth2/device/code";  
    public static final String OAUTH2_DEVICE_GETTOKEN_URL = "/o/oauth2/token";
    public static final String OAUTH2_DEVICE_GETCODE_SCOPES = "https://www.googleapis.com/auth/userinfo.email";
}
