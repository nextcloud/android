/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs.download

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.nextcloud.client.jobs.notification.WorkerNotificationManager
import com.nextcloud.utils.numberFormatter.NumberFormatter
import com.owncloud.android.R
import com.owncloud.android.operations.DownloadFileOperation
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.security.SecureRandom

@Suppress("MagicNumber")
class DownloadNotificationManager(id: Int, private val context: Context, viewThemeUtils: ViewThemeUtils) :
    WorkerNotificationManager(
        id,
        context,
        viewThemeUtils,
        tickerId = R.string.downloader_download_in_progress_ticker,
        channelId = NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD
    ) {

    fun prepareForStart(operation: DownloadFileOperation) {
        currentOperationTitle = operation.file.fileName

        notificationBuilder.run {
            setContentTitle(currentOperationTitle)
            setOngoing(false)
            setProgress(100, 0, operation.size < 0)
            clearActions()
            addAction(
                R.drawable.ic_cancel,
                context.getString(R.string.common_cancel),
                getCancelPendingIntent(operation)
            )
        }

        showNotification()
    }

    private fun getCancelPendingIntent(operation: DownloadFileOperation): PendingIntent {
        val intent = Intent(context, FileDownloadCancelReceiver::class.java).apply {
            putExtra(FileDownloadCancelReceiver.ACCOUNT_NAME, operation.user.accountName)
            putExtra(FileDownloadCancelReceiver.FILE_ID, operation.file.fileId)
            setPackage(context.packageName)
        }

        return PendingIntent.getBroadcast(
            context,
            getId(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun prepareForResult() {
        notificationBuilder
            .clearActions()
            .setAutoCancel(true)
            .setOngoing(false)
            .setProgress(0, 0, false)
    }

    fun updateDownloadProgress(percent: Int, totalToTransfer: Long) {
        val progressText = NumberFormatter.getPercentageText(percent)
        setProgress(percent, progressText, totalToTransfer < 0)
        showNotification()
    }

    fun dismissNotification() {
        dismissNotification(2000)
    }

    fun showNewNotification(text: String) {
        val notifyId = SecureRandom().nextInt()

        notificationBuilder.run {
            clearActions()
            setProgress(0, 0, false)
            setContentTitle(text)
            setOngoing(false)
            notificationManager.notify(notifyId, this.build())
        }
    }

    fun setContentIntent(intent: Intent, flag: Int) {
        notificationBuilder.setContentIntent(
            PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                flag
            )
        )
    }
}
