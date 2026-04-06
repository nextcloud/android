/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018 Edvard Holst <edvard.holst@gmail.com>
 * SPDX-FileCopyrightText: 2025 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activities.data.activities

import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.common.NextcloudClient
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.activities.GetActivitiesRemoteOperation
import com.owncloud.android.ui.activities.data.activities.ActivitiesServiceApi.ActivitiesServiceCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.httpclient.HttpStatus

/**
 * Implementation of the Activities Service API that communicates with the NextCloud remote server
 */
@Suppress("TooGenericExceptionThrown")
class ActivitiesServiceApiImpl(private val accountManager: UserAccountManager) : ActivitiesServiceApi {

    override fun getAllActivities(
        lifecycleScope: CoroutineScope,
        lastGiven: Long,
        callback: ActivitiesServiceCallback<List<Any>>
    ) {
        lifecycleScope.launch(Dispatchers.Main) {
            val result = runCatching {
                withContext(Dispatchers.IO) { fetchActivities(lastGiven) }
            }

            result.fold(
                onSuccess = { (activities, client, updatedLastGiven) ->
                    callback.onLoaded(activities, client, updatedLastGiven)
                },
                onFailure = { error ->
                    Log_OC.e(TAG, "Failed to fetch activities", error)
                    callback.onError(error.message ?: "")
                }
            )
        }
    }

    private data class ActivitiesResult(val activities: List<Any>, val client: NextcloudClient, val lastGiven: Long)

    private fun fetchActivities(lastGiven: Long): ActivitiesResult {
        val context = MainApp.getAppContext()
        val ocAccount = accountManager.user.toOwnCloudAccount()
        val client = OwnCloudClientManagerFactory.getDefaultSingleton()
            .getNextcloudClientFor(ocAccount, context)

        val operation = if (lastGiven > 0) {
            GetActivitiesRemoteOperation(lastGiven)
        } else {
            GetActivitiesRemoteOperation()
        }

        val result = operation.execute(client)

        if (result.isSuccess && result.getData() != null) {
            val data = result.getData()
            val activities = data[0] as List<Any?>
            val updatedLastGiven = data[1] as Long
            val items = activities.filterNotNull()
            return ActivitiesResult(items, client, updatedLastGiven)
        }

        val errorMessage = if (result.httpCode == HttpStatus.SC_NOT_MODIFIED) {
            context.getString(R.string.file_list_empty_headline_server_search)
        } else {
            result.getLogMessage(context)
        }

        Log_OC.d(TAG, result.logMessage)
        throw RuntimeException(errorMessage)
    }

    companion object {
        private val TAG: String = ActivitiesServiceApiImpl::class.java.simpleName
    }
}
