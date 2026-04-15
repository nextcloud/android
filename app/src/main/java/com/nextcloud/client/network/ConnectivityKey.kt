/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.network

import com.nextcloud.client.account.UserAccountManager

data class ConnectivityKey(val accountName: String, val baseUrl: String) {
    companion object {
        fun getBy(accountManager: UserAccountManager): ConnectivityKey = ConnectivityKey(
            accountManager.user.accountName,
            accountManager.user.server.uri.toString()
        )
    }
}
