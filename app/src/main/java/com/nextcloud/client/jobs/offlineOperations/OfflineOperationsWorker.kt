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
import com.owncloud.android.operations.RemoveFileOperation
import com.owncloud.android.operations.RenameFileOperation
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val jobName = inputData.getString(JOB_NAME)
        Log_OC.d(
            TAG,
            "$jobName -----------------------------------\n" +
                "OfflineOperationsWorker started" +
                "\n-----------------------------------"
        )

        if (!isNetworkAndServerAvailable()) {
            Log_OC.d(TAG, "OfflineOperationsWorker cancelled, no internet connection")
            return@withContext Result.retry()
        }

        val client = clientFactory.create(user)
        notificationManager.start()

        var operations = fileDataStorageManager.offlineOperationDao.getAll()
        val totalOperations = operations.size
        var currentSuccessfulOperationIndex = 0

        return@withContext try {
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
            notificationManager.dismissNotification()
            Result.failure()
        }
    }

    private suspend fun isNetworkAndServerAvailable(): Boolean = suspendCoroutine { continuation ->
        connectivityService.isNetworkAndServerAvailable { result ->
            continuation.resume(result)
        }
    }

    @Suppress("Deprecation", "MagicNumber")
    private suspend fun executeOperation(
        operation: OfflineOperationEntity,
        client: OwnCloudClient
    ): Pair<RemoteOperationResult<*>?, RemoteOperation<*>?>? = withContext(Dispatchers.IO) {
        return@withContext when (operation.type) {
            is OfflineOperationType.CreateFolder -> {
                val createFolderOperation = withContext(NonCancellable) {
                    val operationType = (operation.type as OfflineOperationType.CreateFolder)
                    CreateFolderOperation(
                        operationType.path,
                        user,
                        context,
                        fileDataStorageManager
                    )
                }
                createFolderOperation.execute(client) to createFolderOperation
            }

            is OfflineOperationType.CreateFile -> {
                val createFileOperation = withContext(NonCancellable) {
                    val operationType = (operation.type as OfflineOperationType.CreateFile)
                    val lastModificationDate = System.currentTimeMillis() / 1000

                    UploadFileRemoteOperation(
                        operationType.localPath,
                        operationType.remotePath,
                        operationType.mimeType,
                        "",
                        operation.modifiedAt ?: lastModificationDate,
                        operation.createdAt ?: System.currentTimeMillis(),
                        true
                    )
                }

                createFileOperation.execute(client) to createFileOperation
            }

            is OfflineOperationType.RenameFile -> {
                val renameFileOperation = withContext(NonCancellable) {
                    val operationType = (operation.type as OfflineOperationType.RenameFile)
                    fileDataStorageManager.getFileById(operationType.ocFileId)?.remotePath?.let { updatedRemotePath ->
                        RenameFileOperation(
                            updatedRemotePath,
                            operationType.newName,
                            fileDataStorageManager
                        )
                    }
                }

                renameFileOperation?.execute(client) to renameFileOperation
            }

            is OfflineOperationType.RemoveFile -> {
                val removeFileOperation = withContext(NonCancellable) {
                    val operationType = (operation.type as OfflineOperationType.RemoveFile)
                    val ocFile = fileDataStorageManager.getFileByDecryptedRemotePath(operationType.path)
                    RemoveFileOperation(ocFile, false, user, true, context, fileDataStorageManager)
                }

                removeFileOperation.execute(client) to removeFileOperation
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
            if (operation.type is OfflineOperationType.RemoveFile) {
                val operationType = operation.type as OfflineOperationType.RemoveFile
                fileDataStorageManager.getFileByDecryptedRemotePath(operationType.path)?.let { ocFile ->
                    repository.deleteOperation(ocFile)
                }
                notificationManager.dismissNotification(operation.id)
            } else {
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
