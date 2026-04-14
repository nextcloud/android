/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2018-2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.owncloud.android.ui.asynctasks

import androidx.lifecycle.lifecycleScope
import com.nextcloud.common.NextcloudClient
import com.nextcloud.common.OkHttpMethodBase
import com.nextcloud.operations.DeleteMethod
import com.nextcloud.operations.GetMethod
import com.nextcloud.operations.PostMethod
import com.nextcloud.operations.PutMethod
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.notifications.models.Action
import com.owncloud.android.lib.resources.notifications.models.Notification
import com.owncloud.android.ui.adapter.NotificationListAdapter
import com.owncloud.android.ui.fragment.notifications.NotificationsFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.RequestBody
import org.apache.commons.httpclient.HttpStatus
import java.io.IOException

@Suppress("ReturnCount")
class NotificationExecuteActionTask(
    private val client: NextcloudClient,
    private val holder: NotificationListAdapter.NotificationViewHolder,
    private val notification: Notification,
    private val fragment: NotificationsFragment
) {

    fun execute(action: Action) {
        fragment.lifecycleScope.launch {
            val isSuccess = withContext(Dispatchers.IO) {
                performRequest(action)
            }

            fragment.onActionCallback(isSuccess, notification, holder)
        }
    }

    private fun performRequest(action: Action): Boolean {
        if (action.type == null || action.link == null) {
            return false
        }

        val link = action.link ?: return false
        val method: OkHttpMethodBase = when (action.type) {
            "GET" -> GetMethod(link, true)
            "POST" -> PostMethod(link, true, RequestBody.create(null, ""))
            "DELETE" -> DeleteMethod(link, true)
            "PUT" -> PutMethod(link, true, null)
            else -> return false
        }

        method.addRequestHeader(
            RemoteOperation.OCS_API_HEADER,
            RemoteOperation.OCS_API_HEADER_VALUE
        )

        return try {
            val status = client.execute(method)
            status == HttpStatus.SC_OK || status == HttpStatus.SC_ACCEPTED
        } catch (e: IOException) {
            Log_OC.e(this, "Execution of notification action failed: $e")
            false
        } finally {
            method.releaseConnection()
        }
    }
}
