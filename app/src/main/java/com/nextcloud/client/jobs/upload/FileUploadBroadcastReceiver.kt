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
import com.nextcloud.client.account.UserAccountManagerImpl
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.lib.common.utils.Log_OC
import javax.inject.Inject

class FileUploadBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var uploadsStorageManager: UploadsStorageManager

    companion object {
        private const val TAG = "FileUploadBroadcastReceiver"

        const val UPLOAD_ID = "UPLOAD_ID"
        const val ACCOUNT_NAME = "ACCOUNT_NAME"
        const val REMOTE_PATH = "REMOTE_PATH"
        const val REMOVE = "REMOVE"
    }

    @Suppress("ReturnCount")
    override fun onReceive(context: Context, intent: Intent) {
        MainApp.getAppComponent().inject(this)

        if (intent.action == UploadBroadcastAction.PauseAndCancel::class.simpleName) {
            pauseAndCancel(context, intent)
        }
    }

    @Suppress("ReturnCount")
    private fun pauseAndCancel(context: Context, intent: Intent) {
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

        Log_OC.d(TAG, "upload: $remotePath removed: $remove")

        FileUploadWorker.cancelUpload(remotePath, accountName)

        if (remove) {
            removeFileIfAlreadyUploaded(context, remotePath)
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

    @Suppress("DEPRECATION")
    private fun removeFileIfAlreadyUploaded(context: Context, remotePath: String) {
        val userAccountManager = UserAccountManagerImpl.fromContext(context)
        val user = userAccountManager.user
        val storageManager = FileDataStorageManager(user, context.contentResolver)
        val ocFile = storageManager.getFileByPath(remotePath) ?: return
        storageManager.removeFile(ocFile, true, false)
    }
}
