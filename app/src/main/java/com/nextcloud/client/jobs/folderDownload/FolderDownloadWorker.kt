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
        const val SYNC_ALL = "SYNC_ALL"

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
            Log_OC.e(TAG, "failed accountName cannot be null")
            return Result.failure()
        }

        val optionalUser = accountManager.getUser(accountName)
        if (optionalUser.isEmpty) {
            Log_OC.e(TAG, "failed user is not present")
            return Result.failure()
        }

        val syncAll = inputData.getBoolean(SYNC_ALL, false)

        val user = optionalUser.get()
        storageManager = FileDataStorageManager(user, context.contentResolver)
        val folder = storageManager.getFileById(folderID)
        if (folder == null) {
            Log_OC.e(TAG, "failed folder cannot be nul")
            return Result.failure()
        }

        if (syncAll) {
            Log_OC.d(TAG, "checking folder size including all nested subfolders")
            if (!FileStorageUtils.checkIfEnoughSpace(folder)) {
                notificationManager.showNotAvailableDiskSpace()
                return Result.failure()
            }
        }

        Log_OC.d(TAG, "🕒 started for ${user.accountName} downloading ${folder.fileName}")

        trySetForeground(folder)

        folderDownloadEventBroadcaster.sendDownloadEnqueued(folder.fileId)
        pendingDownloads.add(folder.fileId)

        val downloadHelper = FileDownloadHelper.instance()

        return withContext(Dispatchers.IO) {
            try {
                val files = getFiles(folder, storageManager, syncAll)
                val account = user.toOwnCloudAccount()
                val client = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(account, context)

                var result = true
                files.forEachIndexed { index, file ->
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
                folderDownloadEventBroadcaster.sendDownloadCompleted(folder.fileId)
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

    private fun getFiles(folder: OCFile, storageManager: FileDataStorageManager, syncAll: Boolean): List<OCFile> =
        if (syncAll) {
            storageManager.getAllFilesRecursivelyInsideFolder(folder)
                .filter { !it.isDown }
        } else {
            storageManager.getFolderContent(folder, false)
                .filter { !it.isFolder && !it.isDown }
        }
}
