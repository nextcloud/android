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
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.owncloud.android.R
import com.owncloud.android.utils.theme.ViewThemeUtils

open class WorkerNotificationManager(
    private val id: Int,
    private val context: Context,
    viewThemeUtils: ViewThemeUtils,
    private val tickerId: Int,
    channelId: String
) {
    var currentOperationTitle: String? = null

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    var notificationBuilder: NotificationCompat.Builder =
        NotificationCompat.Builder(context, channelId).apply {
            setTicker(context.getString(tickerId))
            setSmallIcon(R.drawable.notification_icon)
            setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.notification_icon))
            setStyle(NotificationCompat.BigTextStyle())
            priority = NotificationCompat.PRIORITY_LOW
            setSound(null)
            setVibrate(null)
            setOnlyAlertOnce(true)
            setSilent(true)
            viewThemeUtils.androidx.themeNotificationCompatBuilder(context, this)
        }

    fun showNotification() {
        notificationManager.notify(id, notificationBuilder.build())
    }

    fun showNotification(notification: Notification) {
        notificationManager.notify(id, notification)
    }

    @Suppress("MagicNumber")
    fun setProgress(percent: Int, progressText: String?, indeterminate: Boolean) {
        notificationBuilder.run {
            setProgress(100, percent, indeterminate)
            setContentTitle(currentOperationTitle)

            progressText?.let {
                setContentText(progressText)
            }
        }
    }

    fun dismissNotification(delay: Long = 0) {
        Handler(Looper.getMainLooper()).postDelayed({
            notificationManager.cancel(id)
        }, delay)
    }

    fun getId(): Int = id

    fun getNotification(): Notification = notificationBuilder.build()
}
