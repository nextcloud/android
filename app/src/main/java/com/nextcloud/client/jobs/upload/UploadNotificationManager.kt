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
import com.owncloud.android.R
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.theme.ViewThemeUtils

class UploadNotificationManager(private val context: Context, viewThemeUtils: ViewThemeUtils) :
    WorkerNotificationManager(ID, context, viewThemeUtils, R.string.foreground_service_upload) {

    companion object {
        private const val ID = 411
    }

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

        val progressText = String.format(
            context.getString(R.string.upload_notification_manager_upload_in_progress_text),
            0
        )

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
        setProgress(percent, R.string.upload_notification_manager_upload_in_progress_text, false)
        showNotification()
        dismissOldErrorNotification(currentOperation)
    }

    fun notifyForFailedResult(
        uploadFileOperation: UploadFileOperation,
        resultCode: RemoteOperationResult.ResultCode,
        conflictsResolveIntent: PendingIntent?,
        credentialIntent: PendingIntent?,
        errorMessage: String
    ) {
        val textId = getFailedResultTitleId(resultCode)

        notificationBuilder.run {
            setTicker(context.getString(textId))
            setContentTitle(context.getString(textId))
            setAutoCancel(false)
            setOngoing(false)
            setProgress(0, 0, false)
            clearActions()

            conflictsResolveIntent?.let {
                addAction(
                    R.drawable.ic_cloud_upload,
                    R.string.upload_list_resolve_conflict,
                    it
                )
            }

            credentialIntent?.let {
                setContentIntent(it)
            }

            setContentText(errorMessage)
        }

        showNewNotification(uploadFileOperation)
    }

    private fun getFailedResultTitleId(resultCode: RemoteOperationResult.ResultCode): Int {
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

    private fun showNewNotification(operation: UploadFileOperation) {
        notificationManager.notify(
            NotificationUtils.createUploadNotificationTag(operation.file),
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
