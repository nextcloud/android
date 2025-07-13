/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.repository

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.account.User
import com.nextcloud.common.NextcloudClient
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("TooGenericExceptionCaught", "DEPRECATION")
class RemoteClientRepository(private val user: User, private val context: Context, lifecycleOwner: LifecycleOwner) :
    ClientRepository {
    private val tag = "ClientRepository"
    private val clientFactory = OwnCloudClientManagerFactory.getDefaultSingleton()
    private val scope = lifecycleOwner.lifecycleScope

    override fun getNextcloudClient(onComplete: (NextcloudClient) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val client = clientFactory.getNextcloudClientFor(user.toOwnCloudAccount(), context)
                onComplete(client)
            } catch (e: Exception) {
                Log_OC.d(tag, "Exception caught getNextcloudClient(): $e")
            }
        }
    }

    override suspend fun getNextcloudClient(): NextcloudClient? = withContext(Dispatchers.IO) {
        try {
            clientFactory.getNextcloudClientFor(user.toOwnCloudAccount(), context)
        } catch (e: Exception) {
            Log_OC.d(tag, "Exception caught getNextcloudClient(): $e")
            null
        }
    }

    override fun getOwncloudClient(onComplete: (OwnCloudClient) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val client = clientFactory.getClientFor(user.toOwnCloudAccount(), context)
                onComplete(client)
            } catch (e: Exception) {
                Log_OC.d(tag, "Exception caught getOwncloudClient(): $e")
            }
        }
    }

    override suspend fun getOwncloudClient(): OwnCloudClient? = withContext(Dispatchers.IO) {
        try {
            clientFactory.getClientFor(user.toOwnCloudAccount(), context)
        } catch (e: Exception) {
            Log_OC.d(tag, "Exception caught getOwncloudClient(): $e")
            null
        }
    }
}
