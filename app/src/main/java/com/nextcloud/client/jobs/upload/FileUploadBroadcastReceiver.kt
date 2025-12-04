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
import javax.inject.Inject

class FileUploadBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var uploadsStorageManager: UploadsStorageManager

    companion object {
        // region cancel or remove actions
        const val UPLOAD_ID = "UPLOAD_ID"
        const val ACCOUNT_NAME = "ACCOUNT_NAME"
        const val REMOTE_PATH = "REMOTE_PATH"
        const val REMOVE = "REMOVE"
        // endregion
    }

    @Suppress("ReturnCount")
    override fun onReceive(context: Context, intent: Intent) {
        MainApp.getAppComponent().inject(this)

        if (intent.action == UploadBroadcastAction.StopOrRemove::class.simpleName) {
            stopOrRemoveUpload(context, intent)
        }
    }

    @Suppress("ReturnCount")
    private fun stopOrRemoveUpload(context: Context, intent: Intent) {
        val uploadId = intent.getLongExtra(UPLOAD_ID, -1L)
        if (uploadId == -1L) {
            return
        }

        val accountName = intent.getStringExtra(ACCOUNT_NAME)
        if (accountName.isNullOrEmpty()) {
            return
        }

        val remotePath = intent.getStringExtra(REMOTE_PATH)
        if (remotePath.isNullOrEmpty()) {
            return
        }

        val remove = intent.getBooleanExtra(REMOVE, false)

        FileUploadWorker.cancelCurrentUpload(remotePath, accountName, onCompleted = {})

        if (remove) {
            uploadsStorageManager.removeUpload(uploadId)
        } else {
            FileUploadHelper.instance().updateUploadStatus(
                remotePath,
                accountName,
                UploadsStorageManager.UploadStatus.UPLOAD_CANCELLED
            )
        }

        // dismiss notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(uploadId.toInt())
    }
}
