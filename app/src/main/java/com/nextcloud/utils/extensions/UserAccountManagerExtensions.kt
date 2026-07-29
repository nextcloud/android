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
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.utils.Log_OC

private const val TAG = "UserAccountManagerExtensions"

fun UserAccountManager.createOwncloudClient(): OwnCloudClient? = createOwncloudClient(currentAccount.name)

@Suppress("TooGenericExceptionCaught", "ReturnCount", "DEPRECATION")
fun UserAccountManager.createOwncloudClient(accountName: String): OwnCloudClient? {
    val context = context ?: MainApp.getAppContext()
    if (context == null) {
        Log_OC.e(TAG, "app context is null, cannot create client")
        return null
    }

    val user = getUser(accountName).orElse(null)
    if (user == null || user.isAnonymous) {
        Log_OC.e(TAG, "account is not registered, cannot create client for: $accountName")
        return null
    }

    return try {
        val result = OwnCloudClientFactory.createOwnCloudClient(user.toPlatformAccount(), context)
        Log_OC.i(TAG, "client created")
        result
    } catch (e: AccountUtils.AccountNotFoundException) {
        Log_OC.e(TAG, "account removed while creating client for: $accountName", e)
        null
    } catch (e: Exception) {
        Log_OC.e(TAG, "cannot create client: ", e)
        null
    }
}
