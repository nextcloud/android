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
import androidx.core.app.NotificationCompat
import com.nextcloud.client.jobs.notification.WorkerNotificationManager
import com.nextcloud.utils.numberFormatter.NumberFormatter
import com.owncloud.android.R
import com.owncloud.android.operations.UploadFileOperation
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
        operation: UploadFileOperation,
        startIntent: PendingIntent,
        currentUploadIndex: Int,
        totalUploadSize: Int
    ) {
        currentOperationTitle = if (totalUploadSize > 1) {
            String.format(
                context.getString(R.string.upload_notification_manager_start_text),
                currentUploadIndex,
                totalUploadSize,
                operation.fileName
            )
        } else {
            operation.fileName
        }

        val progressText = NumberFormatter.getPercentageText(0)

        notificationBuilder.run {
            setProgress(100, 0, false)
            setContentTitle(currentOperationTitle)
            setContentText(progressText)
            setOngoing(false)
            clearActions()
            setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.upload_notification_manager_content_intent_description))
            )
            addAction(UploadBroadcastAction.PauseAndCancel(operation).pauseAction(context))
            addAction(UploadBroadcastAction.PauseAndCancel(operation).cancelAction(context))
            setContentIntent(startIntent)
        }

        if (!operation.isInstantPicture && !operation.isInstantVideo) {
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

    fun showQuotaExceedNotification(operation: UploadFileOperation) {
        val notification = notificationBuilder.run {
            setContentTitle(context.getString(R.string.upload_quota_exceeded))
            setContentText("")
            clearActions()
            setProgress(0, 0, false)
        }.build()

        showNotification(operation.file.fileId.toInt(), notification)
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

        dismissNotification(operation.ocUploadId.toInt())
    }

    fun dismissErrorNotification() = notificationManager.cancel(FileUploadWorker.NOTIFICATION_ERROR_ID)

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
