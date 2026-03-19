/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
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
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.DownloadType
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.FileExportUtils
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.theme.ViewThemeUtils

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
        val (succeeded, failed) = exportFiles(fileIDs, storageManager)

        showSummaryNotification(succeeded, failed)
        return Result.success()
    }

    private fun exportFiles(fileIDs: LongArray, storageManager: FileDataStorageManager): Pair<Int, Int> {
        val fileDownloadHelper = FileDownloadHelper.instance()
        val fileExportUtils = FileExportUtils()
        var succeeded = 0
        var failed = 0

        fileIDs
            .asSequence()
            .mapNotNull { storageManager.getFileById(it) }
            .forEach { ocFile ->
                val exported = when {
                    !FileStorageUtils.checkIfEnoughSpace(ocFile) -> false

                    ocFile.isDown -> runCatching {
                        fileExportUtils.exportFile(ocFile.fileName, ocFile.mimeType, contentResolver, ocFile, null)
                    }.onFailure { Log_OC.e(TAG, "Error exporting file", it) }.isSuccess

                    else -> {
                        fileDownloadHelper.downloadFile(user, ocFile, downloadType = DownloadType.EXPORT)
                        true
                    }
                }

                if (exported) succeeded++ else failed++
            }

        return succeeded to failed
    }

    private fun showSummaryNotification(succeeded: Int, failed: Int) {
        val resources = appContext.resources
        val message = when {
            failed == 0 -> resources.getQuantityString(R.plurals.export_successful, succeeded, succeeded)
            succeeded == 0 -> resources.getQuantityString(R.plurals.export_failed, failed, failed)
            else -> resources.getQuantityString(R.plurals.export_partially_failed, succeeded, succeeded)
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext,
            NOTIFICATION_ID,
            Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply { flags = FLAG_ACTIVITY_NEW_TASK },
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(message)
            .setAutoCancel(true)
            .addAction(NotificationCompat.Action(null, appContext.getString(R.string.locate_folder), pendingIntent))
            .also { viewThemeUtils.androidx.themeNotificationCompatBuilder(appContext, it) }
            .build()

        (appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 179
        const val FILES_TO_DOWNLOAD = "files_to_download"
        private val TAG = FilesExportWork::class.simpleName
    }
}
