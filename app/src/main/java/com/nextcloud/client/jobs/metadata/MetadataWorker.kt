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
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.RefreshFolderOperation

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
        Log_OC.d(TAG, "🕒 Starting metadata sync for folder filePath: $filePath")

        val subfolders = storageManager.getNonEncryptedSubfolders(currentDir.fileId, user.accountName)
        val subFoldersAndFolderItself = listOf(currentDir) + subfolders
        subFoldersAndFolderItself.forEach { subFolder ->
            if (subFolder.etag == subFolder.etagOnServer) {
                Log_OC.d(TAG, "Skipping ${subFolder.remotePath}, eTag didn't change")
                return@forEach
            }

            Log_OC.d(TAG, "⏳ Fetching metadata for: ${subFolder.remotePath}")

            val operation = RefreshFolderOperation(subFolder, storageManager, user, context)
            val result = operation.execute(user, context)
            if (result.isSuccess) {
                Log_OC.d(TAG, "✅ Successfully fetched metadata for: ${subFolder.remotePath}")
            } else {
                Log_OC.e(TAG, "❌ Failed to fetch metadata for: ${subFolder.remotePath}")
            }
        }

        Log_OC.d(TAG, "🏁 Metadata sync completed for folder ID: $id")
        return Result.success()
    }
}
