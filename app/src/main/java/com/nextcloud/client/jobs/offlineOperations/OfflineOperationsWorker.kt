/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.offlineOperations

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.User
import com.nextcloud.client.network.ClientFactoryImpl
import com.nextcloud.model.OfflineOperationType
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.CreateFolderOperation

class OfflineOperationsWorker(
    private val user: User,
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private val TAG = OfflineOperationsWorker::class.java.simpleName
    }

    private val fileDataStorageManager = FileDataStorageManager(user, context.contentResolver)
    private val clientFactory = ClientFactoryImpl(context)

    override suspend fun doWork(): Result {
        val client = clientFactory.create(user)

        val offlineOperations = fileDataStorageManager.offlineOperationDao.getAll()
        offlineOperations.forEach { operation ->
            when (operation.type) {
                is OfflineOperationType.CreateFolder -> {
                    val createFolderOperation = (operation.type as OfflineOperationType.CreateFolder)
                    createFolder(createFolderOperation, client, onCompleted = {
                        fileDataStorageManager.offlineOperationDao.delete(operation)
                    })
                }

                null -> {
                    Log_OC.d(TAG, "OfflineOperationsWorker terminated, unsupported operation type")
                    return Result.failure()
                }
            }
        }

        Log_OC.d(TAG, "OfflineOperationsWorker successfully completed")
        return Result.success()
    }

    @Suppress("TooGenericExceptionCaught", "Deprecation")
    private fun createFolder(
        operation: OfflineOperationType.CreateFolder,
        client: OwnCloudClient,
        onCompleted: () -> Unit
    ) {
        val createFolderOperation = CreateFolderOperation(operation.path, user, context, fileDataStorageManager)

        try {
            val result = createFolderOperation.execute(client)
            if (result.isSuccess) {
                Log_OC.d(TAG, "Create folder operation completed, folder path: ${operation.path}")
                onCompleted()
            } else {
                Log_OC.d(TAG, "Create folder operation terminated, result: $result")
            }
        } catch (e: Exception) {
            Log_OC.d(TAG, "Error caught at createFolder: $e")
        }
    }
}
