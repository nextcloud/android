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
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.operations.CreateFolderOperation
import com.owncloud.android.operations.RemoveFileOperation
import com.owncloud.android.operations.RenameFileOperation
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
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

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val jobName = inputData.getString(JOB_NAME)
            Log_OC.d(TAG, "[$jobName] OfflineOperationsWorker started for user: ${user.accountName}")

            // check network connection
            if (!isNetworkAndServerAvailable()) {
                Log_OC.w(TAG, "⚠️ No internet/server connection. Retrying later...")
                return@withContext Result.retry()
            }

            // check offline operations
            val operations = fileDataStorageManager.offlineOperationDao.getAll()
            if (operations.isEmpty()) {
                Log_OC.d(TAG, "Skipping, no offline operation found")
                return@withContext Result.success()
            }

            // process offline operations
            notificationManager.start()
            val client = clientFactory.create(user)
            processOperations(operations, client)

            // finish
            WorkerStateLiveData.instance().setWorkState(WorkerState.OfflineOperationsCompleted)
            Log_OC.d(TAG, "🏁 Worker finished with result")
            return@withContext Result.success()
        } catch (e: Exception) {
            Log_OC.e(TAG, "💥 ProcessOperations failed: ${e.message}")
            return@withContext Result.failure()
        } finally {
            notificationManager.dismissNotification()
        }
    }

    // region Handle offline operations
    @Suppress("TooGenericExceptionCaught")
    private suspend fun processOperations(operations: List<OfflineOperationEntity>, client: OwnCloudClient) {
        val totalOperationSize = operations.size
        operations.forEachIndexed { index, operation ->
            try {
                Log_OC.d(TAG, "Processing operation, path: ${operation.path}")
                val result = executeOperation(operation, client)
                handleResult(operation, totalOperationSize, index, result)
            } catch (e: Exception) {
                Log_OC.e(TAG, "💥 Exception while processing operation id=${operation.id}: ${e.message}")
            }
        }
    }

    private fun handleResult(
        operation: OfflineOperationEntity,
        totalOperations: Int,
        currentSuccessfulOperationIndex: Int,
        result: OfflineOperationResult
    ) {
        val operationResult = result?.first ?: return
        val logMessage = if (operationResult.isSuccess) "Operation completed" else "Operation failed"
        Log_OC.d(TAG, "$logMessage filename: ${operation.filename}, type: ${operation.type}")

        return if (result.first?.isSuccess == true) {
            handleSuccessResult(operation, totalOperations, currentSuccessfulOperationIndex)
        } else {
            handleErrorResult(operation.id, result)
        }
    }

    private fun handleSuccessResult(
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
        notificationManager.update(totalOperations, currentSuccessfulOperationIndex + 1, operation.filename ?: "")
    }

    private fun handleErrorResult(id: Int?, result: OfflineOperationResult) {
        val operationResult = result?.first ?: return
        val operation = result.second ?: return
        Log_OC.e(TAG, "❌ Operation failed [id=$id]: code=${operationResult.code}, message=${operationResult.message}")
        val excludedErrorCodes =
            listOf(RemoteOperationResult.ResultCode.FOLDER_ALREADY_EXISTS, RemoteOperationResult.ResultCode.LOCKED)

        if (!excludedErrorCodes.contains(operationResult.code)) {
            notificationManager.showNewNotification(id, operationResult, operation)
        } else {
            Log_OC.d(TAG, "ℹ️ Ignored error: ${operationResult.code}")
        }
    }
    // endregion

    private suspend fun isNetworkAndServerAvailable(): Boolean = suspendCoroutine { continuation ->
        connectivityService.isNetworkAndServerAvailable { result ->
            continuation.resume(result)
        }
    }

    // region Operation Execution
    @Suppress("ComplexCondition")
    private suspend fun executeOperation(
        operation: OfflineOperationEntity,
        client: OwnCloudClient
    ): OfflineOperationResult? = withContext(Dispatchers.IO) {
        var path = (operation.path)
        if (path == null) {
            Log_OC.w(TAG, "⚠️ Skipped: path is null for operation id=${operation.id}")
            return@withContext null
        }

        if (operation.type is OfflineOperationType.CreateFile && path.endsWith(OCFile.PATH_SEPARATOR)) {
            Log_OC.w(
                TAG,
                "Create file operation should not ends with path separator removing suffix, operation id=${operation.id}"
            )
            path = path.removeSuffix(OCFile.PATH_SEPARATOR)
        }

        val remoteFile = getRemoteFile(path)
        val ocFile = fileDataStorageManager.getFileByDecryptedRemotePath(path)

        if (remoteFile != null && ocFile != null && isFileChanged(remoteFile, ocFile)) {
            Log_OC.w(TAG, "⚠️ Conflict detected: File already exists on server. Skipping operation id=${operation.id}")

            if (operation.isRenameOrRemove()) {
                Log_OC.d(TAG, "🗑 Removing conflicting rename/remove operation id=${operation.id}")
                fileDataStorageManager.offlineOperationDao.delete(operation)
                notificationManager.showConflictNotificationForDeleteOrRemoveOperation(operation)
            } else {
                Log_OC.d(TAG, "📌 Showing conflict resolution for operation id=${operation.id}")
                notificationManager.showConflictResolveNotification(ocFile, operation)
            }

            return@withContext null
        }

        if (operation.isRenameOrRemove() && ocFile == null) {
            Log_OC.d(TAG, "Skipping, attempting to delete or rename non-existing file")
            fileDataStorageManager.offlineOperationDao.delete(operation)
            return@withContext null
        }

        if (operation.isCreate() && remoteFile != null && ocFile != null && !isFileChanged(remoteFile, ocFile)) {
            Log_OC.d(TAG, "Skipping, attempting to create same file creation")
            fileDataStorageManager.offlineOperationDao.delete(operation)
            return@withContext null
        }

        return@withContext when (val type = operation.type) {
            is OfflineOperationType.CreateFolder -> {
                Log_OC.d(TAG, "📂 Creating folder at ${type.path}")
                createFolder(operation, client)
            }
            is OfflineOperationType.CreateFile -> {
                Log_OC.d(TAG, "📤 Uploading file: local=${type.localPath} → remote=${type.remotePath}")
                createFile(operation, client)
            }
            is OfflineOperationType.RenameFile -> {
                Log_OC.d(TAG, "✏️ Renaming ${operation.path} → ${type.newName}")
                renameFile(operation, client)
            }
            is OfflineOperationType.RemoveFile -> {
                Log_OC.d(TAG, "🗑 Removing file: ${operation.path}")
                ocFile?.let { removeFile(it, client) }
            }
            else -> {
                Log_OC.d(TAG, "⚠️ Unsupported operation type: $type")
                null
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun createFolder(operation: OfflineOperationEntity, client: OwnCloudClient): OfflineOperationResult {
        val operationType = (operation.type as OfflineOperationType.CreateFolder)
        val createFolderOperation = CreateFolderOperation(operationType.path, user, context, fileDataStorageManager)
        return createFolderOperation.execute(client) to createFolderOperation
    }

    @Suppress("DEPRECATION")
    private fun createFile(operation: OfflineOperationEntity, client: OwnCloudClient): OfflineOperationResult {
        val operationType = (operation.type as OfflineOperationType.CreateFile)
        val lastModificationDate = System.currentTimeMillis() / ONE_SECOND
        val createFileOperation = UploadFileRemoteOperation(
            operationType.localPath,
            operationType.remotePath,
            operationType.mimeType,
            "",
            operation.modifiedAt ?: lastModificationDate,
            operation.createdAt ?: System.currentTimeMillis(),
            true
        )
        return createFileOperation.execute(client) to createFileOperation
    }

    @Suppress("DEPRECATION")
    private fun renameFile(operation: OfflineOperationEntity, client: OwnCloudClient): OfflineOperationResult {
        val operationType = (operation.type as OfflineOperationType.RenameFile)
        val renameFileOperation = RenameFileOperation(operation.path, operationType.newName, fileDataStorageManager)
        return renameFileOperation.execute(client) to renameFileOperation
    }

    @Suppress("DEPRECATION")
    private fun removeFile(ocFile: OCFile, client: OwnCloudClient): OfflineOperationResult {
        val removeFileOperation = RemoveFileOperation(ocFile, false, user, true, context, fileDataStorageManager)
        return removeFileOperation.execute(client) to removeFileOperation
    }
    // endregion

    @Suppress("DEPRECATION")
    private fun getRemoteFile(remotePath: String): RemoteFile? {
        val mimeType = MimeTypeUtil.getMimeTypeFromPath(remotePath)
        val isFolder = MimeTypeUtil.isFolder(mimeType)
        val client = ClientFactoryImpl(context).create(user)
        val result = if (isFolder) {
            ReadFolderRemoteOperation(remotePath).execute(client)
        } else {
            ReadFileRemoteOperation(remotePath).execute(client)
        }

        return if (result.isSuccess) {
            result.data[0] as? RemoteFile
        } else {
            null
        }
    }

    private fun isFileChanged(remoteFile: RemoteFile, ocFile: OCFile): Boolean = remoteFile.etag != ocFile.etagOnServer
}
