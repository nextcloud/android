/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs.upload

import android.app.PendingIntent
import android.content.Context
import com.nextcloud.client.jobs.notification.WorkerNotificationManager
import com.nextcloud.utils.extensions.isFileSpecificError
import com.nextcloud.utils.numberFormatter.NumberFormatter
import com.owncloud.android.R
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.operations.upload.buildFailedResultNotification
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.theme.ViewThemeUtils

class UploadNotificationManager(private val context: Context, viewThemeUtils: ViewThemeUtils, id: Int) :
    WorkerNotificationManager(
        id,
        context,
        viewThemeUtils,
        tickerId = R.string.foreground_service_upload,
        channelId = NotificationUtils.NOTIFICATION_CHANNEL_UPLOAD
    ) {

    @Suppress("MagicNumber")
    fun prepareForStart(
        uploadFileOperation: UploadFileOperation,
        cancelPendingIntent: PendingIntent,
        startIntent: PendingIntent,
        currentUploadIndex: Int,
        totalUploadSize: Int
    ) {
        currentOperationTitle = if (totalUploadSize > 1) {
            String.format(
                context.getString(R.string.upload_notification_manager_start_text),
                currentUploadIndex,
                totalUploadSize,
                uploadFileOperation.fileName
            )
        } else {
            uploadFileOperation.fileName
        }

        val progressText = NumberFormatter.getPercentageText(0)

        notificationBuilder.run {
            setProgress(100, 0, false)
            setContentTitle(currentOperationTitle)
            setContentText(progressText)
            setOngoing(false)
            clearActions()

            addAction(
                R.drawable.ic_action_cancel_grey,
                context.getString(R.string.common_cancel),
                cancelPendingIntent
            )

            setContentIntent(startIntent)
        }

        if (!uploadFileOperation.isInstantPicture && !uploadFileOperation.isInstantVideo) {
            showNotification()
        }
    }

    @Suppress("MagicNumber")
    fun updateUploadProgress(percent: Int, currentOperation: UploadFileOperation?) {
        val progressText = NumberFormatter.getPercentageText(percent)
        setProgress(percent, progressText, false)
        showNotification()
        dismissOldErrorNotification(currentOperation)
    }

    fun notifyForFailedResult(uploadFileOperation: UploadFileOperation, resultCode: ResultCode, errorMessage: String) {
        if (uploadFileOperation.isMissingPermissionThrown) {
            return
        }

        uploadFileOperation.buildFailedResultNotification(notificationBuilder, resultCode, errorMessage)

        if (resultCode.isFileSpecificError()) {
            showNewNotification(uploadFileOperation)
        } else {
            showNotification()
        }
    }

    fun addAction(icon: Int, textId: Int, intent: PendingIntent) {
        notificationBuilder.addAction(
            icon,
            context.getString(textId),
            intent
        )
    }

    private fun showNewNotification(operation: UploadFileOperation) {
        notificationManager.notify(
            NotificationUtils.createUploadNotificationTag(operation.file),
            FileUploadWorker.NOTIFICATION_ERROR_ID,
            notificationBuilder.build()
        )
    }

    fun showSameFileAlreadyExistsNotification(filename: String) {
        notificationBuilder.run {
            setAutoCancel(true)
            clearActions()
            setContentText("")
            setProgress(0, 0, false)
            setContentTitle(context.getString(R.string.file_upload_worker_same_file_already_exists, filename))
        }

        val notificationId = filename.hashCode()

        notificationManager.notify(
            notificationId,
            notificationBuilder.build()
        )
    }

    fun showConnectionErrorNotification() {
        notificationManager.cancel(getId())

        notificationBuilder.run {
            setContentTitle(context.getString(R.string.file_upload_worker_error_notification_title))
            setContentText("")
        }

        notificationManager.notify(
            FileUploadWorker.NOTIFICATION_ERROR_ID,
            notificationBuilder.build()
        )
    }

    fun dismissOldErrorNotification(operation: UploadFileOperation?) {
        if (operation == null) {
            return
        }

        dismissOldErrorNotification(operation.file.remotePath, operation.file.storagePath)

        operation.oldFile?.let {
            dismissOldErrorNotification(it.remotePath, it.storagePath)
        }
    }

    fun dismissErrorNotification() = notificationManager.cancel(FileUploadWorker.NOTIFICATION_ERROR_ID)

    fun dismissOldErrorNotification(remotePath: String, localPath: String) {
        notificationManager.cancel(
            NotificationUtils.createUploadNotificationTag(remotePath, localPath),
            FileUploadWorker.NOTIFICATION_ERROR_ID
        )
    }

    fun notifyPaused(intent: PendingIntent) {
        notificationBuilder.run {
            setContentTitle(context.getString(R.string.upload_global_pause_title))
            setTicker(context.getString(R.string.upload_global_pause_title))
            setOngoing(false)
            setAutoCancel(false)
            setProgress(0, 0, false)
            clearActions()
            setContentIntent(intent)
        }

        showNotification()
    }
}
