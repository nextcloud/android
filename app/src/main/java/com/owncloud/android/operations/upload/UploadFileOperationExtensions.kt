/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.upload

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.owncloud.android.R
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.operations.UploadFileOperation.MISSING_FILE_PERMISSION_NOTIFICATION_ID
import com.owncloud.android.ui.notifications.NotificationUtils

fun UploadFileOperation.showStoragePermissionNotification() {
    val allowAllFileAccessAction = getAllowAllFileAccessAction(context)
    val appPermissionsAction = getAppPermissionsAction(context)

    val notificationBuilder =
        NotificationCompat.Builder(context, NotificationUtils.NOTIFICATION_CHANNEL_UPLOAD)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(context.getString(R.string.upload_missing_storage_permission_title))
            .setContentText(context.getString(R.string.upload_missing_storage_permission_description))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(allowAllFileAccessAction)
            .addAction(appPermissionsAction)
            .setAutoCancel(true)

    ContextCompat.getSystemService(context, NotificationManager::class.java)?.run {
        notify(MISSING_FILE_PERMISSION_NOTIFICATION_ID, notificationBuilder.build())
    }
}

private fun getActionPendingIntent(context: Context, actionType: UploadFileBroadcastReceiverActions): PendingIntent {
    val intent = Intent(context, UploadFileBroadcastReceiver::class.java).apply {
        action = "com.owncloud.android.ACTION_UPLOAD_FILE_PERMISSION"
        putExtra(UploadFileBroadcastReceiver.ACTION_TYPE, actionType)
    }

    return PendingIntent.getBroadcast(
        context,
        actionType.ordinal,
        intent,
        PendingIntent.FLAG_IMMUTABLE
    )
}

private fun getAllowAllFileAccessAction(context: Context): NotificationCompat.Action {
    val pendingIntent = getActionPendingIntent(context, UploadFileBroadcastReceiverActions.ALLOW_ALL_FILES)
    return NotificationCompat.Action(
        null,
        context.getString(R.string.upload_missing_storage_permission_allow_file_access),
        pendingIntent
    )
}

private fun getAppPermissionsAction(context: Context): NotificationCompat.Action {
    val pendingIntent = getActionPendingIntent(context, UploadFileBroadcastReceiverActions.APP_PERMISSIONS)
    return NotificationCompat.Action(
        null,
        context.getString(R.string.upload_missing_storage_permission_app_permissions),
        pendingIntent
    )
}
