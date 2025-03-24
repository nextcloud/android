/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.repository

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.coroutineScope
import com.nextcloud.client.account.User
import com.nextcloud.common.NextcloudClient
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ClientRepository(
    private val user: User,
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
): ClientRepositoryType {
    private val tag = "ClientRepository"

    override fun getNextcloudClient(onComplete: (NextcloudClient) -> Unit) {
        lifecycleOwner.lifecycle.coroutineScope.launch(Dispatchers.IO) {
            try {
                val client = OwnCloudClientFactory.createNextcloudClient(user.toPlatformAccount(), context)
                onComplete(client)
            } catch (e: Exception) {
                Log_OC.d(tag, "Exception caught getNextcloudClient(): $e")
            }
        }
    }

    override suspend fun getNextcloudClient(): NextcloudClient? {
        return withContext(Dispatchers.IO) {
            try {
                OwnCloudClientFactory.createNextcloudClient(user.toPlatformAccount(), context)
            } catch (e: Exception) {
                Log_OC.d(tag, "Exception caught getNextcloudClient(): $e")
                null
            }
        }
    }

    override fun getOwncloudClient(onComplete: (OwnCloudClient) -> Unit) {
        lifecycleOwner.lifecycle.coroutineScope.launch(Dispatchers.IO) {
            try {
                val client = OwnCloudClientFactory.createOwnCloudClient(user.toPlatformAccount(), context)
                onComplete(client)
            } catch (e: Exception) {
                Log_OC.d(tag, "Exception caught getOwncloudClient(): $e")
            }
        }
    }

    override suspend fun getOwncloudClient(): OwnCloudClient? {
        return withContext(Dispatchers.IO) {
            try {
                OwnCloudClientFactory.createOwnCloudClient(user.toPlatformAccount(), context)
            } catch (e: Exception) {
                Log_OC.d(tag, "Exception caught getOwncloudClient(): $e")
                null
            }
        }
    }
}
