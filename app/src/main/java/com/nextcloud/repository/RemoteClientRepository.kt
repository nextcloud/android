/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.repository

import android.content.Context
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.ApplicationScope
import com.nextcloud.common.NextcloudClient
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("TooGenericExceptionCaught", "DEPRECATION")
class RemoteClientRepository @Inject constructor(
    private val context: Context,
    private val accountManager: UserAccountManager,
    @param:ApplicationScope private val scope: CoroutineScope
) : ClientRepository {
    companion object {
        private const val TAG = "ClientRepository"
    }

    private val clientFactory = OwnCloudClientManagerFactory.getDefaultSingleton()

    override fun getNextcloudClient(onComplete: (NextcloudClient) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val client = clientFactory.getNextcloudClientFor(accountManager.user.toOwnCloudAccount(), context)
                onComplete(client)
            } catch (e: Exception) {
                Log_OC.d(TAG, "Exception caught getNextcloudClient(): $e")
            }
        }
    }

    override suspend fun getNextcloudClient(): NextcloudClient? = withContext(Dispatchers.IO) {
        try {
            clientFactory.getNextcloudClientFor(accountManager.user.toOwnCloudAccount(), context)
        } catch (e: Exception) {
            Log_OC.d(TAG, "Exception caught getNextcloudClient(): $e")
            null
        }
    }

    override fun getOwncloudClient(onComplete: (OwnCloudClient) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val client = clientFactory.getClientFor(accountManager.user.toOwnCloudAccount(), context)
                onComplete(client)
            } catch (e: Exception) {
                Log_OC.d(TAG, "Exception caught getOwncloudClient(): $e")
            }
        }
    }

    override suspend fun getOwncloudClient(): OwnCloudClient? = withContext(Dispatchers.IO) {
        try {
            clientFactory.getClientFor(accountManager.user.toOwnCloudAccount(), context)
        } catch (e: Exception) {
            Log_OC.d(TAG, "Exception caught getOwncloudClient(): $e")
            null
        }
    }
}
