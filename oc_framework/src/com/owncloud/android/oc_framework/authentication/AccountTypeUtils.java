package com.owncloud.android.oc_framework.authentication;

import android.accounts.Account;

public class AccountTypeUtils {

    // Methods to obtain Strings referring account_type   
    public static String getAccountType(Account account) {
        return account.type;
    }
    
    
    //  public static final String AUTH_TOKEN_TYPE_PASSWORD = "owncloud.password";
    public static String getAuthTokenTypePass(Account account) {
        return account.type + ".password";
    }
    
    //  public static final String AUTH_TOKEN_TYPE_ACCESS_TOKEN = "owncloud.oauth2.access_token";
    public static String getAuthTokenTypeAccessToken(Account account) {
        return account.type + ".oauth2.access_token";
    }
    
    //  public static final String AUTH_TOKEN_TYPE_REFRESH_TOKEN = "owncloud.oauth2.refresh_token";
    public static String getAuthTokenTypeRefreshToken(Account account) {
        return account.type + ".oauth2.refresh_token";
    }
    
    //  public static final String AUTH_TOKEN_TYPE_SAML_WEB_SSO_SESSION_COOKIE = "owncloud.saml.web_sso.session_cookie";
    public static String getAuthTokenTypeSamlSessionCookie(Account account) {
        return account.type +  ".saml.web_sso.session_cookie";
    }
    
}
