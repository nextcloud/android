/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.upload

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.UploadsStorageManager
import javax.inject.Inject

class FileUploadBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var uploadsStorageManager: UploadsStorageManager

    companion object {
        private const val UPLOAD_ID = "UPLOAD_ID"

        fun getBroadcast(context: Context, id: Long): PendingIntent {
            val intent = Intent(context, FileUploadBroadcastReceiver::class.java).apply {
                putExtra(UPLOAD_ID, id)
                setClass(context, FileUploadBroadcastReceiver::class.java)
                setPackage(context.packageName)
            }

            return PendingIntent.getBroadcast(
                context,
                id.toInt(),
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    @Suppress("ReturnCount")
    override fun onReceive(context: Context, intent: Intent) {
        MainApp.getAppComponent().inject(this)

        val uploadId = intent.getLongExtra(UPLOAD_ID, -1L)
        if (uploadId == -1L) {
            return
        }

        uploadsStorageManager.removeUpload(uploadId)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(uploadId.toInt())
    }
}
