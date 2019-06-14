/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2019 Chris Narkiewicz
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextcloud.client.account;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.client.preferences.AppPreferencesImpl;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class UserAccountManagerImpl implements UserAccountManager {

    private static final String TAG = AccountUtils.class.getSimpleName();
    private static final String PREF_SELECT_OC_ACCOUNT = "select_oc_account";

    private Context context;
    private AccountManager accountManager;

    public static UserAccountManagerImpl fromContext(Context context) {
        AccountManager am = (AccountManager)context.getSystemService(Context.ACCOUNT_SERVICE);
        return new UserAccountManagerImpl(context, am);
    }

    @Inject
    public UserAccountManagerImpl(
        Context context,
        AccountManager accountManager
    ) {
        this.context = context;
        this.accountManager = accountManager;
    }

    @Override
    public void removeAllAccounts() {
        for (Account account : getAccounts()) {
            accountManager.removeAccount(account, null, null);
        }
    }

    @Override
    @NonNull
    public Account[] getAccounts() {
        return accountManager.getAccountsByType(getAccountType());
    }

    @Override
    public boolean exists(Account account) {
        Account[] nextcloudAccounts = getAccounts();

        if (account != null && account.name != null) {
            int lastAtPos = account.name.lastIndexOf('@');
            String hostAndPort = account.name.substring(lastAtPos + 1);
            String username = account.name.substring(0, lastAtPos);
            String otherHostAndPort;
            String otherUsername;
            for (Account otherAccount : nextcloudAccounts) {
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

    @Nullable
    public Account getCurrentAccount() {
        return AccountUtils.getCurrentOwnCloudAccount(context);
    }

    @Override
    @Nullable
    public OwnCloudAccount getCurrentOwnCloudAccount() {
        try {
            Account currentPlatformAccount = getCurrentAccount();
            return new OwnCloudAccount(currentPlatformAccount, context);
        } catch (com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException ex) {
            return null;
        }
    }

    @Override
    @Nullable
    public Account getAccountByName(String name) {
        for (Account account : getAccounts()) {
            if (account.name.equals(name)) {
                return account;
            }
        }

        return null;
    }

    @Override
    public boolean setCurrentOwnCloudAccount(String accountName) {
        boolean result = false;
        if (accountName != null) {
            for (final Account account : getAccounts()) {
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

    @Override
    public boolean setCurrentOwnCloudAccount(int hashCode) {
        boolean result = false;
        if (hashCode != 0) {
            for (final Account account : getAccounts()) {
                if (hashCode == account.hashCode()) {
                    SharedPreferences.Editor appPrefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
                    appPrefs.putString(PREF_SELECT_OC_ACCOUNT, account.name);
                    appPrefs.apply();
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    @Override
    @NonNull
    public OwnCloudVersion getServerVersion(Account account) {
        OwnCloudVersion serverVersion = MainApp.MINIMUM_SUPPORTED_SERVER_VERSION;

        if (account != null) {
            AccountManager accountMgr = AccountManager.get(MainApp.getAppContext());
            String serverVersionStr = accountMgr.getUserData(account, com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_OC_VERSION);

            if (serverVersionStr != null) {
                serverVersion = new OwnCloudVersion(serverVersionStr);
            }
        }

        return serverVersion;
    }

    @Override
    public boolean isSearchSupported(Account account) {
        return account != null && getServerVersion(account).isSearchSupported();
    }

    @Override
    public boolean isMediaStreamingSupported(Account account) {
        return account != null && getServerVersion(account).isMediaStreamingSupported();
    }

    @Override
    public void resetOwnCloudAccount() {
        SharedPreferences.Editor appPrefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
        appPrefs.putString(PREF_SELECT_OC_ACCOUNT, null);
        appPrefs.apply();
    }

    @Override
    public  boolean accountOwnsFile(OCFile file, Account account) {
        return !TextUtils.isEmpty(file.getOwnerId()) && account.name.split("@")[0].equals(file.getOwnerId());
    }

    public void migrateUserId() {
        AppPreferences appPreferences = AppPreferencesImpl.fromContext(context);
        if (appPreferences.isUserIdMigrated()) {
            // migration done
            return;
        }

        boolean success = false;
        Account[] ocAccounts = accountManager.getAccountsByType(MainApp.getAccountType(context));
        String userId;
        String displayName;
        GetUserInfoRemoteOperation remoteUserNameOperation = new GetUserInfoRemoteOperation();

        for (Account account : ocAccounts) {
            String storedUserId = accountManager.getUserData(account, com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_USER_ID);

            if (!TextUtils.isEmpty(storedUserId)) {
                continue;
            }

            // add userId
            try {
                OwnCloudAccount ocAccount = new OwnCloudAccount(account, context);
                OwnCloudClient client = OwnCloudClientManagerFactory.getDefaultSingleton()
                    .getClientFor(ocAccount, context);

                RemoteOperationResult result = remoteUserNameOperation.execute(client);

                if (result.isSuccess()) {
                    UserInfo userInfo = (UserInfo) result.getData().get(0);
                    userId = userInfo.id;
                    displayName = userInfo.displayName;
                } else {
                    // skip account, try it next time
                    Log_OC.e(TAG, "Error while getting username for account: " + account.name);
                    continue;
                }
            } catch (Exception e) {
                Log_OC.e(TAG, "Error while getting username: " + e.getMessage());
                continue;
            }

            accountManager.setUserData(account,
                                       com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_DISPLAY_NAME,
                                       displayName);
            accountManager.setUserData(account,
                                       com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_USER_ID,
                                       userId);

            success = true;
        }

        if (success) {
            appPreferences.setMigratedUserId(true);
        }
    }

    private String getAccountType() {
        return context.getString(R.string.account_type);
    }
}
