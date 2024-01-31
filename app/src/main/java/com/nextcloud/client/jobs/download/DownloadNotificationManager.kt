/*
 * Nextcloud Android client application
 *
 * @author Alper Ozturk
 * Copyright (C) 2023 Alper Ozturk
 * Copyright (C) 2023 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.nextcloud.client.jobs.download

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.owncloud.android.R
import com.owncloud.android.lib.resources.files.FileUtils
import com.owncloud.android.operations.DownloadFileOperation
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.io.File
import java.security.SecureRandom

@Suppress("TooManyFunctions")
class DownloadNotificationManager(
    private val id: Int,
    private val context: Context,
    private val viewThemeUtils: ViewThemeUtils
) {
    private var notification: Notification
    private var notificationBuilder: NotificationCompat.Builder
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        notificationBuilder = NotificationUtils.newNotificationBuilder(context, viewThemeUtils).apply {
            setContentTitle(context.getString(R.string.downloader_download_in_progress_ticker))
            setTicker(context.getString(R.string.downloader_download_in_progress_ticker))
            setSmallIcon(R.drawable.notification_icon)
            setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.notification_icon))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setChannelId(NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD)
            }
        }

        notification = notificationBuilder.build()
    }

    @Suppress("MagicNumber")
    fun prepareForStart(operation: DownloadFileOperation) {
        notificationBuilder = NotificationUtils.newNotificationBuilder(context, viewThemeUtils).apply {
            setSmallIcon(R.drawable.notification_icon)
            setOngoing(true)
            setProgress(100, 0, operation.size < 0)
            setContentText(
                String.format(
                    context.getString(R.string.downloader_download_in_progress), 0,
                    File(operation.savePath).name
                )
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setChannelId(NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD)
            }

            notificationManager.notify(
                id,
                this.build()
            )
        }
    }

    fun prepareForResult() {
        notificationBuilder
            .setAutoCancel(true)
            .setOngoing(false)
            .setProgress(0, 0, false)
    }

    @Suppress("MagicNumber")
    fun updateDownloadProgress(filePath: String, percent: Int, totalToTransfer: Long) {
        notificationBuilder.run {
            setProgress(100, percent, totalToTransfer < 0)
            val fileName: String = filePath.substring(filePath.lastIndexOf(FileUtils.PATH_SEPARATOR) + 1)
            val text =
                String.format(context.getString(R.string.downloader_download_in_progress), percent, fileName)
            val title =
                context.getString(R.string.downloader_download_in_progress_ticker)
            updateNotificationText(title, text)
        }
    }

    @Suppress("MagicNumber")
    fun dismissNotification() {
        Handler(Looper.getMainLooper()).postDelayed({
            notificationManager.cancel(id)
        }, 2000)
    }

    fun showNewNotification(text: String) {
        val notifyId = SecureRandom().nextInt()

        notificationBuilder.run {
            setProgress(0, 0, false)
            setContentTitle(null)
            setContentText(text)
            setOngoing(false)
            notificationManager.notify(notifyId, this.build())
        }
    }

    private fun updateNotificationText(title: String?, text: String) {
        notificationBuilder.run {
            title?.let {
                setContentTitle(title)
            }

            setContentText(text)
            notificationManager.notify(id, this.build())
        }
    }

    fun setContentIntent(intent: Intent, flag: Int) {
        notificationBuilder.setContentIntent(
            PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                flag
            )
        )
    }

    fun getId(): Int {
        return id
    }

    fun getNotification(): Notification {
        return notificationBuilder.build()
    }
}
