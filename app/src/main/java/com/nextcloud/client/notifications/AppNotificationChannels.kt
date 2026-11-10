/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.notifications.NotificationUtils

object AppNotificationChannels {

    private val TAG: String = AppNotificationChannels::class.java.simpleName

    private val channels = listOf(
        NotificationChannelSpec(
            NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD,
            R.string.notification_channel_download_name_short,
            R.string.notification_channel_download_description
        ),
        NotificationChannelSpec(
            NotificationUtils.NOTIFICATION_CHANNEL_UPLOAD,
            R.string.notification_channel_upload_name_short,
            R.string.notification_channel_upload_description
        ),
        NotificationChannelSpec(
            NotificationUtils.NOTIFICATION_CHANNEL_MEDIA,
            R.string.notification_channel_media_name,
            R.string.notification_channel_media_description
        ),
        NotificationChannelSpec(
            NotificationUtils.NOTIFICATION_CHANNEL_FILE_SYNC,
            R.string.notification_channel_file_sync_name,
            R.string.notification_channel_file_sync_description
        ),
        NotificationChannelSpec(
            NotificationUtils.NOTIFICATION_CHANNEL_PUSH,
            R.string.notification_channel_push_name,
            R.string.notification_channel_push_description,
            NotificationManager.IMPORTANCE_DEFAULT
        ),
        NotificationChannelSpec(
            NotificationUtils.NOTIFICATION_CHANNEL_BACKGROUND_OPERATIONS,
            R.string.notification_channel_background_operations_name,
            R.string.notification_channel_background_operations_description
        ),
        NotificationChannelSpec(
            NotificationUtils.NOTIFICATION_CHANNEL_GENERAL,
            R.string.notification_channel_general_name,
            R.string.notification_channel_general_description,
            NotificationManager.IMPORTANCE_DEFAULT
        ),
        NotificationChannelSpec(
            NotificationUtils.NOTIFICATION_CHANNEL_OFFLINE_OPERATIONS,
            R.string.notification_channel_offline_operations_name_short,
            R.string.notification_channel_offline_operations_description
        ),
        NotificationChannelSpec(
            NotificationUtils.NOTIFICATION_CHANNEL_CONTENT_OBSERVER,
            R.string.notification_channel_content_observer_name_short,
            R.string.notification_channel_content_observer_description
        )
    )

    fun create(context: Context) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        if (notificationManager == null) {
            Log_OC.e(TAG, "Notification manager is null")
            return
        }

        channels.forEach { notificationManager.createNotificationChannel(it.toNotificationChannel(context)) }
        notificationManager.deleteNotificationChannel(NotificationUtils.NOTIFICATION_CHANNEL_FILE_OBSERVER)
    }

    private fun NotificationChannelSpec.toNotificationChannel(context: Context): NotificationChannel =
        NotificationChannel(id, context.getString(nameRes), importance).apply {
            description = context.getString(descriptionRes)
            enableLights(false)
            enableVibration(false)
        }
}
