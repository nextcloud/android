/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.upload

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.ui.notifications.NotificationUtils
import javax.inject.Inject

class FileUploadBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var uploadsStorageManager: UploadsStorageManager

    companion object {
        const val UPLOAD_ID = "UPLOAD_ID"
        const val REMOTE_PATH = "REMOTE_PATH"
        const val STORAGE_PATH = "STORAGE_PATH"
    }

    override fun onReceive(context: Context, intent: Intent) {
        MainApp.getAppComponent().inject(this)

        val remotePath = intent.getStringExtra(REMOTE_PATH) ?: return
        val storagePath = intent.getStringExtra(STORAGE_PATH) ?: return
        val uploadId = intent.getLongExtra(UPLOAD_ID, -1L)
        if (uploadId == -1L) {
            return
        }

        uploadsStorageManager.removeUpload(uploadId)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(
            NotificationUtils.createUploadNotificationTag(remotePath , storagePath),
            FileUploadWorker.NOTIFICATION_ERROR_ID
        )
    }
}
