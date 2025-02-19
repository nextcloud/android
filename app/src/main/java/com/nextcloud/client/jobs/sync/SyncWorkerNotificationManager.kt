/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.sync

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ForegroundInfo
import com.nextcloud.utils.ForegroundServiceHelper
import com.owncloud.android.R
import com.owncloud.android.datamodel.ForegroundServiceType
import com.owncloud.android.ui.notifications.NotificationUtils
import kotlinx.coroutines.delay

class SyncWorkerNotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Suppress("MagicNumber")
    private val notificationId = 129

    private val channelId = NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD

    @Suppress("MagicNumber")
    private fun getNotification(title: String, description: String? = null, progress: Int? = null): Notification {
        return NotificationCompat.Builder(context, channelId).apply {
            setSmallIcon(android.R.drawable.stat_sys_download_done)
            setContentTitle(title)

            description?.let {
                setContentText(description)
            }

            progress?.let {
                setProgress(100, progress, false)

                addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    context.getString(R.string.common_cancel),
                    getCancelPendingIntent()
                )
            }

            setAutoCancel(true)
            setStyle(NotificationCompat.BigTextStyle())
            priority = NotificationCompat.PRIORITY_LOW

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setChannelId(NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD)
            }
        }.build()
    }

    private fun getCancelPendingIntent(): PendingIntent {
        val intent = Intent(context, SyncWorkerReceiver::class.java)

        return PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @Suppress("MagicNumber")
    fun showStartNotification() {
        val notification =
            getNotification(context.getString(R.string.sync_worker_start_notification_title), progress = 0)
        notificationManager.notify(notificationId, notification)
    }

    @Suppress("MagicNumber")
    fun showProgressNotification(filename: String, currentIndex: Int, totalFileSize: Int) {
        val currentFileIndex = (currentIndex + 1)
        val title = "$currentFileIndex / $totalFileSize - $filename"
        val progress = (currentFileIndex * 100) / totalFileSize
        val notification = getNotification(title, progress = progress)
        notificationManager.notify(notificationId, notification)
    }

    @Suppress("MagicNumber")
    suspend fun showCompletionMessage(success: Boolean) {
        if (success) {
            showNotification(
                R.string.sync_worker_success_notification_title,
                R.string.sync_worker_success_notification_description
            )
        } else {
            showNotification(
                R.string.sync_worker_error_notification_title,
                R.string.sync_worker_error_notification_description
            )
        }

        delay(1000)
        dismiss()
    }

    fun getForegroundInfo(): ForegroundInfo {
        return ForegroundServiceHelper.createWorkerForegroundInfo(
            notificationId,
            getNotification(context.getString(R.string.sync_worker_start_notification_title), progress = 0),
            ForegroundServiceType.DataSync
        )
    }

    @Suppress("MagicNumber")
    suspend fun showNotAvailableDiskSpace() {
        val notification =
            getNotification(context.getString(R.string.sync_worker_insufficient_disk_space_notification_title))
        notificationManager.notify(notificationId, notification)

        delay(1000)
        dismiss()
    }

    private fun showNotification(titleId: Int, descriptionId: Int) {
        val notification = getNotification(
            context.getString(titleId),
            context.getString(descriptionId)
        )

        notificationManager.notify(notificationId, notification)
    }

    fun dismiss() {
        notificationManager.cancel(notificationId)
    }
}
