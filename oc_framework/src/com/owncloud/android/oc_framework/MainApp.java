package com.owncloud.android.oc_framework;

import android.app.Application;
import android.content.Context;

public class MainApp extends Application {

	private static Context mContext;

    public void onCreate(){
        super.onCreate();
        MainApp.mContext = getApplicationContext();
    }

    public static Context getAppContext() {
        return MainApp.mContext;
    }

    // Methods to obtain Strings referring app_name 
    //   From AccountAuthenticator 
    //   public static final String ACCOUNT_TYPE = "owncloud";    
    public static String getAccountType() {
        return getAppContext().getResources().getString(R.string.account_type);
    }
    
    //  From AccountAuthenticator 
    //  public static final String AUTHORITY = "org.owncloud";
    public static String getAuthority() {
        return getAppContext().getResources().getString(R.string.authority);
    }
    
    //  From AccountAuthenticator
    //  public static final String AUTH_TOKEN_TYPE = "org.owncloud";
    public static String getAuthTokenType() {
        return getAppContext().getResources().getString(R.string.authority);
    }
    
    //  From AccountAuthenticator
    //  public static final String AUTH_TOKEN_TYPE_PASSWORD = "owncloud.password";
    public static String getAuthTokenTypePass() {
        return getAppContext().getResources().getString(R.string.account_type) + ".password";
    }
    
    //  From AccountAuthenticator
    //  public static final String AUTH_TOKEN_TYPE_ACCESS_TOKEN = "owncloud.oauth2.access_token";
    public static String getAuthTokenTypeAccessToken() {
        return getAppContext().getResources().getString(R.string.account_type) + ".oauth2.access_token";
    }
    
    //  From AccountAuthenticator
    //  public static final String AUTH_TOKEN_TYPE_REFRESH_TOKEN = "owncloud.oauth2.refresh_token";
    public static String getAuthTokenTypeRefreshToken() {
        return getAppContext().getResources().getString(R.string.account_type) + ".oauth2.refresh_token";
    }
    
    //  From AccountAuthenticator
    //  public static final String AUTH_TOKEN_TYPE_SAML_WEB_SSO_SESSION_COOKIE = "owncloud.saml.web_sso.session_cookie";
    public static String getAuthTokenTypeSamlSessionCookie() {
        return getAppContext().getResources().getString(R.string.account_type) +  ".saml.web_sso.session_cookie";
    }
    
}
