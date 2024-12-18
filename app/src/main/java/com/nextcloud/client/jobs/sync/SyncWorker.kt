/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.sync

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

class SyncWorker(
    private val user: User,
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncWorker"

        const val FILE_PATHS = "FILE_PATHS"

        const val SYNC_WORKER_COMPLETION_BROADCAST = "SYNC_WORKER_COMPLETION_BROADCAST"
        const val FILE_DOWNLOAD_COMPLETION_BROADCAST = "FILE_DOWNLOAD_COMPLETION_BROADCAST"
        const val FILE_PATH = "FILE_PATH"

        private var downloadingFilePaths = mutableListOf<String>()

        /**
         * It is used to add the sync icon next to the file in the folder.
         */
        fun isDownloading(path: String): Boolean {
            return downloadingFilePaths.contains(path)
        }
    }

    private val notificationManager = SyncWorkerNotificationManager(context)

    @Suppress("TooGenericExceptionCaught")
    override suspend fun doWork(): Result {
        Log_OC.d(TAG, "SyncWorker started")

        withContext(Dispatchers.Main) {
            notificationManager.showStartNotification()
        }

        return withContext(Dispatchers.IO) {
            try {
                val filePaths = inputData.getStringArray(FILE_PATHS)
                if (filePaths.isNullOrEmpty()) {
                    return@withContext Result.success()
                }

                val storageManager = FileDataStorageManager(user, context.contentResolver)

                val topParentPath = prepareDownloadingFilePathsAndGetTopParentPath(storageManager, filePaths)

                val client = getClient()

                var result = true
                filePaths.forEachIndexed { index, path ->
                    if (isStopped) {
                        notificationManager.dismiss()
                        return@withContext Result.failure()
                    }

                    val file = storageManager.getFileByDecryptedRemotePath(path) ?: return@forEachIndexed

                    if (!checkDiskSize(file)) {
                        return@withContext Result.failure()
                    }

                    withContext(Dispatchers.Main) {
                        notificationManager.showProgressNotification(file.fileName, index, filePaths.size)
                    }

                    val syncFileResult = syncFile(file, path, client)
                    if (!syncFileResult) {
                        result = false
                    }
                }

                withContext(Dispatchers.Main) {
                    notificationManager.showCompletionMessage(result)
                }

                if (result) {
                    downloadingFilePaths.remove(topParentPath)
                    sendSyncWorkerCompletionBroadcast()
                    Log_OC.d(TAG, "SyncWorker completed")
                    Result.success()
                } else {
                    Log_OC.d(TAG, "SyncWorker failed")
                    Result.failure()
                }
            } catch (e: Exception) {
                Log_OC.d(TAG, "SyncWorker failed reason: $e")
                Result.failure()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun getClient(): OwnCloudClient {
        val account = user.toOwnCloudAccount()
        return OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(account, context)
    }

    /**
     * Add the topParentPath to mark the sync icon on the selected folder.
     */
    private fun prepareDownloadingFilePathsAndGetTopParentPath(
        storageManager: FileDataStorageManager,
        filePaths: Array<String>
    ): String {
        val topParentPath = getTopParentPath(storageManager, filePaths.first())
        downloadingFilePaths = Collections.synchronizedList(ArrayList(filePaths.toList())).apply {
            add(topParentPath)
        }

        return topParentPath
    }

    private suspend fun checkDiskSize(file: OCFile): Boolean {
        val fileSizeInByte = file.fileLength
        val availableDiskSpace = FileOperationsHelper.getAvailableSpaceOnDevice()

        return if (availableDiskSpace < fileSizeInByte) {
            notificationManager.showNotAvailableDiskSpace()
            false
        } else {
            true
        }
    }

    @Suppress("DEPRECATION")
    private fun syncFile(file: OCFile, path: String, client: OwnCloudClient): Boolean {
        val operation = DownloadFileOperation(user, file, context).execute(client)
        Log_OC.d(TAG, "Syncing file: " + file.decryptedRemotePath)

        return if (operation.isSuccess) {
            sendFileDownloadCompletionBroadcast(path)
            downloadingFilePaths.remove(path)
            true
        } else {
            false
        }
    }

    private fun getTopParentPath(storageManager: FileDataStorageManager, firstFilePath: String): String {
        val firstFile = storageManager.getFileByDecryptedRemotePath(firstFilePath)
        val topParentFile = storageManager.getTopParent(firstFile)
        return topParentFile.decryptedRemotePath
    }

    /**
     * It is used to remove the sync icon next to the file in the folder.
     */
    private fun sendFileDownloadCompletionBroadcast(path: String) {
        val intent = Intent(FILE_DOWNLOAD_COMPLETION_BROADCAST).apply {
            putExtra(FILE_PATH, path)
        }

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    /**
     * This function is called when the download of all files in a folder is complete.
     * It is used to add a green tick icon next to the files after the download process is finished.
     */
    private fun sendSyncWorkerCompletionBroadcast() {
        val intent = Intent(SYNC_WORKER_COMPLETION_BROADCAST)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
}
