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

class SyncWorker(
    private val user: User,
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val FILE_PATHS = "FILE_PATHS"
        private const val TAG = "SyncWorker"
    }

    @Suppress("DEPRECATION")
    override suspend fun doWork(): Result {
        Log_OC.d(TAG, "SyncWorker started")
        val filePaths = inputData.getStringArray(FILE_PATHS)

        if (filePaths.isNullOrEmpty()) {
            return Result.failure()
        }

        val fileDataStorageManager = FileDataStorageManager(user, context.contentResolver)

        val account = user.toOwnCloudAccount()
        val client = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(account, context)

        var result = true
        filePaths.forEach { path ->
            fileDataStorageManager.getFileByDecryptedRemotePath(path)?.let { file ->
                val operation = DownloadFileOperation(user, file, context).execute(client)
                Log_OC.d(TAG, "Syncing file: " + file.decryptedRemotePath)
                if (!operation.isSuccess) {
                    result = false
                }
            }
        }

        return if (result) {
            Log_OC.d(TAG, "SyncWorker completed")
            Result.success()
        } else {
            Log_OC.d(TAG, "SyncWorker failed")
            Result.failure()
        }
    }
}
