/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.DownloadFileOperation
import com.owncloud.android.ui.helpers.FileOperationsHelper
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

@Suppress("LongMethod")
class SyncWorker(
    private val user: User,
    private val context: Context,
    private val viewThemeUtils: ViewThemeUtils,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncWorker"
        const val FOLDER_ID = "FOLDER_ID"

        private val pendingFolderDownloads: MutableSet<Long> = ConcurrentHashMap.newKeySet<Long>()

        fun isDownloading(id: Long): Boolean = pendingFolderDownloads.contains(id)
    }

    private var notificationManager: SyncWorkerNotificationManager? = null

    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    override suspend fun doWork(): Result {
        val folderID = inputData.getLong(FOLDER_ID, -1)
        if (folderID == -1L) {
            return Result.failure()
        }
        val storageManager = FileDataStorageManager(user, context.contentResolver)
        val folder = storageManager.getFileById(folderID) ?: return Result.failure()

        notificationManager = SyncWorkerNotificationManager(context, viewThemeUtils)

        Log_OC.d(TAG, "SyncWorker started")

        val foregroundInfo = notificationManager?.getForegroundInfo(folder) ?: return Result.failure()
        setForeground(foregroundInfo)

        pendingFolderDownloads.add(folder.fileId)

        return withContext(Dispatchers.IO) {
            try {
                val files = getFiles(folder, storageManager)
                val client = getClient()

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

                    val syncFileResult = syncFile(file, client)
                    if (!syncFileResult) {
                        result = false
                    }
                }

                withContext(Dispatchers.Main) {
                    notificationManager?.showCompletionMessage(folder.fileName, result)
                }

                if (result) {
                    Log_OC.d(TAG, "SyncWorker completed")
                    Result.success()
                } else {
                    Log_OC.d(TAG, "SyncWorker failed")
                    Result.failure()
                }
            } catch (e: Exception) {
                Log_OC.d(TAG, "SyncWorker failed reason: $e")
                Result.failure()
            } finally {
                pendingFolderDownloads.remove(folder.fileId)
                notificationManager?.dismiss()
            }
        }
    }

    private fun getFiles(folder: OCFile, storageManager: FileDataStorageManager): List<OCFile> =
        storageManager.getFolderContent(folder, false)
            .filter { !it.isFolder && !it.isDown }

    @Suppress("DEPRECATION")
    private fun getClient(): OwnCloudClient {
        val account = user.toOwnCloudAccount()
        return OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(account, context)
    }

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

    @Suppress("DEPRECATION")
    private suspend fun syncFile(file: OCFile, client: OwnCloudClient): Boolean = withContext(Dispatchers.IO) {
        val operation = DownloadFileOperation(user, file, context).execute(client)
        Log_OC.d(TAG, "Syncing file: " + file.decryptedRemotePath)
        operation.isSuccess
    }
}
