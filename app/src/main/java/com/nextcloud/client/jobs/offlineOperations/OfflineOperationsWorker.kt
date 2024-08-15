/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.offlineOperations

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.User
import com.nextcloud.client.database.entity.OfflineOperationEntity
import com.nextcloud.client.network.ClientFactoryImpl
import com.nextcloud.model.OfflineOperationType
import com.nextcloud.model.WorkerState
import com.nextcloud.model.WorkerStateLiveData
import com.nextcloud.receiver.NetworkChangeReceiver
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.CreateFolderOperation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class OfflineOperationsWorker(
    private val user: User,
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private val TAG = OfflineOperationsWorker::class.java.simpleName
        const val JOB_NAME = "job_name"
    }

    private val fileDataStorageManager = FileDataStorageManager(user, context.contentResolver)
    private val clientFactory = ClientFactoryImpl(context)

    override suspend fun doWork(): Result = coroutineScope {
        val jobName = inputData.getString(JOB_NAME)
        Log_OC.d(
            TAG,
            "$jobName -----------------------------------\nOfflineOperationsWorker started\n-----------------------------------"
        )

        if (!NetworkChangeReceiver.isNetworkAvailable(context)) {
            Log_OC.d(TAG, "OfflineOperationsWorker cancelled, no internet connection")
            return@coroutineScope Result.success()
        }

        val isEmpty = fileDataStorageManager.offlineOperationDao.isEmpty()
        if (isEmpty) {
            Log_OC.d(TAG, "OfflineOperationsWorker cancelled, no offline operations were found")
            return@coroutineScope Result.success()
        }

        val offlineOperations = fileDataStorageManager.offlineOperationDao.getAll()
        val client = clientFactory.create(user)
        val operations = offlineOperations.map { operation ->
            async(Dispatchers.IO) {
                when (operation.type) {
                    OfflineOperationType.CreateFolder -> {
                        val result = createFolder(operation, client)
                        Pair(result, operation)
                    }
                    else -> {
                        Pair(null, operation)
                    }
                }
            }
        }

        operations.awaitAll().forEach { (result, operation) ->
            val operationLog = "path: ${operation.path}, type: ${operation.type}"

            if (result?.isSuccess == true) {
                Log_OC.d(TAG, "Operation completed, $operationLog")
                fileDataStorageManager.offlineOperationDao.delete(operation)
            } else if (result?.code == RemoteOperationResult.ResultCode.FOLDER_ALREADY_EXISTS) {
                // TODO check folder conflicts
                Log_OC.d(TAG, "Operation terminated, $operationLog")
            }
        }

        Log_OC.d(TAG, "OfflineOperationsWorker successfully completed")
        WorkerStateLiveData.instance().setWorkState(WorkerState.OfflineOperationsCompleted)
        return@coroutineScope Result.success()
    }

    @Suppress("TooGenericExceptionCaught", "Deprecation")
    private suspend fun createFolder(
        operation: OfflineOperationEntity,
        client: OwnCloudClient
    ): RemoteOperationResult<*>? {
        return withContext(Dispatchers.IO) {
            val createFolderOperation = CreateFolderOperation(operation.path, user, context, fileDataStorageManager)

            try {
                createFolderOperation.execute(client)
            } catch (e: Exception) {
                Log_OC.d(TAG, "Create folder operation terminated, $e")
                null
            }
        }
    }
}
