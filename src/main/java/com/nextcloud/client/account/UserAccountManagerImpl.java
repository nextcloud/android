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
import android.net.Uri;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class UserAccountManagerImpl implements UserAccountManager {

    private static final String TAG = AccountUtils.class.getSimpleName();

    private Context context;
    private AccountManager accountManager;

    @Inject
    public UserAccountManagerImpl(
        Context context,
        AccountManager accountManager
    ) {
        this.context = context;
        this.accountManager = accountManager;
    }

    @Override
    @NonNull
    public Account[] getAccounts() {
        return accountManager.getAccountsByType(getAccountType());
    }

    @Nullable
    public Account getCurrentAccount() {
        return AccountUtils.getCurrentOwnCloudAccount(context);
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

    public void updateAccountVersion() {
        Account currentAccount = AccountUtils.getCurrentOwnCloudAccount(context);

        if (currentAccount == null) {
            return;
        }

        final String currentAccountVersion = accountManager.getUserData(currentAccount, com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_OC_ACCOUNT_VERSION);
        final boolean needsUpdate = !String.valueOf(ACCOUNT_VERSION_WITH_PROPER_ID).equalsIgnoreCase(currentAccountVersion);
        if (!needsUpdate) {
            return;
        }

        Log_OC.i(TAG, "Upgrading accounts to account version #" + ACCOUNT_VERSION_WITH_PROPER_ID);

        Account[] ocAccounts = accountManager.getAccountsByType(MainApp.getAccountType(context));
        String serverUrl;
        String username;
        String displayName;
        String newAccountName;
        Account newAccount;
        GetRemoteUserInfoOperation remoteUserNameOperation = new GetRemoteUserInfoOperation();

        for (Account account : ocAccounts) {
            // build new account name
            serverUrl = accountManager.getUserData(account, com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_OC_BASE_URL);

            // update user name
            try {
                OwnCloudAccount ocAccount = new OwnCloudAccount(account, context);
                OwnCloudClient client = OwnCloudClientManagerFactory.getDefaultSingleton()
                    .getClientFor(ocAccount, context);

                RemoteOperationResult result = remoteUserNameOperation.execute(client);

                if (result.isSuccess()) {
                    UserInfo userInfo = (UserInfo) result.getData().get(0);
                    username = userInfo.id;
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

            newAccountName = com.owncloud.android.lib.common.accounts.AccountUtils.
                buildAccountName(Uri.parse(serverUrl), username);

            // migrate to a new account, if needed
            if (!newAccountName.equals(account.name)) {
                newAccount = migrateAccount(context, currentAccount, accountManager, serverUrl, newAccountName,
                                            account);

            } else {
                // servers which base URL is in the root of their domain need no change
                Log_OC.d(TAG, account.name + " needs no upgrade ");
                newAccount = account;
            }

            accountManager.setUserData(newAccount, com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_DISPLAY_NAME, displayName);
            accountManager.setUserData(newAccount, com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_USER_ID, username);

            // at least, upgrade account version
            Log_OC.d(TAG, "Setting version " + ACCOUNT_VERSION_WITH_PROPER_ID + " to " + newAccountName);
            accountManager.setUserData(newAccount, com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_OC_ACCOUNT_VERSION,
                                   Integer.toString(ACCOUNT_VERSION_WITH_PROPER_ID));
        }
    }

    @NonNull
    private Account migrateAccount(Context context, Account currentAccount, AccountManager accountMgr,
                                          String serverUrl, String newAccountName, Account account) {

        Log_OC.d(TAG, "Upgrading " + account.name + " to " + newAccountName);

        // create the new account
        Account newAccount = new Account(newAccountName, MainApp.getAccountType(context));
        String password = accountMgr.getPassword(account);
        accountMgr.addAccountExplicitly(newAccount, (password != null) ? password : "", null);

        // copy base URL
        accountMgr.setUserData(newAccount, com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_OC_BASE_URL, serverUrl);

        // copy server version
        accountMgr.setUserData(
            newAccount,
            com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_OC_VERSION,
            accountMgr.getUserData(account, com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_OC_VERSION)
        );

        // copy cookies
        accountMgr.setUserData(
            newAccount,
            com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_COOKIES,
            accountMgr.getUserData(account, com.owncloud.android.lib.common.accounts.AccountUtils.Constants.KEY_COOKIES)
        );

        // don't forget the account saved in preferences as the current one
        if (currentAccount.name.equals(account.name)) {
            AccountUtils.setCurrentOwnCloudAccount(context, newAccountName);
        }

        // remove the old account
        accountMgr.removeAccount(account, null, null);

        // will assume it succeeds, not a big deal otherwise
        return newAccount;
    }

    private String getAccountType() {
        return context.getString(R.string.account_type);
    }
}
