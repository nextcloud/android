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

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface UserAccountManager extends CurrentAccountProvider {

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
     * Check if Nextcloud account is registered in {@link android.accounts.AccountManager}
     *
     * @param account Account to check for
     * @return true if account is registered, false otherwise
     */
    boolean exists(Account account);

    /**
     * Verifies that every account has userId set.
     */
    void migrateUserId();

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
    @NonNull
    OwnCloudVersion getServerVersion(Account account);

    boolean isSearchSupported(@Nullable Account account);
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

}
