/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.autoUpload

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.nextcloud.client.jobs.notification.WorkerNotificationManager
import com.owncloud.android.R
import com.owncloud.android.utils.theme.ViewThemeUtils

class AutoUploadNotificationManager(private val context: Context, viewThemeUtils: ViewThemeUtils, id: Int) :
    WorkerNotificationManager(id, context, viewThemeUtils, R.string.auto_upload_ticker_id) {

    @RequiresApi(Build.VERSION_CODES.R)
    fun showStoragePermissionNotification() {
        val intent = Intent(context, AutoUploadBroadcastReceiver::class.java)

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

        notificationBuilder.run {
            setSmallIcon(android.R.drawable.stat_sys_warning)
            setContentTitle(context.getString(R.string.auto_upload_missing_storage_permission_title))
            setContentText(context.getString(R.string.auto_upload_missing_storage_permission_description))
            addAction(action)
        }

        showNotification()
    }
}
