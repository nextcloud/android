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
    }

    fun start(totalOperationSize: Int, currentOperationIndex: Int, filename: String) {
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

        notificationBuilder.run {
            setContentTitle(title)
            setTicker(title)
            setProgress(100, 0, false)
        }

        showNotification()
    }

    fun update(totalOperationSize: Int, currentOperationIndex: Int) {
        val percent = (currentOperationIndex * 100) / totalOperationSize
        notificationBuilder.run {
            setProgress(100, percent, false)
        }
        showNotification()
    }
}
