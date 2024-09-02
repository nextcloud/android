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
import com.nextcloud.client.jobs.offlineOperations.repository.OfflineOperationsRepository
import com.nextcloud.client.network.ClientFactoryImpl
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.model.OfflineOperationType
import com.nextcloud.model.WorkerState
import com.nextcloud.model.WorkerStateLiveData
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation
import com.owncloud.android.operations.CreateFolderOperation
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class OfflineOperationsWorker(
    private val user: User,
    private val context: Context,
    private val connectivityService: ConnectivityService,
    viewThemeUtils: ViewThemeUtils,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private val TAG = OfflineOperationsWorker::class.java.simpleName
        const val JOB_NAME = "JOB_NAME"
    }

    private val fileDataStorageManager = FileDataStorageManager(user, context.contentResolver)
    private val clientFactory = ClientFactoryImpl(context)
    private val notificationManager = OfflineOperationsNotificationManager(context, viewThemeUtils)
    private var repository = OfflineOperationsRepository(fileDataStorageManager)

    @Suppress("TooGenericExceptionCaught")
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
            return@coroutineScope Result.retry()
        }

        val client = clientFactory.create(user)
        notificationManager.start()

        var operations = fileDataStorageManager.offlineOperationDao.getAll()
        val totalOperations = operations.size
        var currentSuccessfulOperationIndex = 0

        return@coroutineScope try {
            while (operations.isNotEmpty()) {
                val operation = operations.first()
                val result = executeOperation(operation, client)
                val isSuccess = handleResult(
                    operation,
                    totalOperations,
                    currentSuccessfulOperationIndex,
                    result?.first,
                    result?.second
                )

                operations = if (isSuccess) {
                    currentSuccessfulOperationIndex++
                    fileDataStorageManager.offlineOperationDao.getAll()
                } else {
                    operations.filter { it != operation }
                }
            }

            Log_OC.d(TAG, "OfflineOperationsWorker successfully completed")
            notificationManager.dismissNotification()
            WorkerStateLiveData.instance().setWorkState(WorkerState.OfflineOperationsCompleted)
            Result.success()
        } catch (e: Exception) {
            Log_OC.d(TAG, "OfflineOperationsWorker terminated: $e")
            Result.failure()
        }
    }

    @Suppress("Deprecation")
    private suspend fun executeOperation(
        operation: OfflineOperationEntity,
        client: OwnCloudClient
    ): Pair<RemoteOperationResult<*>?, RemoteOperation<*>?>? {
        return when (operation.type) {
            is OfflineOperationType.CreateFolder -> {
                if (operation.parentPath != null) {
                    val createFolderOperation = withContext(Dispatchers.IO) {
                        val operationType = (operation.type as OfflineOperationType.CreateFolder)
                        CreateFolderOperation(
                            operationType.path,
                            user,
                            context,
                            fileDataStorageManager
                        )
                    }
                    createFolderOperation.execute(client) to createFolderOperation
                } else {
                    Log_OC.d(TAG, "CreateFolder operation incomplete, missing parentPath")
                    null
                }
            }

            is OfflineOperationType.CreateFile -> {
                val createFileOperation = withContext(Dispatchers.IO) {
                    val operationType = (operation.type as OfflineOperationType.CreateFile)
                    UploadFileRemoteOperation(operationType.localPath, operationType.remotePath, operationType.mimeType, System.currentTimeMillis())
                }

                createFileOperation.execute(client) to createFileOperation
            }

            else -> {
                Log_OC.d(TAG, "Unsupported operation type: ${operation.type}")
                null
            }
        }
    }

    private fun handleResult(
        operation: OfflineOperationEntity,
        totalOperations: Int,
        currentSuccessfulOperationIndex: Int,
        result: RemoteOperationResult<*>?,
        remoteOperation: RemoteOperation<*>?
    ): Boolean {
        if (result == null) {
            Log_OC.d(TAG, "Operation not completed, result is null")
            return false
        }

        val logMessage = if (result.isSuccess) "Operation completed" else "Operation failed"
        Log_OC.d(TAG, "$logMessage filename: ${operation.filename}, type: ${operation.type}")

        if (result.isSuccess) {
            if (operation.type is OfflineOperationType.CreateFolder) {
                repository.updateNextOperations(operation)
            }

            fileDataStorageManager.offlineOperationDao.delete(operation)
            notificationManager.update(totalOperations, currentSuccessfulOperationIndex, operation.filename ?: "")
        } else {
            val excludedErrorCodes = listOf(RemoteOperationResult.ResultCode.FOLDER_ALREADY_EXISTS)

            if (remoteOperation != null && !excludedErrorCodes.contains(result.code)) {
                notificationManager.showNewNotification(result, remoteOperation)
            }
        }

        return result.isSuccess
    }
}
