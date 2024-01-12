/*
 * Nextcloud Android client application
 *
 * @author Alper Ozturk
 * Copyright (C) 2023 Alper Ozturk
 * Copyright (C) 2023 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.nextcloud.client.jobs.upload

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import com.owncloud.android.R
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.FileUtils
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.theme.ViewThemeUtils

class UploadNotificationManager(private val context: Context, private val viewThemeUtils: ViewThemeUtils) {

    private val id = 411

    private var notification: Notification? = null
    private var notificationBuilder: NotificationCompat.Builder
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        notificationBuilder = NotificationUtils.newNotificationBuilder(context, viewThemeUtils).apply {
            setContentTitle(context.getString(R.string.uploader_upload_in_progress_ticker))
            setSmallIcon(R.drawable.notification_icon)
            setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.notification_icon))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setChannelId(NotificationUtils.NOTIFICATION_CHANNEL_UPLOAD)
            }
        }

        notification = notificationBuilder.build()
    }

    @Suppress("MagicNumber")
    fun prepareForStart(upload: UploadFileOperation, pendingIntent: PendingIntent, startIntent: PendingIntent) {
        notificationBuilder = NotificationUtils.newNotificationBuilder(context, viewThemeUtils).apply {
            setSmallIcon(R.drawable.notification_icon)
            setOngoing(true)
            setTicker(context.getString(R.string.uploader_upload_in_progress_ticker))
            setContentTitle(context.getString(R.string.uploader_upload_in_progress_ticker))
            setProgress(100, 0, false)
            setContentText(
                String.format(
                    context.getString(R.string.uploader_upload_in_progress),
                    0,
                    upload.fileName
                )
            )
            clearActions()

            addAction(
                R.drawable.ic_action_cancel_grey,
                context.getString(R.string.common_cancel),
                pendingIntent
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setChannelId(NotificationUtils.NOTIFICATION_CHANNEL_UPLOAD)
            }

            setContentIntent(startIntent)
        }

        if (!upload.isInstantPicture && !upload.isInstantVideo) {
            showNotification()
        }
    }

    fun notifyForResult(
        resultCode: RemoteOperationResult.ResultCode,
        resultIntent: PendingIntent,
        credentialIntent: PendingIntent?,
        errorMessage: String?
    ) {
        val textId = resultText(resultCode)

        notificationBuilder.run {
            setTicker(context.getString(textId))
            setContentTitle(context.getString(textId))
            setAutoCancel(true)
            setOngoing(false)
            setProgress(0, 0, false)
            clearActions()
            setContentIntent(resultIntent)

            credentialIntent?.let {
                setContentIntent(it)
            }

            errorMessage?.let {
                setContentText(it)
            }
        }
    }

    private fun resultText(resultCode: RemoteOperationResult.ResultCode): Int {
        val needsToUpdateCredentials = (resultCode == RemoteOperationResult.ResultCode.UNAUTHORIZED)

        return if (needsToUpdateCredentials) {
            R.string.uploader_upload_failed_credentials_error
        } else if (resultCode == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
            R.string.uploader_upload_failed_sync_conflict_error
        } else {
            R.string.uploader_upload_failed_ticker
        }
    }

    fun addAction(icon: Int, textId: Int, intent: PendingIntent) {
        notificationBuilder.addAction(
            icon,
            context.getString(textId),
            intent
        )
    }

    fun showNewNotification(operation: UploadFileOperation) {
        notificationManager.notify(
            NotificationUtils.createUploadNotificationTag(operation.file),
            FileUploadWorker.NOTIFICATION_ERROR_ID,
            notificationBuilder.build()
        )
    }

    fun showNotification() {
        notificationManager.notify(id, notificationBuilder.build())
    }

    @Suppress("MagicNumber")
    fun updateUploadProgress(filePath: String, percent: Int, currentOperation: UploadFileOperation?) {
        notificationBuilder.run {
            setProgress(100, percent, false)
            val fileName = filePath.substring(filePath.lastIndexOf(FileUtils.PATH_SEPARATOR) + 1)
            val text = String.format(context.getString(R.string.uploader_upload_in_progress), percent, fileName)
            setContentText(text)

            showNotification()
            dismissOldErrorNotification(currentOperation)
        }
    }

    fun dismissOldErrorNotification(operation: UploadFileOperation?) {
        if (operation == null) {
            return
        }

        notificationManager.cancel(
            NotificationUtils.createUploadNotificationTag(operation.file),
            FileUploadWorker.NOTIFICATION_ERROR_ID
        )

        operation.oldFile?.let {
            notificationManager.cancel(
                NotificationUtils.createUploadNotificationTag(it),
                FileUploadWorker.NOTIFICATION_ERROR_ID
            )
        }
    }

    fun dismissWorkerNotifications() {
        notificationManager.cancel(id)
    }
}
