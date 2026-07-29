/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.nextcloud.client.account.UserAccountManager
import com.owncloud.android.MainApp
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.utils.Log_OC

private const val TAG = "UserAccountManagerExtensions"

@Suppress("TooGenericExceptionCaught", "ReturnCount")
fun UserAccountManager.createOwncloudClient(): OwnCloudClient? {
    val context = context ?: MainApp.getAppContext()
    if (context == null) {
        Log_OC.e(TAG, "app context is null, cannot create client")
        return null
    }

    val account = currentAccount
    if (account.isAnonymous(context)) {
        Log_OC.e(TAG, "current account is anonymous, cannot create client")
        return null
    }

    return try {
        val result = OwnCloudClientFactory.createOwnCloudClient(account, context)
        Log_OC.i(TAG, "client created")
        result
    } catch (e: Exception) {
        Log_OC.e(TAG, "cannot create client: ", e)
        null
    }
}
