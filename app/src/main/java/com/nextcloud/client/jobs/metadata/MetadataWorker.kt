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

        if (user.isAnonymous) {
            Log_OC.w(TAG, "user is anonymous cannot start metadata worker")
            return Result.failure()
        }

        val storageManager = FileDataStorageManager(user, context.contentResolver)
        val currentDir = storageManager.getFileByDecryptedRemotePath(filePath)
        if (!currentDir.canFetch()) {
            return Result.failure()
        }

        Log_OC.d(TAG, "🕒 Starting metadata sync for folder: $filePath, id: ${currentDir?.fileId}")

        // First check current dir
        val currentRefreshResult = refreshFolder(currentDir!!, storageManager)
        if (!currentRefreshResult) {
            return Result.failure()
        }

        // Re-fetch the folder after refresh to get updated data
        val refreshedDir = storageManager.getFileByPath(filePath)
        if (!refreshedDir.canFetch()) {
            return Result.failure()
        }

        // then get up-to-date subfolders
        val subfolders = storageManager.getNonEncryptedSubfolders(refreshedDir.fileId, user.accountName)
        Log_OC.d(TAG, "Found ${subfolders.size} subfolders to sync")

        var failedCount = 0
        for (subfolder in subfolders) {
            val success = refreshFolder(subfolder, storageManager)
            if (!success) {
                failedCount++
            }
        }

        Log_OC.d(TAG, "🏁 Metadata sync completed for folder: $filePath. Failed: $failedCount/${subfolders.size}")

        return Result.success()
    }

    private fun OCFile?.canFetch(): Boolean {
        if (this == null) {
            Log_OC.e(TAG, "file is null")
            return false
        }

        if (!hasValidParentId()) {
            Log_OC.e(TAG, "❌ Folder has invalid ID: $remotePath")
            return false
        }

        if (isEncrypted) {
            Log_OC.d(TAG, "skipping encrypted folder")
            return false
        }

        return true
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
            if (!folder.canFetch()) {
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
