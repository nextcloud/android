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
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.model.OfflineOperationType
import com.nextcloud.model.WorkerState
import com.nextcloud.model.WorkerStateLiveData
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.CreateFolderOperation
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class OfflineOperationsWorker(
    private val user: User,
    private val context: Context,
    private val connectivityService: ConnectivityService,
    viewThemeUtils: ViewThemeUtils,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private val TAG = OfflineOperationsWorker::class.java.simpleName
        const val JOB_NAME = "job_name"
    }

    private val fileDataStorageManager = FileDataStorageManager(user, context.contentResolver)
    private val clientFactory = ClientFactoryImpl(context)
    private val notificationManager = OfflineOperationsNotificationManager(context, viewThemeUtils)

    @Suppress("TooGenericExceptionCaught", "Deprecation")
    override suspend fun doWork(): Result = coroutineScope {
        val jobName = inputData.getString(JOB_NAME)
        Log_OC.d(
            TAG,
            "$jobName -----------------------------------\n" +
                "OfflineOperationsWorker started" +
                "\n-----------------------------------"
        )

        if (!connectivityService.isNetworkAndServerAvailable()) {
            Log_OC.d(TAG, "OfflineOperationsWorker cancelled, no internet connection")
            return@coroutineScope Result.success()
        }

        val isEmpty = fileDataStorageManager.offlineOperationDao.isEmpty()
        if (isEmpty) {
            Log_OC.d(TAG, "OfflineOperationsWorker cancelled, no offline operations were found")
            return@coroutineScope Result.success()
        }

        val client = clientFactory.create(user)
        val operations = fileDataStorageManager.offlineOperationDao.getAll()

        notificationManager.start()

        operations.forEachIndexed { index, operation ->
            val result = try {
                if (operation.type == OfflineOperationType.CreateFolder && operation.parentPath != null) {
                    val createFolderOperation = async(Dispatchers.IO) {
                        CreateFolderOperation(
                            operation.path,
                            user,
                            context,
                            fileDataStorageManager
                        )
                    }.await()

                    createFolderOperation.execute(client) to createFolderOperation
                } else {
                    Log_OC.d(TAG, "Operation terminated, not supported or incomplete operation")
                    null
                }
            } catch (e: Exception) {
                Log_OC.d(TAG, "Operation terminated, exception caught: $e")
                null
            }

            handleResult(operation, operations, index, result?.first, result?.second)
        }

        Log_OC.d(TAG, "OfflineOperationsWorker successfully completed")
        notificationManager.dismissNotification()
        WorkerStateLiveData.instance().setWorkState(WorkerState.OfflineOperationsCompleted)
        return@coroutineScope Result.success()
    }

    private fun handleResult(
        operation: OfflineOperationEntity,
        operations: List<OfflineOperationEntity>,
        currentOperationIndex: Int,
        result: RemoteOperationResult<*>?,
        remoteOperation: RemoteOperation<*>?
    ) {
        result ?: return Log_OC.d(TAG, "Operation not completed, result is null")

        val logMessage = if (result.isSuccess) "Operation completed" else "Operation terminated"
        Log_OC.d(TAG, "$logMessage path: ${operation.path}, type: ${operation.type}")

        if (result.isSuccess) {
            fileDataStorageManager.offlineOperationDao.run {
                operation.id?.let { id ->
                    operation.path?.let { path ->
                        updateParentPathById(operation.id, path)
                    }
                }

                delete(operation)
            }

            notificationManager.update(operations.size, currentOperationIndex, operation.filename ?: "")
        } else {
            val excludedErrorCodes = listOf(RemoteOperationResult.ResultCode.FOLDER_ALREADY_EXISTS)

            if (remoteOperation != null && !excludedErrorCodes.contains(result.code)) {
                notificationManager.showNewNotification(result, remoteOperation)
            }
        }
    }
}
