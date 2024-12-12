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
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.DownloadFileOperation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

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

        private var downloadingFilePaths = ArrayList<String>()

        /**
         * It is used to add the sync icon next to the file in the folder.
         */
        fun isDownloading(path: String): Boolean {
            return downloadingFilePaths.contains(path)
        }
    }

    private val notificationManager = SyncWorkerNotificationManager(context)

    @Suppress("DEPRECATION", "MagicNumber")
    override suspend fun doWork(): Result {
        withContext(Dispatchers.Main) {
            notificationManager.showStartNotification()
        }

        return withContext(Dispatchers.IO) {
            Log_OC.d(TAG, "SyncWorker started")
            val filePaths = inputData.getStringArray(FILE_PATHS)

            if (filePaths.isNullOrEmpty()) {
                return@withContext Result.success()
            }

            val fileDataStorageManager = FileDataStorageManager(user, context.contentResolver)

            // Add the topParentPath to mark the sync icon on the selected folder.
            val topParentPath = getTopParentPath(fileDataStorageManager, filePaths.first())
            downloadingFilePaths = ArrayList(filePaths.toList()).apply {
                add(topParentPath)
            }

            val account = user.toOwnCloudAccount()
            val client = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(account, context)

            var result = true
            filePaths.forEachIndexed { index, path ->
                if (isStopped) {
                    notificationManager.dismiss()
                    return@withContext Result.failure()
                }

                fileDataStorageManager.getFileByDecryptedRemotePath(path)?.let { file ->
                    withContext(Dispatchers.Main) {
                        notificationManager.showProgressNotification(file.fileName, index, filePaths.size)
                    }

                    delay(1000)

                    val operation = DownloadFileOperation(user, file, context).execute(client)
                    Log_OC.d(TAG, "Syncing file: " + file.decryptedRemotePath)

                    if (operation.isSuccess) {
                        sendFileDownloadCompletionBroadcast(path)
                        downloadingFilePaths.remove(path)
                    } else {
                        result = false
                    }
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
        }
    }

    private fun getTopParentPath(fileDataStorageManager: FileDataStorageManager, firstFilePath: String): String {
        val firstFile = fileDataStorageManager.getFileByDecryptedRemotePath(firstFilePath)
        val topParentFile = fileDataStorageManager.getTopParent(firstFile)
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
