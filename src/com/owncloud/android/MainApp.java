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
package com.owncloud.android;

import android.app.Application;
import android.content.Context;
/**
 * Main Application of the project
 * 
 * Contains methods to build the "static" strings. These strings were before constants in different classes
 * 
 * @author masensio
 */
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
    
    //  From ProviderMeta 
    //  public static final String DB_FILE = "owncloud.db";
    public static String getDBFile() {
        return getAppContext().getResources().getString(R.string.db_file);
    }
    
    //  From ProviderMeta
    //  private final String mDatabaseName = "ownCloud";
    public static String getDBName() {
        return getAppContext().getResources().getString(R.string.db_name);
    }
     
    //  data_folder
    public static String getDataFolder() {
        return getAppContext().getResources().getString(R.string.data_folder);
    }
    
    // log_name
    public static String getLogName() {
        return getAppContext().getResources().getString(R.string.log_name);
    }
}
