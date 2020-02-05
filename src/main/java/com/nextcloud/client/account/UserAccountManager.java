/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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
import android.app.Activity;
import android.content.Intent;

import com.nextcloud.java.util.Optional;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface UserAccountManager extends CurrentAccountProvider {

    int ACCOUNT_VERSION = 1;
    int ACCOUNT_VERSION_WITH_PROPER_ID = 2;
    String ACCOUNT_USES_STANDARD_PASSWORD = "ACCOUNT_USES_STANDARD_PASSWORD";
    String PENDING_FOR_REMOVAL = "PENDING_FOR_REMOVAL";

    @Nullable
    OwnCloudAccount getCurrentOwnCloudAccount();

    /**
     * Remove all NextCloud accounts from OS account manager.
     */
    void removeAllAccounts();

    /**
     * Get configured NextCloud's user accounts.
     *
     * @return Array of accounts or empty array, if accounts are not configured.
     */
    @NonNull
    Account[] getAccounts();

    /**
     * Get configured nextcloud user accounts
     * @return List of users or empty list, if users are not registered.
     */
    @NonNull
    List<User> getAllUsers();

    /**
     * Get user with a specific account name.
     *
     * @param accountName Account name of the requested user
     * @return User or empty optional if user does not exist.
     */
    @NonNull
    Optional<User> getUser(CharSequence accountName);

    /**
     * Check if Nextcloud account is registered in {@link android.accounts.AccountManager}
     *
     * @param account Account to check for
     * @return true if account is registered, false otherwise
     */
    boolean exists(Account account);

    /**
     * Verifies that every account has userId set
     */
    boolean migrateUserId();

    @Nullable
    Account getAccountByName(String name);

    boolean setCurrentOwnCloudAccount(String accountName);

    boolean setCurrentOwnCloudAccount(int hashCode);

    /**
     * Access the version of the OC server corresponding to an account SAVED IN THE ACCOUNTMANAGER
     *
     * @param account ownCloud account
     * @return Version of the OC server corresponding to account, according to the data saved
     * in the system AccountManager
     */
    @Deprecated
    @NonNull
    OwnCloudVersion getServerVersion(Account account);

    /**
     * Check if user's account supports media streaming. This is a property of server where user has his account.
     *
     * @deprecated Please use {@link OwnCloudVersion#isMediaStreamingSupported()} directly,
     * obtainable from {@link User#getServer()} and {@link Server#getVersion()}
     *
     * @param account Account used to perform {@link android.accounts.AccountManager} lookup.
     *
     * @return true is server supports media streaming, false otherwise
     */
    @Deprecated
    boolean isMediaStreamingSupported(@Nullable Account account);

    void resetOwnCloudAccount();

    /**
     * Checks if an account owns the file (file's ownerId is the same as account name)
     *
     * @param file File to check
     * @param account account to compare
     * @return false if ownerId is not set or owner is a different account
     */
    boolean accountOwnsFile(OCFile file, Account account);

    /**
     * Extract username from account.
     *
     * Full account name is in form of "username@nextcloud.domain".
     *
     * @param account Account instance
     * @return User name (without domain) or null, if name cannot be extracted.
     */
    static String getUsername(Account account) {
        if (account != null && account.name != null) {
            return account.name.substring(0, account.name.lastIndexOf('@'));
        } else {
            return null;
        }
    }

    /**
     * Launch account registration activity.
     *
     * This method returns immediately. Authenticator activity will be launched asynchronously.
     *
     * @param activity Activity used to launch authenticator flow via {@link Activity#startActivity(Intent)}
     */
    void startAccountCreation(Activity activity);
}
