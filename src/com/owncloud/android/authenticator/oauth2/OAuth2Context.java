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
    
    public static final String OAUTH2_G_DEVICE_CLIENT_ID = "1044165972576.apps.googleusercontent.com";  
    public static final String OAUTH2_G_DEVICE_CLIENT_SECRET = "rwrA86fnIRCC3bZm0tWnKOkV";
    public static final String OAUTH_G_DEVICE_GETTOKEN_GRANT_TYPE = "http://oauth.net/grant_type/device/1.0";
    public static final String OAUTH2_G_DEVICE_GETCODE_URL = "https://accounts.google.com/o/oauth2/device/code";  
    public static final String OAUTH2_G_DEVICE_GETTOKEN_URL = "https://accounts.google.com/o/oauth2/token";
    public static final String OAUTH2_G_DEVICE_GETCODE_SCOPES = "https://www.googleapis.com/auth/userinfo.email";
    
    //public static final String OAUTH2_F_AUTHORIZATION_ENDPOINT_URL = "https://frko.surfnetlabs.nl/workshop/php-oauth/authorize.php";
    //public static final String OAUTH2_F_TOKEN_ENDPOINT_URL = "https://frko.surfnetlabs.nl/workshop/php-oauth/token.php";
    public static final String OAUTH2_F_CLIENT_ID = "oc-android-test";
    public static final String OAUTH2_F_SCOPE = "grades";
    
    public static final String OAUTH2_AUTH_CODE_GRANT_TYPE = "authorization_code";
    public static final String OAUTH2_CODE_RESPONSE_TYPE = "code";

    public static final String OAUTH2_TOKEN_RECEIVED_ERROR = "error";

    public static final String MY_REDIRECT_URI = "oauth-mobile-app://callback";   // THIS CAN'T BE READ DYNAMICALLY; MUST BE DEFINED IN INSTALLATION TIME
    
    public static final String KEY_ACCESS_TOKEN = "access_token";
    public static final String KEY_TOKEN_TYPE = "token_type";
    public static final String KEY_EXPIRES_IN = "expires_in";
    public static final String KEY_REFRESH_TOKEN = "refresh_token";
    public static final String KEY_SCOPE = "scope";
    public static final String KEY_ERROR = "error";
    public static final String KEY_ERROR_DESCRIPTION = "error_description";
    public static final String KEY_ERROR_URI = "error_uri";
    public static final String KEY_REDIRECT_URI = "redirect_uri";
    public static final String KEY_GRANT_TYPE = "grant_type";
    public static final String KEY_CODE = "code";
    public static final String KEY_CLIENT_ID = "client_id";
    
    public static final String CODE_USER_CODE  =  "user_code";
    public static final String CODE_CLIENT_ID  =  "client_id";
    public static final String CODE_SCOPE  =  "scope";    
    public static final String CODE_VERIFICATION_URL  =  "verification_url";
    public static final String CODE_EXPIRES_IN  =  "expires_in";
    public static final String CODE_DEVICE_CODE = "device_code";
    public static final String CODE_INTERVAL = "interval";
    public static final String CODE_RESPONSE_TYPE = "response_type";
    public static final String CODE_REDIRECT_URI = "redirect_uri";
    
    public static final String ERROR_ACCESS_DENIED = "access_denied";
    
}
