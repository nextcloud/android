/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.account;

import android.accounts.Account;

import androidx.annotation.NonNull;

/**
 * This interface provides access to currently selected user.
 *
 * @see UserAccountManager
 */
public interface CurrentAccountProvider {
    /**
     * Get currently active user profile. If there is no active user, anonymous user is returned.
     *
     * @return User profile. Profile is never null.
     */
    @NonNull
    default User getUser() {
        return new AnonymousUser("dummy");
    }
}
