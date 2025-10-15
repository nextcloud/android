/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.folderDownload

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.jobs.download.FileDownloadHelper
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

@Suppress("LongMethod")
class FolderDownloadWorker(
    private val accountManager: UserAccountManager,
    private val context: Context,
    private val viewThemeUtils: ViewThemeUtils,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "📂" + "FolderDownloadWorker"
        const val FOLDER_ID = "FOLDER_ID"
        const val ACCOUNT_NAME = "ACCOUNT_NAME"

        private val pendingDownloads: MutableSet<Long> = ConcurrentHashMap.newKeySet<Long>()

        fun isDownloading(id: Long): Boolean = pendingDownloads.contains(id)
    }

    private var notificationManager: FolderDownloadWorkerNotificationManager? = null
    private lateinit var storageManager: FileDataStorageManager

    @Suppress("TooGenericExceptionCaught", "ReturnCount", "DEPRECATION")
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

        notificationManager = FolderDownloadWorkerNotificationManager(context, viewThemeUtils)

        Log_OC.d(TAG, "🕒 started for ${user.accountName} downloading ${folder.fileName}")

        val foregroundInfo = notificationManager?.getForegroundInfo(folder) ?: return Result.failure()
        setForeground(foregroundInfo)

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
                        notificationManager?.showProgressNotification(
                            folder.fileName,
                            file.fileName,
                            index,
                            files.size
                        )
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
                    notificationManager?.showCompletionMessage(folder.fileName, result)
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
                pendingDownloads.remove(folder.fileId)
                notificationManager?.dismiss()
            }
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

    private suspend fun checkDiskSize(file: OCFile): Boolean {
        val fileSizeInByte = file.fileLength
        val availableDiskSpace = FileOperationsHelper.getAvailableSpaceOnDevice()

        return if (availableDiskSpace < fileSizeInByte) {
            notificationManager?.showNotAvailableDiskSpace()
            false
        } else {
            true
        }
    }
}
