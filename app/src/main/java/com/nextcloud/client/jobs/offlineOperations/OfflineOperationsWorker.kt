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
import com.nextcloud.utils.extensions.toOCFile
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation
import com.owncloud.android.operations.CreateFolderOperation
import com.owncloud.android.operations.RemoveFileOperation
import com.owncloud.android.operations.RenameFileOperation
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private typealias OfflineOperationResult = Pair<RemoteOperationResult<*>?, RemoteOperation<*>?>?

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

        private const val ONE_SECOND = 1000L
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

        return@withContext try {
            for (index in 0..<operations.size) {
                val operation = operations[index]
                val result = executeOperation(operation, client)
                val isSuccess = handleResult(
                    operation,
                    totalOperations,
                    index,
                    result
                )

                if (!isSuccess) {
                    Log_OC.e(TAG, "Offline operation skipped: $operation")
                    continue
                }
            }

            Log_OC.i(TAG, "OfflineOperationsWorker successfully completed")
            WorkerStateLiveData.instance().setWorkState(WorkerState.OfflineOperationsCompleted)
            Result.success()
        } catch (e: Exception) {
            Log_OC.e(TAG, "OfflineOperationsWorker terminated: $e")
            Result.failure()
        } finally {
            notificationManager.dismissNotification()
        }
    }

    private suspend fun isNetworkAndServerAvailable(): Boolean = suspendCoroutine { continuation ->
        connectivityService.isNetworkAndServerAvailable { result ->
            continuation.resume(result)
        }
    }

    // region Operation Execution
    private suspend fun executeOperation(
        operation: OfflineOperationEntity,
        client: OwnCloudClient
    ): OfflineOperationResult? = withContext(Dispatchers.IO) {
        when (operation.type) {
            is OfflineOperationType.CreateFolder -> createFolder(operation, client)
            is OfflineOperationType.CreateFile -> createFile(operation, client)
            is OfflineOperationType.RenameFile -> renameFile(operation, client)
            is OfflineOperationType.RemoveFile -> removeFile(operation, client)
            else -> {
                Log_OC.d(TAG, "Unsupported operation type: ${operation.type}")
                null
            }
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun createFolder(
        operation: OfflineOperationEntity,
        client: OwnCloudClient
    ): OfflineOperationResult {
        val createFolderOperation = withContext(NonCancellable) {
            val operationType = (operation.type as OfflineOperationType.CreateFolder)
            CreateFolderOperation(
                operationType.path,
                user,
                context,
                fileDataStorageManager
            )
        }

        return createFolderOperation.execute(client) to createFolderOperation
    }

    @Suppress("DEPRECATION")
    private suspend fun createFile(
        operation: OfflineOperationEntity,
        client: OwnCloudClient
    ): OfflineOperationResult {
        val createFileOperation = withContext(NonCancellable) {
            val operationType = (operation.type as OfflineOperationType.CreateFile)
            val lastModificationDate = System.currentTimeMillis() / ONE_SECOND

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

        return createFileOperation.execute(client) to createFileOperation
    }

    @Suppress("DEPRECATION")
    private suspend fun renameFile(
        operation: OfflineOperationEntity,
        client: OwnCloudClient
    ): OfflineOperationResult {
        val operationType = (operation.type as OfflineOperationType.RenameFile)
        val ocFile = fileDataStorageManager.getFileById(operationType.ocFileId)

        if (ocFile == null) {
            Log_OC.w(TAG, "Rename offline operation is cancelled and deleted from the offline operations table")
            fileDataStorageManager.offlineOperationDao.delete(operation)
            return null
        }

        if (isFileChanged(ocFile)) {
            Log_OC.w(TAG, "Cant execute rename operation, file is changed")
            return null
        }

        val ocFileRemotePath = ocFile.remotePath
        if (ocFileRemotePath == null) {
            Log_OC.w(TAG, "Rename offline operation is cancelled, remote path is null")
            return null
        }

        val renameFileOperation = withContext(NonCancellable) {
            RenameFileOperation(ocFileRemotePath, operationType.newName, fileDataStorageManager)
        }

        return renameFileOperation.execute(client) to renameFileOperation
    }

    @Suppress("DEPRECATION")
    private suspend fun removeFile(
        operation: OfflineOperationEntity,
        client: OwnCloudClient
    ): OfflineOperationResult {
        val operationType = (operation.type as OfflineOperationType.RemoveFile)
        val ocFile = fileDataStorageManager.getFileByDecryptedRemotePath(operationType.path)

        if (ocFile == null) {
            Log_OC.w(TAG, "Remove offline operation is cancelled and deleted from the offline operations table")
            fileDataStorageManager.offlineOperationDao.delete(operation)
            return null
        }

        if (isFileChanged(ocFile)) {
            Log_OC.w(TAG, "Cant execute remove operation, file is changed")
            return null
        }

        val removeFileOperation = withContext(NonCancellable) {
            RemoveFileOperation(ocFile, false, user, true, context, fileDataStorageManager)
        }

        return removeFileOperation.execute(client) to removeFileOperation
    }
    // endregion

    private suspend fun handleResult(
        operation: OfflineOperationEntity,
        totalOperations: Int,
        currentSuccessfulOperationIndex: Int,
        result: OfflineOperationResult
    ): Boolean {
        val operationResult = result?.first ?: return false

        val logMessage = if (operationResult.isSuccess == true) "Operation completed" else "Operation failed"
        Log_OC.d(TAG, "$logMessage filename: ${operation.filename}, type: ${operation.type}")

        return if (result.first?.isSuccess == true) {
            handleSuccessResult(operation, totalOperations, currentSuccessfulOperationIndex)
            true
        } else {
            handleErrorResult(result)
            false
        }
    }

    private suspend fun handleSuccessResult(
        operation: OfflineOperationEntity,
        totalOperations: Int,
        currentSuccessfulOperationIndex: Int
    ) {
        if (operation.type is OfflineOperationType.RemoveFile) {
            val operationType = operation.type as OfflineOperationType.RemoveFile
            fileDataStorageManager.getFileByDecryptedRemotePath(operationType.path)?.let { ocFile ->
                repository.deleteOperation(ocFile)
            }
        } else {
            repository.updateNextOperations(operation)
        }

        fileDataStorageManager.offlineOperationDao.delete(operation)

        notificationManager.update(totalOperations, currentSuccessfulOperationIndex, operation.filename ?: "")
        delay(ONE_SECOND)
        notificationManager.dismissNotification(operation.id)
    }

    private fun handleErrorResult(result: OfflineOperationResult) {
        val operationResult = result?.first ?: return
        val operation = result.second ?: return

        val excludedErrorCodes = listOf(RemoteOperationResult.ResultCode.FOLDER_ALREADY_EXISTS)

        if (!excludedErrorCodes.contains(operationResult.code)) {
            notificationManager.showNewNotification(operationResult, operation)
        }
    }

    @Suppress("DEPRECATION")
    private fun isFileChanged(file: OCFile): Boolean {
        val client = ClientFactoryImpl(context).create(user)
        val remoteFile = if (file.isFolder) {
            ReadFolderRemoteOperation(file.remotePath).execute(client)
        } else {
            ReadFileRemoteOperation(file.remotePath).execute(client)
        }

        val remoteETag = remoteFile.toOCFile()?.get(0)?.etag
        return file.etag != remoteETag
    }
}
