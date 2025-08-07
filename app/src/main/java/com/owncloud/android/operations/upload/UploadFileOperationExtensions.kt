/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.upload

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.owncloud.android.R
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.operations.UploadFileOperation.MISSING_FILE_PERMISSION_NOTIFICATION_ID

fun UploadFileOperation.showStoragePermissionNotification() {
    val intent = Intent(context, UploadFileBroadcastReceiver::class.java)

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val action = NotificationCompat.Action(
        null,
        context.getString(R.string.common_ok),
        pendingIntent
    )

    val notificationBuilder =
        NotificationCompat.Builder(context, context.getString(R.string.notification_channel_upload_name_short))
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(context.getString(R.string.upload_missing_storage_permission_title))
            .setContentText(context.getString(R.string.upload_missing_storage_permission_description))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(action)
            .setAutoCancel(true)

    ContextCompat.getSystemService(context, NotificationManager::class.java)?.run {
        notify(MISSING_FILE_PERMISSION_NOTIFICATION_ID, notificationBuilder.build())
    }
}