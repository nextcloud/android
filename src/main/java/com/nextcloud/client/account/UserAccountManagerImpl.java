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
import android.text.TextUtils;

import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.client.preferences.AppPreferencesImpl;
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

    public void migrateUserId() {
        boolean success = false;

        AppPreferences appPreferences = AppPreferencesImpl.fromContext(context);

        if (appPreferences.isUserIdMigrated()) {
            // migration done
            return;
        }


        Account[] ocAccounts = accountManager.getAccountsByType(MainApp.getAccountType(context));
        String userId;
        String displayName;
        GetRemoteUserInfoOperation remoteUserNameOperation = new GetRemoteUserInfoOperation();

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
