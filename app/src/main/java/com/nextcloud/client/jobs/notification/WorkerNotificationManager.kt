/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.owncloud.android.R
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.theme.ViewThemeUtils

open class WorkerNotificationManager(
    private val id: Int,
    private val context: Context,
    viewThemeUtils: ViewThemeUtils,
    private val tickerId: Int
) {
    var currentOperationTitle: String? = null

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    var notificationBuilder: NotificationCompat.Builder =
        NotificationUtils.newNotificationBuilder(context, NotificationUtils.NOTIFICATION_CHANNEL_BACKGROUND_OPERATIONS, viewThemeUtils).apply {
            setTicker(context.getString(tickerId))
            setSmallIcon(R.drawable.notification_icon)
            setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.notification_icon))
            setStyle(NotificationCompat.BigTextStyle())
            priority = NotificationCompat.PRIORITY_LOW

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setChannelId(NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD)
            }
        }

    fun showNotification() {
        notificationManager.notify(id, notificationBuilder.build())
    }

    @Suppress("MagicNumber")
    fun setProgress(percent: Int, progressTextId: Int, indeterminate: Boolean) {
        val progressText = String.format(
            context.getString(progressTextId),
            percent
        )

        notificationBuilder.run {
            setProgress(100, percent, indeterminate)
            setContentTitle(currentOperationTitle)
            setContentText(progressText)
        }
    }

    fun dismissNotification(delay: Long = 0) {
        Handler(Looper.getMainLooper()).postDelayed({
            notificationManager.cancel(id)
        }, delay)
    }

    fun getId(): Int {
        return id
    }

    fun getNotification(): Notification {
        return notificationBuilder.build()
    }
}
