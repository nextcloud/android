/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.folderDownload

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.ForegroundInfo
import com.nextcloud.client.jobs.notification.WorkerNotificationManager
import com.nextcloud.utils.ForegroundServiceHelper
import com.owncloud.android.R
import com.owncloud.android.datamodel.ForegroundServiceType
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlin.random.Random

class FolderDownloadWorkerNotificationManager(private val context: Context, viewThemeUtils: ViewThemeUtils) :
    WorkerNotificationManager(
        id = NOTIFICATION_ID,
        context,
        viewThemeUtils,
        tickerId = R.string.folder_download_worker_ticker_id,
        channelId = NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD
    ) {

    companion object {
        private const val NOTIFICATION_ID = 391
        private const val MAX_PROGRESS = 100
    }

    private fun getNotification(title: String, description: String = "", progress: Int? = null): Notification =
        notificationBuilder.apply {
            setSmallIcon(R.drawable.ic_sync)
            setContentTitle(title)
            clearActions()
            setContentText(description)

            if (progress != null) {
                setProgress(MAX_PROGRESS, progress, false)
                addAction(
                    R.drawable.ic_cancel,
                    context.getString(R.string.common_cancel),
                    getCancelPendingIntent()
                )
            } else {
                setProgress(0, 0, false)
            }

            setAutoCancel(true)
        }.build()

    private fun getCancelPendingIntent(): PendingIntent {
        val intent = Intent(context, FolderDownloadWorkerReceiver::class.java)

        return PendingIntent.getBroadcast(
            context,
            Random.nextInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @Suppress("MagicNumber")
    fun getProgressNotification(
        folderName: String,
        filename: String,
        currentIndex: Int,
        totalFileSize: Int
    ): Notification {
        val currentFileIndex = (currentIndex + 1)
        val description = context.getString(R.string.folder_download_counter, currentFileIndex, totalFileSize, filename)
        val progress = (currentFileIndex * MAX_PROGRESS) / totalFileSize
        return getNotification(folderName, description, progress)
    }

    fun showCompletionNotification(folderName: String, success: Boolean) {
        val titleId = if (success) {
            R.string.folder_download_success_notification_title
        } else {
            R.string.folder_download_error_notification_title
        }

        val title = context.getString(titleId, folderName)

        val notification = getNotification(title)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun showNotAvailableDiskSpace() {
        val title = context.getString(R.string.folder_download_insufficient_disk_space_notification_title)
        val notification = getNotification(title)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun getForegroundInfo(folder: OCFile?): ForegroundInfo {
        val notification = if (folder != null) {
            getNotification(folder.fileName)
        } else {
            getNotification(title = context.getString(R.string.folder_download_worker_ticker_id))
        }

        return getForegroundInfo(notification)
    }

    fun getForegroundInfo(notification: Notification): ForegroundInfo =
        ForegroundServiceHelper.createWorkerForegroundInfo(
            NOTIFICATION_ID,
            notification,
            ForegroundServiceType.DataSync
        )

    fun dismiss() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
