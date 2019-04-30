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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface UserAccountManager extends CurrentAccountProvider {

    int ACCOUNT_VERSION = 1;
    int ACCOUNT_VERSION_WITH_PROPER_ID = 2;
    String ACCOUNT_USES_STANDARD_PASSWORD = "ACCOUNT_USES_STANDARD_PASSWORD";

    /**
     * Get configured NextCloud's user accounts.
     *
     * @return Array of accounts or empty array, if accounts are not configured.
     */
    @NonNull
    Account[] getAccounts();

    /**
     * Update the accounts in AccountManager to meet the current
     * version of accounts expected by the app, if needed.
     * <p>
     * Introduced to handle a change in the structure of stored account names
     * needed to allow different Nextcloud servers in the same domain, but not in
     * the same path.
     */
    void updateAccountVersion();

    @Nullable
    Account getAccountByName(String name);

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
