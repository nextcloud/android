/* ownCloud Android client application
 *   Copyright (C) 2012  Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
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

package com.owncloud.android;

import com.owncloud.android.authenticator.AccountAuthenticator;
import com.owncloud.android.utils.OwnCloudVersion;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class AccountUtils {
    public static final String WEBDAV_PATH_1_2 = "/webdav/owncloud.php";
    public static final String WEBDAV_PATH_2_0 = "/files/webdav.php";
    public static final String WEBDAV_PATH_4_0 = "/remote.php/webdav";
    public static final String CARDDAV_PATH_2_0 = "/apps/contacts/carddav.php";
    public static final String CARDDAV_PATH_4_0 = "/remote/carddav.php";
    public static final String STATUS_PATH = "/status.php";

    /**
     * Can be used to get the currently selected ownCloud account in the
     * preferences
     * 
     * @param context The current appContext
     * @return The current account or first available, if none is available,
     *         then null.
     */
    public static Account getCurrentOwnCloudAccount(Context context) {
        Account[] ocAccounts = AccountManager.get(context).getAccountsByType(
                AccountAuthenticator.ACCOUNT_TYPE);
        Account defaultAccount = null;

        SharedPreferences appPreferences = PreferenceManager
                .getDefaultSharedPreferences(context);
        String accountName = appPreferences
                .getString("select_oc_account", null);

        if (accountName != null) {
            for (Account account : ocAccounts) {
                if (account.name.equals(accountName)) {
                    defaultAccount = account;
                    break;
                }
            }
        }
        
        if (defaultAccount == null && ocAccounts.length != 0) {
            // we at least need to take first account as fallback
            defaultAccount = ocAccounts[0];
        }

        return defaultAccount;
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
    public static String getWebdavPath(OwnCloudVersion version) {
        if (version != null) {
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
     */
    public static String constructFullURLForAccount(Context context, Account account) {
        try {
            AccountManager ama = AccountManager.get(context);
            String baseurl = ama.getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL);
            String strver  = ama.getUserData(account, AccountAuthenticator.KEY_OC_VERSION);
            OwnCloudVersion ver = new OwnCloudVersion(strver);
            String webdavpath = getWebdavPath(ver);

            if (webdavpath == null) return null;
            return baseurl + webdavpath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
