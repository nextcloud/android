/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.network

import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager

data class ConnectivityKey(val accountName: String, val baseUrl: String) {
    companion object {
        /**
         * [UserAccountManager.getUser] rebuilds the user from AccountManager, the shared preferences and the
         * arbitrary data provider on every call, so it is resolved once here.
         */
        @JvmStatic
        fun getBy(accountManager: UserAccountManager): ConnectivityKey = getBy(accountManager.user)

        @JvmStatic
        fun getBy(user: User): ConnectivityKey = ConnectivityKey(user.accountName, user.server.uri.toString())
    }
}
