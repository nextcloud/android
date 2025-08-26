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
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.RefreshFolderOperation

class MetadataWorker(private val context: Context, params: WorkerParameters, private val user: User) :
    CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "MetadataWorker"
        const val FILE_ID = "file_id"
    }

    @Suppress("DEPRECATION")
    override suspend fun doWork(): Result {
        val storageManager = FileDataStorageManager(user, context.contentResolver)
        val id = inputData.getLong(FILE_ID, -1L)
        if (id == -1L) {
            Log_OC.e(TAG, "❌ Invalid folder ID. Aborting metadata sync.")
            return Result.failure()
        }
        Log_OC.d(TAG, "🕒 Starting metadata sync for folder ID: $id")

        val subfolders = storageManager.getSubFiles(id).filter { !it.isEncrypted && it.isFolder }
        if (subfolders.isEmpty()) {
            Log_OC.d(TAG, "📂 No subfolders found for folder ID: $id. Nothing to sync.")
            return Result.success()
        }

        subfolders.forEach { subFolder ->
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
