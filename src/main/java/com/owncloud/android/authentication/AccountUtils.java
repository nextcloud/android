/**
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
 *
 */

package com.owncloud.android.authentication;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.lib.common.accounts.AccountUtils.Constants;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation;
import com.owncloud.android.operations.GetCapabilitiesOperarion;
import com.owncloud.android.ui.activity.ManageAccountsActivity;

import java.util.Locale;

public class AccountUtils {
    private static final String TAG = AccountUtils.class.getSimpleName();

    public static final int ACCOUNT_VERSION = 1;
    public static final int ACCOUNT_VERSION_WITH_PROPER_ID = 2;
    public static final String ACCOUNT_USES_STANDARD_PASSWORD = "ACCOUNT_USES_STANDARD_PASSWORD";

    /**
     * Can be used to get the currently selected ownCloud {@link Account} in the
     * application preferences.
     * 
     * @param   context     The current application {@link Context}
     * @return              The ownCloud {@link Account} currently saved in preferences, or the first 
     *                      {@link Account} available, if valid (still registered in the system as ownCloud 
     *                      account). If none is available and valid, returns null.
     */
    public static @Nullable
    Account getCurrentOwnCloudAccount(Context context) {
        Account[] ocAccounts = getAccounts(context);
        Account defaultAccount = null;

        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(context.getContentResolver());

        SharedPreferences appPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String accountName = appPreferences.getString("select_oc_account", null);

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
        return accountManager.getAccountsByType(MainApp.getAccountType());
    }

    
    public static boolean exists(Account account, Context context) {
        Account[] ocAccounts = getAccounts(context);

        if (account != null && account.name != null) {
            int lastAtPos = account.name.lastIndexOf("@");
            String hostAndPort = account.name.substring(lastAtPos + 1);
            String username = account.name.substring(0, lastAtPos);
            String otherHostAndPort;
            String otherUsername;
            Locale currentLocale = context.getResources().getConfiguration().locale;
            for (Account otherAccount : ocAccounts) {
                lastAtPos = otherAccount.name.lastIndexOf("@");
                otherHostAndPort = otherAccount.name.substring(lastAtPos + 1);
                otherUsername = otherAccount.name.substring(0, lastAtPos);
                if (otherHostAndPort.equals(hostAndPort) &&
                        otherUsername.toLowerCase(currentLocale).
                            equals(username.toLowerCase(currentLocale))) {
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
        Account[] ocAccounts = AccountManager.get(context).getAccountsByType(
                MainApp.getAccountType());
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
            boolean found;
            for (final Account account : getAccounts(context)) {
                found = (account.name.equals(accountName));
                if (found) {
                    SharedPreferences.Editor appPrefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
                    appPrefs.putString("select_oc_account", accountName);

                    // update credentials
                    Thread t = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            FileDataStorageManager storageManager = new FileDataStorageManager(account,
                                    context.getContentResolver());
                            GetCapabilitiesOperarion getCapabilities = new GetCapabilitiesOperarion();
                            RemoteOperationResult updateResult = getCapabilities.execute(storageManager, context);
                            Log_OC.w(TAG, "Update Capabilities: " + updateResult.isSuccess());
                        }
                    });

                    t.start();

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
        appPrefs.putString("select_oc_account", null);

        appPrefs.apply();
    }

    /**
     * Update the accounts in AccountManager to meet the current version of accounts expected by the app, if needed.
     *
     * Introduced to handle a change in the structure of stored account names needed to allow different OC servers
     * in the same domain, but not in the same path.
     *
     * @param context Used to access the AccountManager.
     */
    public static void updateAccountVersion(Context context) {
        Account currentAccount = AccountUtils.getCurrentOwnCloudAccount(context);
        AccountManager accountMgr = AccountManager.get(context);

        if ( currentAccount != null ) {
            String currentAccountVersion = accountMgr.getUserData(currentAccount, Constants.KEY_OC_ACCOUNT_VERSION);

            if (!String.valueOf(ACCOUNT_VERSION_WITH_PROPER_ID).equalsIgnoreCase(currentAccountVersion)) {
                Log_OC.i(TAG, "Upgrading accounts to account version #" + ACCOUNT_VERSION_WITH_PROPER_ID);
                Account[] ocAccounts = accountMgr.getAccountsByType(MainApp.getAccountType());
                String serverUrl;
                String username;
                String newAccountName;
                String password;
                Account newAccount;
                for (Account account : ocAccounts) {
                    // build new account name
                    serverUrl = accountMgr.getUserData(account, Constants.KEY_OC_BASE_URL);

                    // update user name
                    try {
                        OwnCloudAccount ocAccount = new OwnCloudAccount(account, context);
                        OwnCloudClient client = OwnCloudClientManagerFactory.getDefaultSingleton()
                                .getClientFor(ocAccount, context);

                        GetRemoteUserInfoOperation remoteUserNameOperation = new GetRemoteUserInfoOperation();
                        RemoteOperationResult result = remoteUserNameOperation.execute(client);

                        if (result.isSuccess()) {
                            UserInfo userInfo = (UserInfo) result.getData().get(0);
                            username = userInfo.id;
                        } else {
                            // skip account, try it next time
                            Log_OC.e(TAG, "Error while getting username for account: " + account.name);
                            continue;
                        }
                    } catch (Exception e) {
                        Log_OC.e(TAG, "Error while getting username: " + e.getMessage());
                        continue;
                    }

                    newAccountName = com.owncloud.android.lib.common.accounts.AccountUtils.
                            buildAccountName(Uri.parse(serverUrl), username);

                    // migrate to a new account, if needed
                    if (!newAccountName.equals(account.name)) {
                        Log_OC.d(TAG, "Upgrading " + account.name + " to " + newAccountName);

                        // create the new account
                        newAccount = new Account(newAccountName, MainApp.getAccountType());
                        password = accountMgr.getPassword(account);
                        accountMgr.addAccountExplicitly(newAccount, (password != null) ? password : "", null);

                        // copy base URL
                        accountMgr.setUserData(newAccount, Constants.KEY_OC_BASE_URL, serverUrl);

                        // copy server version
                        accountMgr.setUserData(
                                newAccount,
                                Constants.KEY_OC_VERSION,
                                accountMgr.getUserData(account, Constants.KEY_OC_VERSION)
                        );

                        // copy cookies
                        accountMgr.setUserData(
                                newAccount,
                                Constants.KEY_COOKIES,
                                accountMgr.getUserData(account, Constants.KEY_COOKIES)
                        );

                        // copy type of authentication
                        final String isSamlStr = accountMgr.getUserData(account, Constants.KEY_SUPPORTS_SAML_WEB_SSO);
                        if (Boolean.parseBoolean(isSamlStr)) {
                            accountMgr.setUserData(newAccount, Constants.KEY_SUPPORTS_SAML_WEB_SSO, "TRUE");
                        }

                        final String isOauthStr = accountMgr.getUserData(account, Constants.KEY_SUPPORTS_OAUTH2);
                        if (Boolean.parseBoolean(isOauthStr)) {
                            accountMgr.setUserData(newAccount, Constants.KEY_SUPPORTS_OAUTH2, "TRUE");
                        }
                        /* TODO - study if it's possible to run this method in a background thread to copy the authToken
                        if (isOAuth || isSaml) {
                            accountMgr.setAuthToken(newAccount, mAuthTokenType, mAuthToken);
                        }
                        */

                        // don't forget the account saved in preferences as the current one
                        if (currentAccount.name.equals(account.name)) {
                            AccountUtils.setCurrentOwnCloudAccount(context, newAccountName);
                        }

                        // remove the old account
                        accountMgr.removeAccount(account, null, null);
                            // will assume it succeeds, not a big deal otherwise

                    } else {
                        // servers which base URL is in the root of their domain need no change
                        Log_OC.d(TAG, account.name + " needs no upgrade ");
                        newAccount = account;
                    }

                    // at least, upgrade account version
                    Log_OC.d(TAG, "Setting version " + ACCOUNT_VERSION_WITH_PROPER_ID + " to " + newAccountName);
                    accountMgr.setUserData(newAccount,
                            Constants.KEY_OC_ACCOUNT_VERSION, Integer.toString(ACCOUNT_VERSION_WITH_PROPER_ID));
                }
            }
        }
    }

    /**
     * Access the version of the OC server corresponding to an account SAVED IN THE ACCOUNTMANAGER
     *
     * @param   account     ownCloud account
     * @return              Version of the OC server corresponding to account, according to the data saved
     *                      in the system AccountManager
     */
    public static OwnCloudVersion getServerVersion(Account account) {
        OwnCloudVersion serverVersion = null;
        if (account != null) {
            AccountManager accountMgr = AccountManager.get(MainApp.getAppContext());
            String serverVersionStr = accountMgr.getUserData(account, Constants.KEY_OC_VERSION);
            if (serverVersionStr != null) {
                serverVersion = new OwnCloudVersion(serverVersionStr);
            }
        }
        return serverVersion;
    }

    public static boolean hasSearchUsersSupport(Account account){
        OwnCloudVersion serverVersion = getServerVersion(account);
        return (serverVersion != null && serverVersion.isSearchUsersSupported());
    }

    public static boolean hasSearchSupport(Account account) {
        OwnCloudVersion serverVersion = getServerVersion(account);
        return (serverVersion != null && serverVersion.isSearchSupported());
    }
}
