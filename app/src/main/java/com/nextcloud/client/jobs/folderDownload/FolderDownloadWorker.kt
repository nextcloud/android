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
        const val RECURSIVE_DOWNLOAD = "RECURSIVE_DOWNLOAD"

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

        val recursiveDownload = inputData.getBoolean(RECURSIVE_DOWNLOAD, false)

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

        Log_OC.d(TAG, "🕒 started for ${user.accountName} downloading ${folder.fileName} (recursive: $recursiveDownload)")

        trySetForeground(folder)

        pendingDownloads.add(folder.fileId)

        val downloadHelper = FileDownloadHelper.instance()

        return withContext(Dispatchers.IO) {
            try {
                val files = getFiles(folder, storageManager, recursiveDownload)
                
                // Add warning log when no files found for recursive download
                if (files.isEmpty()) {
                    Log_OC.w(TAG, "⚠️ No files found for recursive download in folder: ${folder.fileName}")
                }
                
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

                    val operation = DownloadFileOperation(user, file, context)
                    val operationResult = operation.execute(client)
                    if (operationResult?.isSuccess == true && operation.downloadType === DownloadType.DOWNLOAD) {
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
                    Result.success()
                } else {
                    Log_OC.d(TAG, "❌ failed")
                    Result.failure()
                }
            } catch (e: Exception) {
                Log_OC.d(TAG, "❌ failed reason: $e")
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

    private fun getFiles(folder: OCFile, storageManager: FileDataStorageManager, recursive: Boolean): List<OCFile> {
        if (recursive) {
            return getAllFilesRecursive(folder, storageManager)
        }
        // Use folder ID to avoid fileExists() check
        return storageManager.getFolderContent(folder.fileId, false)
            .filter { !it.isFolder }
    }

    /**
     * Recursively get all files in the folder and its subfolders
     */
    private fun getAllFilesRecursive(folder: OCFile, storageManager: FileDataStorageManager): List<OCFile> {
        val result = mutableListOf<OCFile>()
        
        // Use the folder ID directly to avoid fileExists() check that fails for subfolders not yet downloaded
        val folderContent = storageManager.getFolderContent(folder.fileId, false)
        
        Log_OC.d(TAG, "📂 getAllFilesRecursive: folder=${folder.fileName}, folderId=${folder.fileId}, contentCount=${folderContent.size}")
        
        for (file in folderContent) {
            if (!file.isFolder) {
                // Add all files, regardless of download status, to ensure subfolders are synced
                result.add(file)
            } else {
                Log_OC.d(TAG, "📂 Found subfolder: ${file.fileName}, recursing...")
                // Recursively process subfolders
                result.addAll(getAllFilesRecursive(file, storageManager))
            }
        }
        
        Log_OC.d(TAG, "📂 getAllFilesRecursive: returning ${result.size} files from folder ${folder.fileName}")
        return result
    }

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
