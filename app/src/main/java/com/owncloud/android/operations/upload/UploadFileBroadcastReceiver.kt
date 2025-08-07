/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.upload

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import com.owncloud.android.operations.UploadFileOperation

class UploadFileBroadcastReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_TYPE = "UploadFileBroadcastReceiver.ACTION_TYPE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val actionType =
            IntentCompat.getSerializableExtra(intent, ACTION_TYPE, UploadFileBroadcastReceiverActions::class.java)
                ?: return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(UploadFileOperation.MISSING_FILE_PERMISSION_NOTIFICATION_ID)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            actionType == UploadFileBroadcastReceiverActions.ALLOW_ALL_FILES
        ) {
            Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = "package:${context.packageName}".toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }.run {
                context.startActivity(this)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }.run {
                context.startActivity(this)
            }
        }
    }
}
