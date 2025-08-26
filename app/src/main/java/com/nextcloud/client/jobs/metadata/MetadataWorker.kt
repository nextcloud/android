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
        const val CURRENT_DIR_ID = "current_dir_id"
    }

    @Suppress("DEPRECATION")
    override suspend fun doWork(): Result {
        val storageManager = FileDataStorageManager(user, context.contentResolver)
        val id = inputData.getLong(CURRENT_DIR_ID, -1L)
        if (id == -1L) {
            Log_OC.e(TAG, "❌ folder id is not valid")
            return Result.failure()
        }

        val subFolders = storageManager.getSubfoldersById(id)
        if (subFolders.isEmpty()) {
            Log_OC.d(TAG, "✅ Subfolders are empty")
            return Result.success()
        }
        val subfoldersToBeSynced = subFolders.filter { !it.isEncrypted }

        subfoldersToBeSynced.forEach { subFolder ->
            Log_OC.d(TAG, "🕛 fetching metadata: " + subFolder.remotePath)

            val operation = RefreshFolderOperation(subFolder, storageManager, user, context)
            val result = operation.execute(user, context)
            if (result.isSuccess) {
                Log_OC.d(TAG, "✅ metadata fetched: " + subFolder.remotePath)
            } else {
                Log_OC.e(TAG, "❌ metadata cannot fetched: " + subFolder.remotePath)
            }
        }

        return Result.success()
    }
}
