/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.nextcloud.android.common.ui.network.auth.ServerCredentials
import com.nextcloud.client.account.User
import com.owncloud.android.MainApp
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC

private const val TAG = "UserExtensions"

@Suppress("TooGenericExceptionCaught")
fun User.toServerCredentials(): ServerCredentials? = try {
    OwnCloudClientManagerFactory
        .getDefaultSingleton()
        .getClientFor(toOwnCloudAccount(), MainApp.getAppContext())
        .toServerCredentials(server.uri.toString())
} catch (e: Exception) {
    Log_OC.e(TAG, "Failed to create client for $accountName", e)
    null
}
