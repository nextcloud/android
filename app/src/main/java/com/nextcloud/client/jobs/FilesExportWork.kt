/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs

import android.app.DownloadManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.DownloadType
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.FileExportUtils
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.security.SecureRandom

class FilesExportWork(
    private val appContext: Context,
    private val user: User,
    private val contentResolver: ContentResolver,
    private val viewThemeUtils: ViewThemeUtils,
    params: WorkerParameters
) : Worker(appContext, params) {

    private lateinit var storageManager: FileDataStorageManager

    override fun doWork(): Result {
        val fileIDs = inputData.getLongArray(FILES_TO_DOWNLOAD) ?: LongArray(0)

        if (fileIDs.isEmpty()) {
            Log_OC.w(this, "File export was started without any file")
            return Result.success()
        }

        storageManager = FileDataStorageManager(user, contentResolver)

        val successfulExports = exportFiles(fileIDs)

        showSuccessNotification(successfulExports)
        return Result.success()
    }

    private fun exportFiles(fileIDs: LongArray): Int {
        val fileDownloadHelper = FileDownloadHelper.instance()

        var successfulExports = 0
        fileIDs
            .asSequence()
            .map { storageManager.getFileById(it) }
            .filterNotNull()
            .forEach { ocFile ->
                if (!FileStorageUtils.checkIfEnoughSpace(ocFile)) {
                    showErrorNotification(successfulExports)
                    return@forEach
                }

                if (ocFile.isDown) {
                    try {
                        exportFile(ocFile)
                    } catch (e: IllegalStateException) {
                        Log_OC.e(TAG, "Error exporting file", e)
                        showErrorNotification(successfulExports)
                    }
                } else {
                    fileDownloadHelper.downloadFile(
                        user,
                        ocFile,
                        downloadType = DownloadType.EXPORT
                    )
                }

                successfulExports++
            }
        return successfulExports
    }

    @Throws(IllegalStateException::class)
    private fun exportFile(ocFile: OCFile) {
        FileExportUtils().exportFile(
            ocFile.fileName,
            ocFile.mimeType,
            contentResolver,
            ocFile,
            null
        )
    }

    private fun showErrorNotification(successfulExports: Int) {
        val message = if (successfulExports == 0) {
            appContext.resources.getQuantityString(R.plurals.export_failed, successfulExports, successfulExports)
        } else {
            appContext.resources.getQuantityString(
                R.plurals.export_partially_failed,
                successfulExports,
                successfulExports
            )
        }
        showNotification(message)
    }

    private fun showSuccessNotification(successfulExports: Int) {
        showNotification(
            appContext.resources.getQuantityString(
                R.plurals.export_successful,
                successfulExports,
                successfulExports
            )
        )
    }

    private fun showNotification(message: String) {
        val notificationId = SecureRandom().nextInt()

        val notificationBuilder = NotificationCompat.Builder(
            appContext,
            NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD
        )
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(message)
            .setAutoCancel(true)

        viewThemeUtils.androidx.themeNotificationCompatBuilder(appContext, notificationBuilder)

        val actionIntent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
            flags = FLAG_ACTIVITY_NEW_TASK
        }
        val actionPendingIntent = PendingIntent.getActivity(
            appContext,
            notificationId,
            actionIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )
        notificationBuilder.addAction(
            NotificationCompat.Action(
                null,
                appContext.getString(R.string.locate_folder),
                actionPendingIntent
            )
        )

        val notificationManager = appContext
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    companion object {
        const val FILES_TO_DOWNLOAD = "files_to_download"
        private val TAG = FilesExportWork::class.simpleName
    }
}
