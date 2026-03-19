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
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.worker.WorkerFilesPayload
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.DownloadFileOperation
import com.owncloud.android.operations.DownloadType
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.FileExportUtils
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FilesExportWork(
    private val context: Context,
    private val user: User,
    private val contentResolver: ContentResolver,
    private val viewThemeUtils: ViewThemeUtils,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val path = inputData.getString(FILES_TO_DOWNLOAD)
        val fileIds = WorkerFilesPayload.read(path)
        if (fileIds.isEmpty()) {
            Log_OC.w(TAG, "File export was started without any file")
            WorkerFilesPayload.cleanup(path)
            return Result.success()
        }

        val storageManager = FileDataStorageManager(user, contentResolver)

        try {
            val (succeeded, failed) = exportFiles(fileIds, storageManager)
            showSummaryNotification(succeeded, failed)
        } finally {
            WorkerFilesPayload.cleanup(path)
        }

        return Result.success()
    }

    @Suppress("DEPRECATION")
    private suspend fun exportFiles(fileIDs: List<Long>, storageManager: FileDataStorageManager): Pair<Int, Int> =
        withContext(Dispatchers.IO) {
            val client = runCatching {
                OwnCloudClientManagerFactory.getDefaultSingleton()
                    .getClientFor(user.toOwnCloudAccount(), context)
            }.onFailure {
                Log_OC.e(TAG, "Failed to create OwnCloudClient", it)
            }.getOrNull()

            val fileExportUtils = FileExportUtils()
            var succeeded = 0
            var failed = 0

            fileIDs
                .mapNotNull { storageManager.getFileById(it) }
                .forEach { ocFile ->
                    val exported = when {
                        !FileStorageUtils.checkIfEnoughSpace(ocFile) -> false

                        ocFile.isDown -> runCatching {
                            fileExportUtils.exportFile(ocFile.fileName, ocFile.mimeType, contentResolver, ocFile, null)
                        }.onFailure { Log_OC.e(TAG, "Error exporting file", it) }.isSuccess

                        client != null -> downloadFile(ocFile, client)

                        else -> {
                            Log_OC.e(TAG, "Skipping download, client unavailable: ${ocFile.remotePath}")
                            false
                        }
                    }

                    if (exported) succeeded++ else failed++
                }

            return@withContext succeeded to failed
        }

    @Suppress("DEPRECATION")
    private suspend fun downloadFile(file: OCFile, client: OwnCloudClient): Boolean = withContext(Dispatchers.IO) {
        val operation = DownloadFileOperation(user, file, context)
        operation.downloadType = DownloadType.EXPORT
        return@withContext runCatching {
            operation.execute(client)?.isSuccess == true
        }.onFailure {
            Log_OC.e(TAG, "Exception downloading file: ${file.remotePath}", it)
        }.getOrDefault(false)
    }

    private fun showSummaryNotification(succeeded: Int, failed: Int) {
        val resources = context.resources
        val message = when {
            failed == 0 -> resources.getQuantityString(R.plurals.export_successful, succeeded, succeeded)
            succeeded == 0 -> resources.getQuantityString(R.plurals.export_failed, failed, failed)
            else -> resources.getQuantityString(R.plurals.export_partially_failed, succeeded, succeeded)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply { flags = FLAG_ACTIVITY_NEW_TASK },
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(message)
            .setAutoCancel(true)
            .addAction(NotificationCompat.Action(null, context.getString(R.string.locate_folder), pendingIntent))
            .also { viewThemeUtils.androidx.themeNotificationCompatBuilder(context, it) }
            .build()

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NOTIFICATION_ID = 179
        const val FILES_TO_DOWNLOAD = "files_to_download"
        private val TAG = FilesExportWork::class.simpleName
    }
}
