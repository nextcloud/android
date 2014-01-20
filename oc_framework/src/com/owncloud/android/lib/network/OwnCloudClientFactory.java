/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2014 ownCloud (http://www.owncloud.org/)
 *   
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *   
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *   
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS 
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN 
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.network;

import java.io.IOException;
import java.security.GeneralSecurityException;

import com.owncloud.android.lib.accounts.AccountTypeUtils;
import com.owncloud.android.lib.accounts.AccountUtils;
import com.owncloud.android.lib.accounts.OwnCloudAccount;
import com.owncloud.android.lib.accounts.AccountUtils.AccountNotFoundException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class OwnCloudClientFactory {
    
    final private static String TAG = OwnCloudClientFactory.class.getSimpleName();
    
    /** Default timeout for waiting data from the server */
    public static final int DEFAULT_DATA_TIMEOUT = 60000;
    
    /** Default timeout for establishing a connection */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 60000;

    
    /**
     * Creates a OwnCloudClient setup for an ownCloud account
     * 
     * Do not call this method from the main thread.
     * 
     * @param account                       The ownCloud account
     * @param appContext                    Android application context
     * @return                              A OwnCloudClient object ready to be used
     * @throws AuthenticatorException       If the authenticator failed to get the authorization token for the account.
     * @throws OperationCanceledException   If the authenticator operation was cancelled while getting the authorization token for the account. 
     * @throws IOException                  If there was some I/O error while getting the authorization token for the account.
     * @throws AccountNotFoundException     If 'account' is unknown for the AccountManager
     */
    public static OwnCloudClient createOwnCloudClient (Account account, Context appContext) throws OperationCanceledException, AuthenticatorException, IOException, AccountNotFoundException {
        //Log_OC.d(TAG, "Creating OwnCloudClient associated to " + account.name);
       
        Uri uri = Uri.parse(AccountUtils.constructFullURLForAccount(appContext, account));
        AccountManager am = AccountManager.get(appContext);
        boolean isOauth2 = am.getUserData(account, OwnCloudAccount.Constants.KEY_SUPPORTS_OAUTH2) != null;   // TODO avoid calling to getUserData here
        boolean isSamlSso = am.getUserData(account, OwnCloudAccount.Constants.KEY_SUPPORTS_SAML_WEB_SSO) != null;
        OwnCloudClient client = createOwnCloudClient(uri, appContext, !isSamlSso);
        if (isOauth2) {    
            String accessToken = am.blockingGetAuthToken(account, AccountTypeUtils.getAuthTokenTypeAccessToken(account.type), false);
            client.setBearerCredentials(accessToken);   // TODO not assume that the access token is a bearer token
        
        } else if (isSamlSso) {    // TODO avoid a call to getUserData here
            String accessToken = am.blockingGetAuthToken(account, AccountTypeUtils.getAuthTokenTypeSamlSessionCookie(account.type), false);
            client.setSsoSessionCookie(accessToken);
            
        } else {
            String username = account.name.substring(0, account.name.lastIndexOf('@'));
            //String password = am.getPassword(account);
            String password = am.blockingGetAuthToken(account, AccountTypeUtils.getAuthTokenTypePass(account.type), false);
            client.setBasicCredentials(username, password);
        }
        
        return client;
    }
    
    
    public static OwnCloudClient createOwnCloudClient (Account account, Context appContext, Activity currentActivity) throws OperationCanceledException, AuthenticatorException, IOException, AccountNotFoundException {
        Uri uri = Uri.parse(AccountUtils.constructFullURLForAccount(appContext, account));
        AccountManager am = AccountManager.get(appContext);
        boolean isOauth2 = am.getUserData(account, OwnCloudAccount.Constants.KEY_SUPPORTS_OAUTH2) != null;   // TODO avoid calling to getUserData here
        boolean isSamlSso = am.getUserData(account, OwnCloudAccount.Constants.KEY_SUPPORTS_SAML_WEB_SSO) != null;
        OwnCloudClient client = createOwnCloudClient(uri, appContext, !isSamlSso);
        
        if (isOauth2) {    // TODO avoid a call to getUserData here
            AccountManagerFuture<Bundle> future =  am.getAuthToken(account,  AccountTypeUtils.getAuthTokenTypeAccessToken(account.type), null, currentActivity, null, null);
            Bundle result = future.getResult();
            String accessToken = result.getString(AccountManager.KEY_AUTHTOKEN);
            if (accessToken == null) throw new AuthenticatorException("WTF!");
            client.setBearerCredentials(accessToken);   // TODO not assume that the access token is a bearer token

        } else if (isSamlSso) {    // TODO avoid a call to getUserData here
            AccountManagerFuture<Bundle> future =  am.getAuthToken(account, AccountTypeUtils.getAuthTokenTypeSamlSessionCookie(account.type), null, currentActivity, null, null);
            Bundle result = future.getResult();
            String accessToken = result.getString(AccountManager.KEY_AUTHTOKEN);
            if (accessToken == null) throw new AuthenticatorException("WTF!");
            client.setSsoSessionCookie(accessToken);

        } else {
            String username = account.name.substring(0, account.name.lastIndexOf('@'));
            //String password = am.getPassword(account);
            //String password = am.blockingGetAuthToken(account, MainApp.getAuthTokenTypePass(), false);
            AccountManagerFuture<Bundle> future =  am.getAuthToken(account,  AccountTypeUtils.getAuthTokenTypePass(account.type), null, currentActivity, null, null);
            Bundle result = future.getResult();
            String password = result.getString(AccountManager.KEY_AUTHTOKEN);
            client.setBasicCredentials(username, password);
        }
        
        return client;
    }
    
    /**
     * Creates a OwnCloudClient to access a URL and sets the desired parameters for ownCloud client connections.
     * 
     * @param uri       URL to the ownCloud server
     * @param context   Android context where the OwnCloudClient is being created.
     * @return          A OwnCloudClient object ready to be used
     */
    public static OwnCloudClient createOwnCloudClient(Uri uri, Context context, boolean followRedirects) {
        try {
            NetworkUtils.registerAdvancedSslContext(true, context);
        }  catch (GeneralSecurityException e) {
            Log.e(TAG, "Advanced SSL Context could not be loaded. Default SSL management in the system will be used for HTTPS connections", e);
            
        } catch (IOException e) {
            Log.e(TAG, "The local server truststore could not be read. Default SSL management in the system will be used for HTTPS connections", e);
        }
        
        OwnCloudClient client = new OwnCloudClient(NetworkUtils.getMultiThreadedConnManager());
        
        client.setDefaultTimeouts(DEFAULT_DATA_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT);
        client.setBaseUri(uri);
        client.setFollowRedirects(followRedirects);
        
        return client;
    }
    
    
}
