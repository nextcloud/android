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
        const val TOP_PARENT_PATH = "TOP_PARENT_PATH"

        private var downloadingFilePaths = ArrayList<String>()

        fun isDownloading(path: String): Boolean {
            return downloadingFilePaths.contains(path)
        }
    }

    private val notificationManager = SyncWorkerNotificationManager(context)

    @Suppress("DEPRECATION")
    override suspend fun doWork(): Result {
        withContext(Dispatchers.Main) {
            notificationManager.showStartNotification()
        }

        return withContext(Dispatchers.IO) {
            Log_OC.d(TAG, "SyncWorker started")
            val filePaths = inputData.getStringArray(FILE_PATHS)
            val topParentPath = inputData.getString(TOP_PARENT_PATH)

            if (filePaths.isNullOrEmpty() || topParentPath.isNullOrEmpty()) {
                return@withContext Result.failure()
            }

            downloadingFilePaths = ArrayList(filePaths.toList()).apply {
                add(topParentPath)
            }

            val fileDataStorageManager = FileDataStorageManager(user, context.contentResolver)

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
                        downloadingFilePaths.remove(path)
                    } else {
                        result = false
                    }
                }
            }

            // TODO add notify isDownloading for adapter
            // TODO add cancel only one file download
            withContext(Dispatchers.Main) {
                notificationManager.showCompletionMessage(result)
            }

            if (result) {
                downloadingFilePaths.remove(topParentPath)
                Log_OC.d(TAG, "SyncWorker completed")
                Result.success()
            } else {
                Log_OC.d(TAG, "SyncWorker failed")
                Result.failure()
            }
        }
    }
}
