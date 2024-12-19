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
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.helpers.FileOperationsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    private val user: User,
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncWorker"

        const val FOLDER_ID = "FOLDER_ID"

        const val FILE_DOWNLOAD_COMPLETION_BROADCAST = "FILE_DOWNLOAD_COMPLETION_BROADCAST"
        const val FILE_PATH = "FILE_PATH"

        private var downloadingFiles = mutableListOf<OCFile>()

        /**
         * It is used to add the sync icon next to the file in the folder.
         */
        fun isDownloading(path: String): Boolean {
            return downloadingFiles.any { it.decryptedRemotePath == path }
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
                val folderID = inputData.getLong(FOLDER_ID, -1)
                if (folderID == -1L) {
                    return@withContext Result.failure()
                }

                val storageManager = FileDataStorageManager(user, context.contentResolver)
                val folder = storageManager.getFileById(folderID) ?: return@withContext Result.failure()
                val files = getFiles(folder, storageManager)

                downloadingFiles = ArrayList(files).apply {
                    // Add folder to mark the sync icon on the selected folder.
                    add(folder)
                }

                val client = getClient()

                var result = true
                files.forEachIndexed { index, file ->
                    if (file.isFolder) {
                        return@forEachIndexed
                    }

                    if (isStopped) {
                        notificationManager.dismiss()
                        return@withContext Result.failure()
                    }

                    if (!checkDiskSize(file)) {
                        return@withContext Result.failure()
                    }

                    withContext(Dispatchers.Main) {
                        notificationManager.showProgressNotification(file.fileName, index, files.size)
                    }

                    val syncFileResult = syncFile(file, client)
                    if (!syncFileResult) {
                        result = false
                    }
                }

                withContext(Dispatchers.Main) {
                    notificationManager.showCompletionMessage(result)
                }

                if (result) {
                    downloadingFiles.remove(folder)
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
            notificationManager.showNotAvailableDiskSpace()
            false
        } else {
            true
        }
    }

    @Suppress("DEPRECATION")
    private fun syncFile(file: OCFile, client: OwnCloudClient): Boolean {
        val operation = DownloadFileOperation(user, file, context).execute(client)
        Log_OC.d(TAG, "Syncing file: " + file.decryptedRemotePath)

        return if (operation.isSuccess) {
            sendFileDownloadCompletionBroadcast(file)
            downloadingFiles.remove(file)
            true
        } else {
            false
        }
    }

    /**
     * It is used to remove the sync icon next to the file in the folder.
     */
    private fun sendFileDownloadCompletionBroadcast(file: OCFile) {
        val intent = Intent(FILE_DOWNLOAD_COMPLETION_BROADCAST).apply {
            putExtra(FILE_PATH, file.decryptedRemotePath)
        }

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    /**
     * This function is called when the download of all files in a folder is complete.
     * It is used to add a green tick icon next to the files after the download process is finished.
     */
    private fun sendSyncWorkerCompletionBroadcast() {
        val intent = Intent(FileDisplayActivity.REFRESH_FOLDER_EVENT_RECEIVER)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }
}
