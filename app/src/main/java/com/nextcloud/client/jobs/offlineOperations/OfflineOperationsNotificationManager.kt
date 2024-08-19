/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.offlineOperations

import android.content.Context
import com.nextcloud.client.jobs.notification.WorkerNotificationManager
import com.owncloud.android.R
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.utils.ErrorMessageAdapter
import com.owncloud.android.utils.theme.ViewThemeUtils

class OfflineOperationsNotificationManager(private val context: Context, viewThemeUtils: ViewThemeUtils) :
    WorkerNotificationManager(
        ID,
        context,
        viewThemeUtils,
        R.string.offline_operations_worker_notification_manager_ticker
    ) {

    companion object {
        private const val ID = 121
        private const val ERROR_ID = 122
    }

    @Suppress("MagicNumber")
    fun start() {
        notificationBuilder.run {
            setContentTitle(context.getString(R.string.offline_operations_worker_notification_start_text))
            setProgress(100, 0, false)
        }

        showNotification()
    }

    @Suppress("MagicNumber")
    fun update(totalOperationSize: Int, currentOperationIndex: Int, filename: String) {
        val title = if (totalOperationSize > 1) {
            String.format(
                context.getString(R.string.offline_operations_worker_progress_text),
                currentOperationIndex,
                totalOperationSize,
                filename
            )
        } else {
            filename
        }

        val progress = (currentOperationIndex * 100) / totalOperationSize

        notificationBuilder.run {
            setContentTitle(title)
            setProgress(100, progress, false)
        }

        showNotification()
    }

    fun showNewNotification(result: RemoteOperationResult<*>, operation: RemoteOperation<*>) {
        val reason = ErrorMessageAdapter.getErrorCauseMessage(result, operation, context.resources)
        val text = context.getString(R.string.offline_operations_worker_notification_error_text, reason)

        notificationBuilder.run {
            setContentTitle(text)
            setOngoing(false)
            notificationManager.notify(ERROR_ID, this.build())
        }
    }
}
