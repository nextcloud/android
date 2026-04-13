/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.manager

import android.app.Notification
import android.content.Context
import com.nextcloud.client.jobs.notification.WorkerNotificationManager
import com.owncloud.android.R
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.theme.ViewThemeUtils

class SynchronizeFileNotificationManager(
    private val context: Context,
    viewThemeUtils: ViewThemeUtils?
) : WorkerNotificationManager(
    id = NOTIFICATION_ID,
    context = context,
    viewThemeUtils = viewThemeUtils,
    tickerId = R.string.folder_download_worker_ticker_id,
    channelId = NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD
) {

    companion object {
        private const val NOTIFICATION_ID = 392
        private const val MAX_PROGRESS = 100
    }

    fun showProgress(fileName: String, progress: Int) {
        val description = context
            .getString(R.string.sync_file_notification_progress, progress)
        notificationManager.notify(NOTIFICATION_ID, buildNotification(fileName, description, progress))
    }

    fun showCompletion(folderName: String, success: Boolean) {
        val titleId = if (success) {
            R.string.folder_download_success_notification_title
        } else {
            R.string.folder_download_error_notification_title
        }

        val title = context.getString(titleId, folderName)
        notificationManager.notify(NOTIFICATION_ID, buildNotification(title))
    }

    fun dismiss() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun buildNotification(title: String, description: String = "", progress: Int? = null): Notification =
        notificationBuilder.apply {
            setSmallIcon(R.drawable.ic_sync)
            setContentTitle(title)
            setContentText(description)
            clearActions()
            setAutoCancel(true)
            if (progress != null) {
                setProgress(MAX_PROGRESS, progress, false)
            } else {
                setProgress(0, 0, false)
            }
        }.build()
}
