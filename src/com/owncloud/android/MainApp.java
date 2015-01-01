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

import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory.Policy;
import com.owncloud.android.lib.common.utils.Log_OC;
/**
 * Main Application of the project
 * 
 * Contains methods to build the "static" strings. These strings were before constants in different
 * classes
 * 
 * @author masensio
 * @author David A. Velasco
 */
public class MainApp extends Application {
    
    private static final String AUTH_ON = "on";
    
    @SuppressWarnings("unused")
    private static final String POLICY_SINGLE_SESSION_PER_ACCOUNT = "single session per account";
    @SuppressWarnings("unused")
    private static final String POLICY_ALWAYS_NEW_CLIENT = "always new client";

    private static Context mContext;
    
    public void onCreate(){
        super.onCreate();
        MainApp.mContext = getApplicationContext();
        
        boolean isSamlAuth = AUTH_ON.equals(getString(R.string.auth_method_saml_web_sso));
        
        if (isSamlAuth) {   
            OwnCloudClientManagerFactory.setDefaultPolicy(Policy.SINGLE_SESSION_PER_ACCOUNT);
            
        } else {
            OwnCloudClientManagerFactory.setDefaultPolicy(Policy.ALWAYS_NEW_CLIENT);
        }

        // initialise thumbnails cache on background thread
        new ThumbnailsCacheManager.InitDiskCacheTask().execute();
        
        if (BuildConfig.DEBUG) {

            String dataFolder = getDataFolder();

            // Set folder for store logs
            Log_OC.setLogDataFolder(dataFolder);

            Log_OC.startLogging();
            Log_OC.d("Debug", "start logging");
        }
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
