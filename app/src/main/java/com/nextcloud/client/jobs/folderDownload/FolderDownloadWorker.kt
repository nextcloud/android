/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.folderDownload

import android.content.Context
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.DownloadFileOperation
import com.owncloud.android.operations.DownloadType
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

@Suppress("LongMethod", "TooGenericExceptionCaught")
class FolderDownloadWorker(
    private val accountManager: UserAccountManager,
    private val context: Context,
    viewThemeUtils: ViewThemeUtils,
    localBroadcastManager: LocalBroadcastManager,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "📂" + "FolderDownloadWorker"
        const val FOLDER_ID = "FOLDER_ID"
        const val ACCOUNT_NAME = "ACCOUNT_NAME"
        private val pendingDownloads: MutableSet<Long> = ConcurrentHashMap.newKeySet()
        fun isDownloading(id: Long): Boolean = pendingDownloads.contains(id)
    }

    private val notificationManager = FolderDownloadWorkerNotificationManager(context, viewThemeUtils)
    private val folderDownloadEventBroadcaster = FolderDownloadEventBroadcaster(context, localBroadcastManager)
    private lateinit var storageManager: FileDataStorageManager

    @Suppress("ReturnCount", "DEPRECATION")
    override suspend fun doWork(): Result {
        val folderID = inputData.getLong(FOLDER_ID, -1)
        if (folderID == -1L) {
            return Result.failure()
        }

        val accountName = inputData.getString(ACCOUNT_NAME)
        if (accountName == null) {
            Log_OC.e(TAG, "failed: accountName cannot be null")
            return Result.failure()
        }

        val optionalUser = accountManager.getUser(accountName)
        if (optionalUser.isEmpty) {
            Log_OC.e(TAG, "failed: user is not present")
            return Result.failure()
        }

        val user = optionalUser.get()
        storageManager = FileDataStorageManager(user, context.contentResolver)

        val folder = storageManager.getFileById(folderID)
        if (folder == null) {
            Log_OC.e(TAG, "failed: folder cannot be null")
            return Result.failure()
        }

        Log_OC.d(TAG, "🕒 started for ${user.accountName} | folder=${folder.fileName}")

        trySetForeground(folder)
        folderDownloadEventBroadcaster.sendDownloadEnqueued(folder.fileId)
        pendingDownloads.add(folder.fileId)

        val downloadHelper = FileDownloadHelper.instance()

        return withContext(Dispatchers.IO) {
            try {
                val account = user.toOwnCloudAccount()
                val client = OwnCloudClientManagerFactory.getDefaultSingleton()
                    .getClientFor(account, context)
                val files = getFiles(folder, storageManager)
                if (files.isEmpty()) {
                    Log_OC.d(TAG, "✅ no files need downloading")
                    notificationManager.showCompletionNotification(folder.fileName, true)
                    return@withContext Result.success()
                }

                var overallSuccess = true

                files.forEachIndexed { index, file ->
                    if (isStopped) {
                        Log_OC.d(TAG, "⚠️ worker stopped mid-download, aborting remaining files")
                        return@withContext Result.failure()
                    }

                    if (!FileStorageUtils.checkIfEnoughSpace(folder)) {
                        notificationManager.showNotAvailableDiskSpace()
                        return@withContext Result.failure()
                    }

                    withContext(Dispatchers.Main) {
                        val notification = notificationManager.getProgressNotification(
                            folder.fileName,
                            file.fileName,
                            index,
                            files.size
                        )
                        notificationManager.showNotification(notification)
                        setForeground(notificationManager.getForegroundInfo(notification))
                    }

                    val operation = DownloadFileOperation(user, file, context)
                    val operationResult = operation.execute(client)

                    if (operationResult?.isSuccess == true && operation.downloadType === DownloadType.DOWNLOAD) {
                        getOCFile(operation)?.let { ocFile ->
                            downloadHelper.saveFile(ocFile, operation, storageManager)
                        }
                    }

                    if (operationResult?.isSuccess != true) {
                        Log_OC.w(TAG, "⚠️ download failed for ${file.remotePath}: ${operationResult?.logMessage}")
                        overallSuccess = false
                    }
                }

                notificationManager.showCompletionNotification(folder.fileName, overallSuccess)

                if (overallSuccess) {
                    Log_OC.d(TAG, "✅ completed successfully")
                    Result.success()
                } else {
                    Log_OC.d(TAG, "❌ completed with failures")
                    Result.failure()
                }
            } catch (e: Exception) {
                Log_OC.e(TAG, "❌ unexpected failure: $e")
                notificationManager.showCompletionNotification(folder.fileName, false)
                Result.failure()
            } finally {
                folderDownloadEventBroadcaster.sendDownloadCompleted(folder.fileId)
                pendingDownloads.remove(folder.fileId)

                // delay so that user can see the error or success notification
                delay(2000)
                notificationManager.dismiss()
            }
        }
    }

    @Suppress("ReturnCount")
    override suspend fun getForegroundInfo(): ForegroundInfo {
        return try {
            val folderID = inputData.getLong(FOLDER_ID, -1)
            val accountName = inputData.getString(ACCOUNT_NAME)

            if (folderID == -1L || accountName == null || !::storageManager.isInitialized) {
                return notificationManager.getForegroundInfo(null)
            }

            val folder = storageManager.getFileById(folderID)
                ?: return notificationManager.getForegroundInfo(null)

            notificationManager.getForegroundInfo(folder)
        } catch (e: Exception) {
            Log_OC.w(TAG, "⚠️ error getting foreground info: ${e.message}")
            notificationManager.getForegroundInfo(null)
        }
    }

    private suspend fun trySetForeground(folder: OCFile) {
        try {
            val foregroundInfo = notificationManager.getForegroundInfo(folder)
            setForeground(foregroundInfo)
        } catch (e: Exception) {
            Log_OC.w(TAG, "⚠️ could not set foreground service: ${e.message}")
        }
    }

    private fun getOCFile(operation: DownloadFileOperation): OCFile? = operation.file?.fileId?.let {
        storageManager.getFileById(it)
    }
        ?: storageManager.getFileByDecryptedRemotePath(operation.file?.remotePath)
        ?: run {
            Log_OC.e(TAG, "could not resolve OCFile for save: ${operation.file?.remotePath}")
            null
        }

    private fun getFiles(folder: OCFile, storageManager: FileDataStorageManager): List<OCFile> =
        storageManager.getFolderContent(folder, false)
            .filter { !it.isFolder && !it.isDown }
}
