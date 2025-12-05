/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.upload

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.owncloud.android.R
import com.owncloud.android.operations.UploadFileOperation

sealed class UploadBroadcastAction {
    data class PauseAndCancel(val operation: UploadFileOperation) : UploadBroadcastAction() {

        /**
         * Updates upload status to CANCELLED
         */
        fun pauseAction(context: Context): NotificationCompat.Action = NotificationCompat.Action(
            R.drawable.ic_cancel,
            context.getString(R.string.pause_upload),
            getBroadcast(context, false)
        )

        /**
         * Removes the upload completely
         */
        fun cancelAction(context: Context): NotificationCompat.Action = NotificationCompat.Action(
            R.drawable.ic_delete,
            context.getString(R.string.cancel_upload),
            getBroadcast(context, true)
        )

        @Suppress("MagicNumber")
        private fun getBroadcast(context: Context, remove: Boolean): PendingIntent {
            val intent = Intent(context, FileUploadBroadcastReceiver::class.java).apply {
                putExtra(FileUploadBroadcastReceiver.UPLOAD_ID, operation.ocUploadId)
                putExtra(FileUploadBroadcastReceiver.ACCOUNT_NAME, operation.user.accountName)
                putExtra(FileUploadBroadcastReceiver.REMOTE_PATH, operation.remotePath)
                putExtra(FileUploadBroadcastReceiver.REMOVE, remove)
                action = PauseAndCancel::class.simpleName

                setClass(context, FileUploadBroadcastReceiver::class.java)
                setPackage(context.packageName)
            }

            val requestCode = if (remove) operation.ocUploadId.toInt() + 1000 else operation.ocUploadId.toInt()

            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
