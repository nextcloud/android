/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Daniele Verducci <daniele.verducci@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.autoUpload

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.upload.DeleteUploadedFileOperation

class AutoUploadLocalDeletionWorker(
    private val context: Context,
    params: WorkerParameters,
    private val user: User,
    private val storageManager: FileDataStorageManager,
    private val syncedFolderProvider: SyncedFolderProvider,
) : CoroutineWorker (context, params) {

    companion object {
        const val SYNCED_FOLDER_IDS = "synced_folder_IDs"

        private const val TAG = "\uD83D\uDDD1\uFE0F AutoUploadLocalDeletionWorker"
    }

    override suspend fun doWork(): Result {
        // TODO: Notify user the operation started
        Log_OC.d(TAG, "Started")

        val syncedFolderIDs = inputData.getLongArray(SYNCED_FOLDER_IDS)
            ?: throw IllegalArgumentException("$SYNCED_FOLDER_IDS param is mandatory")
        val syncedFolders = syncedFolderIDs.map { syncedFolderProvider.getSyncedFolderByID(it) }

        syncedFolders
            .filterNotNull()
            .filter { it.isEnabled }
            .forEach {
                val op = DeleteUploadedFileOperation(
                    it,
                    user,
                    context,
                    storageManager
                )
                val res = op.run()
                if (res.code != RemoteOperationResult.ResultCode.OK) {
                    Log_OC.d(TAG, "Failed")
                    return Result.failure() // TODO: Notify user the operation failed
                }
            }
        // TODO: Notify user the operation completed
        Log_OC.d(TAG, "Success")
        return Result.success()
    }

}