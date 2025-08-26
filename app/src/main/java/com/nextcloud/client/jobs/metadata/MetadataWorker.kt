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
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.operations.RefreshFolderOperation

class MetadataWorker(
    private val context: Context,
    params: WorkerParameters,
    private val user: User
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "MetadataWorker"
        const val CURRENT_DIR_PATH = "current_dir_path"
    }

    @Suppress("DEPRECATION")
    override suspend fun doWork(): Result {
        return try {
            val storageManager = FileDataStorageManager(user, context.contentResolver)
            val path = inputData.getString(CURRENT_DIR_PATH) ?: return Result.failure()
            val file = storageManager.getFileByDecryptedRemotePath(path) ?: return Result.failure()
            if (!file.isFolder) {
                Log_OC.e(TAG, "Given file is not folder")
                return Result.failure()
            }

            val subFolders = storageManager.getFolderContent(file, false)
            if (subFolders.isEmpty()) {
                Log_OC.d(TAG, "Subfolders are empty")
                return Result.success()
            }

            subFolders.forEach { subFolder ->
                val operation = RefreshFolderOperation(subFolder, storageManager, user, context)
                val result = operation.executeNextcloudClient(user, context)
                if (result.isSuccess) {
                    val remoteFile = result.data[0] as? RemoteFile
                    Log_OC.d(TAG, "metadata fetched: " + remoteFile?.remotePath)
                } else {
                    Log_OC.e(TAG, "metadata cannot fetched: " + subFolder.remotePath)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log_OC.e(TAG, "Exception: $e")
            Result.failure()
        }
    }
}
