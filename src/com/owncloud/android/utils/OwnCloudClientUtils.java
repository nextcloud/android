/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
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
package com.owncloud.android.utils;

import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.protocol.Protocol;

import com.owncloud.android.AccountUtils;
import com.owncloud.android.authenticator.AccountAuthenticator;
import com.owncloud.android.authenticator.EasySSLSocketFactory;
import com.owncloud.android.utils.OwnCloudVersion;

import eu.alefzero.webdav.WebdavClient;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

public class OwnCloudClientUtils {
    
    final private static String TAG = "OwnCloudClientFactory";
    
    /** Default timeout for waiting data from the server */
    public static final int DEFAULT_DATA_TIMEOUT = 60000;
    
    /** Default timeout for establishing a connection */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 60000;

    /** Connection manager for all the WebdavClients */
    static private MultiThreadedHttpConnectionManager mConnManager = null;
    
    
    /**
     * Creates a WebdavClient setup for an ownCloud account
     * 
     * @param account   The ownCloud account
     * @param context   The application context
     * @return          A WebdavClient object ready to be used
     */
    public static WebdavClient createOwnCloudClient (Account account, Context context) {
        Log.d(TAG, "Creating WebdavClient associated to " + account.name);
       
        String baseUrl = AccountManager.get(context).getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL);
        OwnCloudVersion ownCloudVersion = new OwnCloudVersion(AccountManager.get(context).getUserData(account, AccountAuthenticator.KEY_OC_VERSION));
        String webDavPath = AccountUtils.getWebdavPath(ownCloudVersion);
        
        WebdavClient client = createOwnCloudClient(Uri.parse(baseUrl + webDavPath));
        
        String username = account.name.substring(0, account.name.lastIndexOf('@'));
        String password = AccountManager.get(context).getPassword(account);
        //String password = am.blockingGetAuthToken(mAccount, AccountAuthenticator.AUTH_TOKEN_TYPE, true);
        
        client.setCredentials(username, password);
        
        return client;
    }
    
    
    /**
     * Creates a WebdavClient to try a new account before saving it
     * 
     * @param uri       URL to the ownCloud server
     * @param username  User name
     * @param password  User password
     * @return          A WebdavClient object ready to be used
     */
    public static WebdavClient createOwnCloudClient(Uri uri, String username, String password) {
        Log.d(TAG, "Creating WebdavClient for " + username + "@" + uri);
        
        WebdavClient client = createOwnCloudClient(uri);
        
        client.setCredentials(username, password);
        
        return client;
    }
    
    
    /**
     * Creates a WebdavClient to access a URL and sets the desired parameters for ownCloud client connections.
     * 
     * @param uri       URL to the ownCloud server
     * @return          A WebdavClient object ready to be used
     */
    public static WebdavClient createOwnCloudClient(Uri uri) {
        Log.d(TAG, "Creating WebdavClient for " + uri);
        
        allowSelfsignedCertificates(true);
        
        WebdavClient client = new WebdavClient(getMultiThreadedConnManager());
        
        allowSelfsignedCertificates(true);
        client.setDefaultTimeouts(DEFAULT_DATA_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT);
        client.setBaseUri(uri);
        
        return client;
    }
    
    
    /**
     * Allows or disallows self-signed certificates in ownCloud servers to reach
     * 
     * @param allow     'True' to allow, 'false' to disallow
     */
    public static void allowSelfsignedCertificates(boolean allow) {
        Protocol pr = null;
        try {
            pr = Protocol.getProtocol("https");
        } catch (IllegalStateException e) {
            // nothing to do here; really
        }
        boolean isAllowed = (pr != null && pr.getSocketFactory() instanceof EasySSLSocketFactory);
        if (allow && !isAllowed) {
            Protocol.registerProtocol("https", new Protocol("https", new EasySSLSocketFactory(), 443));
        } else if (!allow && isAllowed) {
            // TODO - a more strict SocketFactory object should be provided here
        }
    }

    
    
    static private MultiThreadedHttpConnectionManager getMultiThreadedConnManager() {
        if (mConnManager == null) {
            mConnManager = new MultiThreadedHttpConnectionManager();
            mConnManager.getParams().setDefaultMaxConnectionsPerHost(5);
            mConnManager.getParams().setMaxTotalConnections(5);
        }
        return mConnManager;
    }

}
