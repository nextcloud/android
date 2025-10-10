/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.sync

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
import kotlinx.coroutines.delay
import kotlin.random.Random

class SyncWorkerNotificationManager(private val context: Context, viewThemeUtils: ViewThemeUtils) :
    WorkerNotificationManager(
        id = NOTIFICATION_ID,
        context,
        viewThemeUtils,
        tickerId = R.string.sync_worker_ticker_id,
        channelId = NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD
    ) {

    companion object {
        private const val NOTIFICATION_ID = 391
        private const val MAX_PROGRESS = 100
        private const val DELAY = 1000L
    }

    private fun getNotification(title: String, description: String? = null, progress: Int? = null): Notification =
        notificationBuilder.apply {
            setSmallIcon(R.drawable.ic_sync)
            setContentTitle(title)
            clearActions()

            description?.let {
                setContentText(description)
            }

            progress?.let {
                setProgress(MAX_PROGRESS, progress, false)
                addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    context.getString(R.string.common_cancel),
                    getCancelPendingIntent()
                )
            }

            setAutoCancel(true)
        }.build()

    private fun getCancelPendingIntent(): PendingIntent {
        val intent = Intent(context, SyncWorkerReceiver::class.java)

        return PendingIntent.getBroadcast(
            context,
            Random.nextInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showProgressNotification(folderName: String, filename: String, currentIndex: Int, totalFileSize: Int) {
        val currentFileIndex = (currentIndex + 1)
        val description = context.getString(R.string.sync_worker_counter, currentFileIndex, totalFileSize, filename)
        val progress = (currentFileIndex * MAX_PROGRESS) / totalFileSize
        val notification = getNotification(title = folderName, description = description, progress = progress)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    suspend fun showCompletionMessage(folderName: String, success: Boolean) {
        val title = if (success) {
            context.getString(R.string.sync_worker_success_notification_title, folderName)
        } else {
            context.getString(R.string.sync_worker_error_notification_title, folderName)
        }

        val notification = getNotification(title = title)
        notificationManager.notify(NOTIFICATION_ID, notification)

        delay(DELAY)
        dismiss()
    }

    fun getForegroundInfo(folder: OCFile): ForegroundInfo = ForegroundServiceHelper.createWorkerForegroundInfo(
        NOTIFICATION_ID,
        getNotification(folder.fileName, progress = 0),
        ForegroundServiceType.DataSync
    )

    suspend fun showNotAvailableDiskSpace() {
        val title = context.getString(R.string.sync_worker_insufficient_disk_space_notification_title)
        val notification = getNotification(title)
        notificationManager.notify(NOTIFICATION_ID, notification)

        delay(DELAY)
        dismiss()
    }

    fun dismiss() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
