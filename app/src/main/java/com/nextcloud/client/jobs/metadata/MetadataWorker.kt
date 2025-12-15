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

@Suppress("DEPRECATION", "ReturnCount", "TooGenericExceptionCaught")
class MetadataWorker(private val context: Context, params: WorkerParameters, private val user: User) :
    CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "MetadataWorker"
        const val FILE_PATH = "file_path"
    }

    override suspend fun doWork(): Result {
        val filePath = inputData.getString(FILE_PATH)
        if (filePath == null) {
            Log_OC.e(TAG, "❌ Invalid folder path. Aborting metadata sync. $filePath")
            return Result.failure()
        }

        val storageManager = FileDataStorageManager(user, context.contentResolver)
        val currentDir = storageManager.getFileByDecryptedRemotePath(filePath)
        if (currentDir == null) {
            Log_OC.e(TAG, "❌ Current directory is null. Aborting metadata sync. $filePath")
            return Result.failure()
        }
        if (!currentDir.hasValidParentId()) {
            Log_OC.e(TAG, "❌ Current directory has invalid ID: ${currentDir.fileId}. Path: $filePath")
            return Result.failure()
        }

        Log_OC.d(TAG, "🕒 Starting metadata sync for folder: $filePath, id: ${currentDir.fileId}")

        // First check current dir
        val currentRefreshResult = refreshFolder(currentDir, storageManager)
        if (!currentRefreshResult) {
            Log_OC.e(TAG, "❌ Failed to refresh current directory: $filePath")
            return Result.failure()
        }

        // Re-fetch the folder after refresh to get updated data
        val refreshedDir = storageManager.getFileByPath(filePath)
        if (refreshedDir == null || !refreshedDir.hasValidParentId()) {
            Log_OC.e(TAG, "❌ Directory invalid after refresh. Path: $filePath")
            return Result.failure()
        }

        // then get up-to-date subfolders
        val subfolders = storageManager.getNonEncryptedSubfolders(refreshedDir.fileId, user.accountName)
        Log_OC.d(TAG, "Found ${subfolders.size} subfolders to sync")

        var failedCount = 0
        subfolders.forEach { subFolder ->
            if (!subFolder.hasValidParentId()) {
                Log_OC.e(TAG, "❌ Skipping subfolder with invalid ID: ${subFolder.remotePath}")
                failedCount++
                return@forEach
            }

            val success = refreshFolder(subFolder, storageManager)
            if (!success) {
                failedCount++
            }
        }

        Log_OC.d(TAG, "🏁 Metadata sync completed for folder: $filePath. Failed: $failedCount/${subfolders.size}")

        return Result.success()
    }

    @Suppress("DEPRECATION")
    private suspend fun refreshFolder(folder: OCFile, storageManager: FileDataStorageManager): Boolean =
        withContext(Dispatchers.IO) {
            Log_OC.d(
                TAG,
                "📂 eTag check\n" +
                    "  Path:         " + folder.remotePath + "\n" +
                    "  eTag:         " + folder.etag + "\n" +
                    "  eTagOnServer: " + folder.etagOnServer
            )
            if (!folder.hasValidParentId()) {
                Log_OC.e(TAG, "❌ Folder has invalid ID: ${folder.remotePath}")
                return@withContext false
            }

            if (!folder.isEtagChanged) {
                Log_OC.d(TAG, "Skipping ${folder.remotePath}, eTag didn't change")
                return@withContext true
            }

            Log_OC.d(TAG, "⏳ Fetching metadata for: ${folder.remotePath}, id: ${folder.fileId}")

            val operation = RefreshFolderOperation(folder, storageManager, user, context)
            return@withContext try {
                val result = operation.execute(user, context)
                if (result.isSuccess) {
                    Log_OC.d(TAG, "✅ Successfully fetched metadata for: ${folder.remotePath}")
                    true
                } else {
                    Log_OC.e(TAG, "❌ Failed to fetch metadata for: ${folder.remotePath}")
                    false
                }
            } catch (e: Exception) {
                Log_OC.e(TAG, "❌ Exception refreshing folder ${folder.remotePath}: ${e.message}", e)
                false
            }
        }
}
