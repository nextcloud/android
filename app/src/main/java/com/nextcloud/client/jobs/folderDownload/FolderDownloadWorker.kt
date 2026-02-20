/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.folderDownload

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.files.FileIndicator
import com.nextcloud.client.files.FileIndicatorManager
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.nextcloud.model.WorkerState
import com.nextcloud.model.WorkerStateObserver
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.DownloadFileOperation
import com.owncloud.android.operations.DownloadType
import com.owncloud.android.ui.helpers.FileOperationsHelper
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

@Suppress("LongMethod", "TooGenericExceptionCaught")
class FolderDownloadWorker(
    private val accountManager: UserAccountManager,
    private val context: Context,
    viewThemeUtils: ViewThemeUtils,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "📂" + "FolderDownloadWorker"
        const val FOLDER_ID = "FOLDER_ID"
        const val ACCOUNT_NAME = "ACCOUNT_NAME"

        private val pendingDownloads: MutableSet<Long> = ConcurrentHashMap.newKeySet<Long>()

        fun isDownloading(id: Long): Boolean = pendingDownloads.contains(id)
    }

    private val notificationManager = FolderDownloadWorkerNotificationManager(context, viewThemeUtils)
    private lateinit var storageManager: FileDataStorageManager

    @Suppress("ReturnCount", "DEPRECATION")
    override suspend fun doWork(): Result {
        val folderID = inputData.getLong(FOLDER_ID, -1)
        if (folderID == -1L) {
            return Result.failure()
        }

        val accountName = inputData.getString(ACCOUNT_NAME)
        if (accountName == null) {
            Log_OC.e(TAG, "failed accountName cannot be null")
            return Result.failure()
        }

        val optionalUser = accountManager.getUser(accountName)
        if (optionalUser.isEmpty) {
            Log_OC.e(TAG, "failed user is not present")
            return Result.failure()
        }

        val user = optionalUser.get()
        storageManager = FileDataStorageManager(user, context.contentResolver)
        val folder = storageManager.getFileById(folderID)
        if (folder == null) {
            Log_OC.e(TAG, "failed folder cannot be nul")
            return Result.failure()
        }

        FileIndicatorManager.update(folder.fileId, FileIndicator.Syncing)
        Log_OC.d(TAG, "🕒 started for ${user.accountName} downloading ${folder.fileName}")

        trySetForeground(folder)

        pendingDownloads.add(folder.fileId)

        val downloadHelper = FileDownloadHelper.instance()

        return withContext(Dispatchers.IO) {
            try {
                val files = getFiles(folder, storageManager)
                val account = user.toOwnCloudAccount()
                val client = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(account, context)

                var result = true
                files.forEachIndexed { index, file ->
                    if (!checkDiskSize(file)) {
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

                        val foregroundInfo = notificationManager.getForegroundInfo(notification)
                        setForeground(foregroundInfo)
                    }

                    FileIndicatorManager.update(file.fileId, FileIndicator.Syncing)
                    val operation = DownloadFileOperation(user, file, context)
                    val operationResult = operation.execute(client)
                    if (operationResult?.isSuccess == true && operation.downloadType === DownloadType.DOWNLOAD) {
                        FileIndicatorManager.update(file.fileId, FileIndicator.Synced)
                        getOCFile(operation)?.let { ocFile ->
                            downloadHelper.saveFile(ocFile, operation, storageManager)
                        }
                    }

                    if (!operationResult.isSuccess) {
                        result = false
                    }
                }

                withContext(Dispatchers.Main) {
                    notificationManager.showCompletionNotification(folder.fileName, result)
                }

                if (result) {
                    Log_OC.d(TAG, "✅ completed")
                    FileIndicatorManager.update(folderID, FileIndicator.Synced)
                    Result.success()
                } else {
                    Log_OC.d(TAG, "❌ failed")
                    FileIndicatorManager.update(folderID, FileIndicator.Error)
                    Result.failure()
                }
            } catch (e: Exception) {
                Log_OC.d(TAG, "❌ failed reason: $e")
                FileIndicatorManager.update(folderID, FileIndicator.Error)
                Result.failure()
            } finally {
                WorkerStateObserver.send(WorkerState.FolderDownloadCompleted(folder))
                pendingDownloads.remove(folder.fileId)
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

            val folder = storageManager.getFileById(folderID) ?: return notificationManager.getForegroundInfo(null)

            return notificationManager.getForegroundInfo(folder)
        } catch (e: Exception) {
            Log_OC.w(TAG, "⚠️ Error getting foreground info: ${e.message}")
            notificationManager.getForegroundInfo(null)
        }
    }

    private suspend fun trySetForeground(folder: OCFile) {
        try {
            val foregroundInfo = notificationManager.getForegroundInfo(folder)
            setForeground(foregroundInfo)
        } catch (e: Exception) {
            Log_OC.w(TAG, "⚠️ Could not set foreground service: ${e.message}")
        }
    }

    private fun getOCFile(operation: DownloadFileOperation): OCFile? {
        val file = operation.file?.fileId?.let { storageManager.getFileById(it) }
            ?: storageManager.getFileByDecryptedRemotePath(operation.file?.remotePath)
            ?: run {
                Log_OC.e(TAG, "could not save ${operation.file?.remotePath}")
                return null
            }

        return file
    }

    private fun getFiles(folder: OCFile, storageManager: FileDataStorageManager): List<OCFile> =
        storageManager.getFolderContent(folder, false)
            .filter { !it.isFolder && !it.isDown }

    private fun checkDiskSize(file: OCFile): Boolean {
        val fileSizeInByte = file.fileLength
        val availableDiskSpace = FileOperationsHelper.getAvailableSpaceOnDevice()

        return if (availableDiskSpace < fileSizeInByte) {
            notificationManager.showNotAvailableDiskSpace()
            false
        } else {
            true
        }
    }
}
