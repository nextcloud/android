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
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.CreateFolderOperation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
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

    private val scope = CoroutineScope(Dispatchers.IO)
    private val fileDataStorageManager = FileDataStorageManager(user, context.contentResolver)
    private val clientFactory = ClientFactoryImpl(context)

    override suspend fun doWork(): Result {
        val jobName = inputData.getString(JOB_NAME)
        Log_OC.d(
            TAG,
            "$jobName -----------------------------------\nOfflineOperationsWorker started\n-----------------------------------"
        )

        if (!NetworkChangeReceiver.isNetworkAvailable(context)) {
            Log_OC.d(TAG, "OfflineOperationsWorker cancelled, no internet connection.")
            return Result.success()
        }

        val offlineOperations = fileDataStorageManager.offlineOperationDao.getAll()
        if (offlineOperations.isEmpty()) {
            Log_OC.d(TAG, "OfflineOperationsWorker cancelled, no offline operations were found.")
            return Result.success()
        }

        val client = clientFactory.create(user)
        val deferredResults = arrayListOf<Deferred<Pair<Boolean, OfflineOperationEntity>>>()
        scope.launch {
            offlineOperations.forEach { operation ->
                when (operation.type) {
                    OfflineOperationType.CreateFolder -> {
                        deferredResults.add(async { Pair(createFolder(operation, client), operation) })
                    }

                    null -> {
                        Log_OC.d(TAG, "OfflineOperationsWorker terminated, unsupported operation type")
                        deferredResults.add(async { Pair(false, operation) })
                    }
                }
            }
        }

        val results = awaitAll(*deferredResults.toTypedArray())
        results.forEach { (isSuccess, operation) ->
            if (isSuccess) {
                Log_OC.d(
                    TAG,
                    "Create folder operation completed, folder path: ${operation.path}, type: ${operation.type}"
                )
                fileDataStorageManager.offlineOperationDao.delete(operation)
            } else {
                Log_OC.d(
                    TAG,
                    "Create folder operation terminated, folder path: ${operation.path}, type: ${operation.type}"
                )
            }
        }

        Log_OC.d(TAG, "OfflineOperationsWorker successfully completed")
        // TODO update UI after operation completions
        WorkerStateLiveData.instance().setWorkState(WorkerState.OfflineOperationsCompleted)
        return Result.success()
    }

    @Suppress("TooGenericExceptionCaught", "Deprecation")
    private suspend fun createFolder(
        operation: OfflineOperationEntity,
        client: OwnCloudClient
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val createFolderOperation = CreateFolderOperation(operation.path, user, context, fileDataStorageManager)

            try {
                val result = createFolderOperation.execute(client)
                result.isSuccess
            } catch (e: Exception) {
                Log_OC.d(TAG, "Create folder operation terminated, exception is: $e")
                false
            }
        }
    }
}
