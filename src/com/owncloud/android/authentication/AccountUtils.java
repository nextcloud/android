/* ownCloud Android client application
 *   Copyright (C) 2012  Bartek Przybylski
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

package com.owncloud.android.authentication;

import com.owncloud.android.utils.OwnCloudVersion;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountsException;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class AccountUtils {
    public static final String WEBDAV_PATH_1_2 = "/webdav/owncloud.php";
    public static final String WEBDAV_PATH_2_0 = "/files/webdav.php";
    public static final String WEBDAV_PATH_4_0 = "/remote.php/webdav";
    private static final String ODAV_PATH = "/remote.php/odav";
    private static final String SAML_SSO_PATH = "/remote.php/webdav";
    public static final String CARDDAV_PATH_2_0 = "/apps/contacts/carddav.php";
    public static final String CARDDAV_PATH_4_0 = "/remote/carddav.php";
    public static final String STATUS_PATH = "/status.php";

    /**
     * Can be used to get the currently selected ownCloud {@link Account} in the
     * application preferences.
     * 
     * @param   context     The current application {@link Context}
     * @return              The ownCloud {@link Account} currently saved in preferences, or the first 
     *                      {@link Account} available, if valid (still registered in the system as ownCloud 
     *                      account). If none is available and valid, returns null.
     */
    public static Account getCurrentOwnCloudAccount(Context context) {
        Account[] ocAccounts = AccountManager.get(context).getAccountsByType(
                AccountAuthenticator.ACCOUNT_TYPE);
        Account defaultAccount = null;

        SharedPreferences appPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);
        String accountName = appPreferences
                .getString("select_oc_account", null);

        // account validation: the saved account MUST be in the list of ownCloud Accounts known by the AccountManager
        if (accountName != null) {
            for (Account account : ocAccounts) {
                if (account.name.equals(accountName)) {
                    defaultAccount = account;
                    break;
                }
            }
        }
        
        if (defaultAccount == null && ocAccounts.length != 0) {
            // take first account as fallback
            defaultAccount = ocAccounts[0];
        }

        return defaultAccount;
    }

    
    public static boolean exists(Account account, Context context) {
        Account[] ocAccounts = AccountManager.get(context).getAccountsByType(
                AccountAuthenticator.ACCOUNT_TYPE);

        if (account != null && account.name != null) {
            for (Account ac : ocAccounts) {
                if (ac.name.equals(account.name)) {
                    return true;
                }
            }
        }
        return false;
    }
    

    /**
     * Checks, whether or not there are any ownCloud accounts setup.
     * 
     * @return true, if there is at least one account.
     */
    public static boolean accountsAreSetup(Context context) {
        AccountManager accMan = AccountManager.get(context);
        Account[] accounts = accMan
                .getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);
        return accounts.length > 0;
    }
    
    
    public static boolean setCurrentOwnCloudAccount(Context context, String accountName) {
        boolean result = false;
        if (accountName != null) {
            Account[] ocAccounts = AccountManager.get(context).getAccountsByType(
                    AccountAuthenticator.ACCOUNT_TYPE);
            boolean found = false;
            for (Account account : ocAccounts) {
                found = (account.name.equals(accountName));
                if (found) {
                    SharedPreferences.Editor appPrefs = PreferenceManager
                            .getDefaultSharedPreferences(context).edit();
                    appPrefs.putString("select_oc_account", accountName);
    
                    appPrefs.commit();
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * 
     * @param version version of owncloud
     * @return webdav path for given OC version, null if OC version unknown
     */
    public static String getWebdavPath(OwnCloudVersion version, boolean supportsOAuth, boolean supportsSamlSso) {
        if (version != null) {
            if (supportsOAuth) {
                return ODAV_PATH;
            }
            if (supportsSamlSso) {
                return SAML_SSO_PATH;
            }
            if (version.compareTo(OwnCloudVersion.owncloud_v4) >= 0)
                return WEBDAV_PATH_4_0;
            if (version.compareTo(OwnCloudVersion.owncloud_v3) >= 0
                    || version.compareTo(OwnCloudVersion.owncloud_v2) >= 0)
                return WEBDAV_PATH_2_0;
            if (version.compareTo(OwnCloudVersion.owncloud_v1) >= 0)
                return WEBDAV_PATH_1_2;
        }
        return null;
    }
    
    /**
     * Returns the proper URL path to access the WebDAV interface of an ownCloud server,
     * according to its version and the authorization method used.
     * 
     * @param   version         Version of ownCloud server.
     * @param   authTokenType   Authorization token type, matching some of the AUTH_TOKEN_TYPE_* constants in {@link AccountAuthenticator}. 
     * @return                  WebDAV path for given OC version and authorization method, null if OC version is unknown.
     */
    public static String getWebdavPath(OwnCloudVersion version, String authTokenType) {
        if (version != null) {
            if (AccountAuthenticator.AUTH_TOKEN_TYPE_ACCESS_TOKEN.equals(authTokenType)) {
                return ODAV_PATH;
            }
            if (AccountAuthenticator.AUTH_TOKEN_TYPE_SAML_WEB_SSO_SESSION_COOKIE.equals(authTokenType)) {
                return SAML_SSO_PATH;
            }
            if (version.compareTo(OwnCloudVersion.owncloud_v4) >= 0)
                return WEBDAV_PATH_4_0;
            if (version.compareTo(OwnCloudVersion.owncloud_v3) >= 0
                    || version.compareTo(OwnCloudVersion.owncloud_v2) >= 0)
                return WEBDAV_PATH_2_0;
            if (version.compareTo(OwnCloudVersion.owncloud_v1) >= 0)
                return WEBDAV_PATH_1_2;
        }
        return null;
    }
    
    /**
     * Constructs full url to host and webdav resource basing on host version
     * @param context
     * @param account
     * @return url or null on failure
     * @throws AccountNotFoundException     When 'account' is unknown for the AccountManager
     */
    public static String constructFullURLForAccount(Context context, Account account) throws AccountNotFoundException {
        AccountManager ama = AccountManager.get(context);
        String baseurl = ama.getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL);
        String strver  = ama.getUserData(account, AccountAuthenticator.KEY_OC_VERSION);
        boolean supportsOAuth = (ama.getUserData(account, AccountAuthenticator.KEY_SUPPORTS_OAUTH2) != null);
        boolean supportsSamlSso = (ama.getUserData(account, AccountAuthenticator.KEY_SUPPORTS_SAML_WEB_SSO) != null);
        OwnCloudVersion ver = new OwnCloudVersion(strver);
        String webdavpath = getWebdavPath(ver, supportsOAuth, supportsSamlSso);

        if (baseurl == null || webdavpath == null) 
            throw new AccountNotFoundException(account, "Account not found", null);
        
        return baseurl + webdavpath;
    }
    
    
    public static class AccountNotFoundException extends AccountsException {
        
        /** Generated - should be refreshed every time the class changes!! */
        private static final long serialVersionUID = -9013287181793186830L;
        
        private Account mFailedAccount; 
                
        public AccountNotFoundException(Account failedAccount, String message, Throwable cause) {
            super(message, cause);
            mFailedAccount = failedAccount;
        }
        
        public Account getFailedAccount() {
            return mFailedAccount;
        }
    }

}
