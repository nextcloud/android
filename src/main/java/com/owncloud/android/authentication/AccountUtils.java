/*
 *   ownCloud Android client application
 *
 *   Copyright (C) 2012  Bartek Przybylski
 *   Copyright (C) 2016 ownCloud Inc.
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
 */

package com.owncloud.android.authentication;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.ui.activity.ManageAccountsActivity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Helper class for dealing with accounts.
 */
public final class AccountUtils {
    private static final String PREF_SELECT_OC_ACCOUNT = "select_oc_account";

    public static final int ACCOUNT_VERSION = 1;
    //public static final int ACCOUNT_VERSION_WITH_PROPER_ID = 2;
    public static final String ACCOUNT_USES_STANDARD_PASSWORD = "ACCOUNT_USES_STANDARD_PASSWORD";

    private AccountUtils() {
        // Required empty constructor
    }

    /**
     * Can be used to get the currently selected ownCloud {@link Account} in the
     * application preferences.
     *
     * @param   context     The current application {@link Context}
     * @return The ownCloud {@link Account} currently saved in preferences, or the first
     *                      {@link Account} available, if valid (still registered in the system as ownCloud
     *                      account). If none is available and valid, returns null.
     */
    public static @Nullable Account getCurrentOwnCloudAccount(Context context) {
        Account[] ocAccounts = getAccounts(context);
        Account defaultAccount = null;

        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(context.getContentResolver());

        SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String accountName = appPreferences.getString(PREF_SELECT_OC_ACCOUNT, null);

        // account validation: the saved account MUST be in the list of ownCloud Accounts known by the AccountManager
        if (accountName != null) {
            for (Account account : ocAccounts) {
                if (account.name.equals(accountName)) {
                    defaultAccount = account;
                    break;
                }
            }
        }

        if (defaultAccount == null && ocAccounts.length > 0) {
            // take first which is not pending for removal account as fallback
            for (Account account: ocAccounts) {
                boolean pendingForRemoval = arbitraryDataProvider.getBooleanValue(account,
                        ManageAccountsActivity.PENDING_FOR_REMOVAL);

                if (!pendingForRemoval) {
                    defaultAccount = account;
                    break;
                }
            }
        }

        return defaultAccount;
    }

    public static Account[] getAccounts(Context context) {
        AccountManager accountManager = AccountManager.get(context);
        return accountManager.getAccountsByType(MainApp.getAccountType(context));
    }


    public static boolean exists(Account account, Context context) {
        Account[] ocAccounts = getAccounts(context);

        if (account != null && account.name != null) {
            int lastAtPos = account.name.lastIndexOf('@');
            String hostAndPort = account.name.substring(lastAtPos + 1);
            String username = account.name.substring(0, lastAtPos);
            String otherHostAndPort;
            String otherUsername;
            for (Account otherAccount : ocAccounts) {
                lastAtPos = otherAccount.name.lastIndexOf('@');
                otherHostAndPort = otherAccount.name.substring(lastAtPos + 1);
                otherUsername = otherAccount.name.substring(0, lastAtPos);
                if (otherHostAndPort.equals(hostAndPort) &&
                        otherUsername.equalsIgnoreCase(username)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * returns the user's name based on the account name.
     *
     * @param accountName the account name
     * @return the user's name
     */
    public static String getAccountUsername(String accountName) {
        if (accountName != null) {
            return accountName.substring(0, accountName.lastIndexOf('@'));
        } else {
            return null;
        }
    }

    /**
     * Returns owncloud account identified by accountName or null if it does not exist.
     * @param context the context
     * @param accountName name of account to be returned
     * @return owncloud account named accountName
     */
    public static Account getOwnCloudAccountByName(Context context, String accountName) {
        Account[] ocAccounts = AccountManager.get(context).getAccountsByType(MainApp.getAccountType(context));
        for (Account account : ocAccounts) {
            if(account.name.equals(accountName)) {
                return account;
            }
        }
        return null;
    }


    public static boolean setCurrentOwnCloudAccount(final Context context, String accountName) {
        boolean result = false;
        if (accountName != null) {
            for (final Account account : getAccounts(context)) {
                if (accountName.equals(account.name)) {
                    SharedPreferences.Editor appPrefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
                    appPrefs.putString(PREF_SELECT_OC_ACCOUNT, accountName);
                    appPrefs.apply();
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    public static void resetOwnCloudAccount(Context context) {
        SharedPreferences.Editor appPrefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
        appPrefs.putString(PREF_SELECT_OC_ACCOUNT, null);

        appPrefs.apply();
    }

    /**
     * Access the version of the OC server corresponding to an account SAVED IN THE ACCOUNTMANAGER
     *
     * @param account ownCloud account
     * @return Version of the OC server corresponding to account, according to the data saved
     * in the system AccountManager
     */
    public static @NonNull
    OwnCloudVersion getServerVersion(Account account) {
        OwnCloudVersion serverVersion = MainApp.MINIMUM_SUPPORTED_SERVER_VERSION;

        if (account != null) {
            AccountManager accountMgr = AccountManager.get(MainApp.getAppContext());
            String serverVersionStr = accountMgr.getUserData(account, Constants.KEY_OC_VERSION);

            if (serverVersionStr != null) {
                serverVersion = new OwnCloudVersion(serverVersionStr);
            }
        }

        return serverVersion;
    }

    public static boolean hasSearchSupport(Account account) {
        return getServerVersion(account).isSearchSupported();
    }
}
