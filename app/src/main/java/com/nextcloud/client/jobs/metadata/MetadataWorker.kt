/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.metadata

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.User
import com.nextcloud.utils.extensions.getNonEncryptedSubfolders
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.RefreshFolderOperation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MetadataWorker(private val context: Context, params: WorkerParameters, private val user: User) :
    CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "MetadataWorker"
        const val FILE_PATH = "file_path"
    }

    @Suppress("DEPRECATION", "ReturnCount")
    override suspend fun doWork(): Result {
        val storageManager = FileDataStorageManager(user, context.contentResolver)
        val filePath = inputData.getString(FILE_PATH)
        if (filePath == null) {
            Log_OC.e(TAG, "❌ Invalid folder path. Aborting metadata sync. $filePath")
            return Result.failure()
        }
        val currentDir = storageManager.getFileByDecryptedRemotePath(filePath)
        if (currentDir == null) {
            Log_OC.e(TAG, "❌ Current directory is null. Aborting metadata sync. $filePath")
            return Result.failure()
        }
        Log_OC.d(TAG, "🕒 Starting metadata sync for folder: $filePath")

        // first check current dir
        refreshFolder(currentDir, storageManager)

        // then get up-to-date subfolders
        val subfolders = storageManager.getNonEncryptedSubfolders(currentDir.fileId, user.accountName)
        subfolders.forEach { subFolder ->
            refreshFolder(subFolder, storageManager)
        }

        Log_OC.d(TAG, "🏁 Metadata sync completed for folder: $filePath")
        return Result.success()
    }

    @Suppress("DEPRECATION")
    private suspend fun refreshFolder(folder: OCFile, storageManager: FileDataStorageManager) =
        withContext(Dispatchers.IO) {
            Log_OC.d(
                TAG,
                "📂 eTag check\n" +
                    "  Path:        " + folder.remotePath + "\n" +
                    "  eTag:  " + folder.etag + "\n" +
                    "  eTagOnServer: " + folder.etagOnServer
            )
            if (!folder.etag.isNullOrBlank() && !folder.etagOnServer.isNullOrBlank() &&
                folder.etag == folder.etagOnServer
            ) {
                Log_OC.d(TAG, "Skipping ${folder.remotePath}, eTag didn't change")
                return@withContext
            }

            Log_OC.d(TAG, "⏳ Fetching metadata for: ${folder.remotePath}")

            val operation = RefreshFolderOperation(folder, storageManager, user, context)
            val result = operation.execute(user, context)
            if (result.isSuccess) {
                Log_OC.d(TAG, "✅ Successfully fetched metadata for: ${folder.remotePath}")
            } else {
                Log_OC.e(TAG, "❌ Failed to fetch metadata for: ${folder.remotePath}")
            }
        }
}
