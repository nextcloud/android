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

class SyncWorkerNotificationManager(private val context: Context, private val notificationId: Int) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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
    fun showProgressNotification(folderName: String, filename: String, currentIndex: Int, totalFileSize: Int) {
        val currentFileIndex = (currentIndex + 1)
        val title = "$currentFileIndex / $totalFileSize - $filename"
        val progress = (currentFileIndex * 100) / totalFileSize
        val notification = getNotification(title = folderName, description = title, progress = progress)
        notificationManager.notify(notificationId, notification)
    }

    @Suppress("MagicNumber")
    suspend fun showCompletionMessage(folderName: String, success: Boolean) {
        val title = if (success) {
            context.getString(R.string.sync_worker_success_notification_title, folderName)
        } else {
            context.getString(R.string.sync_worker_error_notification_title, folderName)
        }

        val notification = getNotification(title = title)
        notificationManager.notify(notificationId, notification)

        delay(1000)
        dismiss()
    }

    fun getForegroundInfo(folderName: String): ForegroundInfo {
        return ForegroundServiceHelper.createWorkerForegroundInfo(
            notificationId,
            getNotification(folderName, progress = 0),
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

    fun dismiss() {
        notificationManager.cancel(notificationId)
    }
}
