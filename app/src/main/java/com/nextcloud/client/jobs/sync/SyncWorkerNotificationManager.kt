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
import com.owncloud.android.ui.notifications.NotificationUtils

class SyncWorkerNotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notificationId = 129
    private val channelId = NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD

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
                    "Cancel",
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

    fun showStartNotification() {
        val notification = getNotification("Sync Operation Started", progress = 0)
        notificationManager.notify(notificationId, notification)
    }

    fun showProgressNotification(filename: String, currentIndex: Int, totalFileSize: Int) {
        val currentFileIndex = (currentIndex + 1)
        val title = "$currentFileIndex / $totalFileSize - $filename"
        val progress = (currentFileIndex * 100) / totalFileSize
        val notification = getNotification(title, progress = progress)
        notificationManager.notify(notificationId, notification)
    }

    fun showSuccessNotification() {
        val notification = getNotification("Download Complete", "File downloaded successfully")
        notificationManager.notify(notificationId, notification)
    }

    fun showErrorNotification() {
        val notification = getNotification("Download Failed", "Error downloading file")
        notificationManager.notify(notificationId, notification)
    }

    fun dismiss() {
        notificationManager.cancel(notificationId)
    }
}
