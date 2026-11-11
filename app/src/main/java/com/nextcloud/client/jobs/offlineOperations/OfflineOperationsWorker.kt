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
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.model.OfflineOperationType
import com.nextcloud.repository.ClientRepository
import com.nextcloud.utils.extensions.isNetworkAndServerAvailableSuspended
import com.nextcloud.utils.extensions.isTheSameAs
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
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.FileUtil
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.nextcloud.utils.AppConstants.MILLIS_PER_SECOND
import java.io.File

private typealias OfflineOperationResult = Pair<RemoteOperationResult<*>?, RemoteOperation<*>?>?

class OfflineOperationsWorker(
    private val user: User,
    private val context: Context,
    private val connectivityService: ConnectivityService,
    private val clientRepository: ClientRepository,
    viewThemeUtils: ViewThemeUtils,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private val TAG = OfflineOperationsWorker::class.java.simpleName
        const val JOB_NAME = "JOB_NAME"
    }

    private val fileDataStorageManager = FileDataStorageManager(user, context.contentResolver)
    private val notificationManager = OfflineOperationsNotificationManager(context, viewThemeUtils)
    private var repository = OfflineOperationsRepository(fileDataStorageManager)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val jobName = inputData.getString(JOB_NAME)
            Log_OC.d(TAG, "[$jobName] OfflineOperationsWorker started for user: ${user.accountName}")

            // check network connection
            if (!connectivityService.isNetworkAndServerAvailableSuspended()) {
                Log_OC.w(TAG, "⚠️ No internet/server connection. Waiting for the next trigger...")
                return@withContext Result.success()
            }

            // check offline operations
            val operations = fileDataStorageManager.offlineOperationDao.getAll()
            if (operations.isEmpty()) {
                Log_OC.d(TAG, "Skipping, no offline operation found")
                return@withContext Result.success()
            }

            // process offline operations
            val client = clientRepository.getOwncloudClient()
            if (client == null) {
                Log_OC.e(TAG, "💥 Client is not available, operations are not processed")
                return@withContext Result.failure()
            }

            notificationManager.start()
            processOperations(operations, client)

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
    private suspend fun processOperations(operations: List<OfflineOperationEntity>, client: OwnCloudClient) {
        val totalOperationSize = operations.size
        operations.forEachIndexed { index, snapshot ->
            val operation = snapshot.id?.let { fileDataStorageManager.offlineOperationDao.getById(it) }
            if (operation == null) {
                Log_OC.d(TAG, "Skipping, operation was removed during this run, id=${snapshot.id}")
                return@forEachIndexed
            }

            try {
                Log_OC.d(TAG, "Processing operation, path: ${operation.path}")
                val result = executeOperation(operation, client)
                handleResult(operation, totalOperationSize, index, result, client)
            } catch (e: Exception) {
                Log_OC.e(TAG, "💥 Exception while processing operation id=${operation.id}: ${e.message}")
            }
        }
    }

    private fun handleResult(
        operation: OfflineOperationEntity,
        totalOperations: Int,
        currentSuccessfulOperationIndex: Int,
        result: OfflineOperationResult,
        client: OwnCloudClient
    ) {
        val operationResult = result?.first ?: return
        val logMessage = if (operationResult.isSuccess) "Operation completed" else "Operation failed"
        Log_OC.d(TAG, "$logMessage filename: ${operation.filename}, type: ${operation.type}")

        return if (result.first?.isSuccess == true) {
            handleSuccessResult(operation, totalOperations, currentSuccessfulOperationIndex, client)
        } else {
            handleErrorResult(operation.id, result)
        }
    }

    private fun handleSuccessResult(
        operation: OfflineOperationEntity,
        totalOperations: Int,
        currentSuccessfulOperationIndex: Int,
        client: OwnCloudClient
    ) {
        val operationType = operation.type
        if (operationType is OfflineOperationType.RemoveFile) {
            if (operationType.path.endsWith(OCFile.PATH_SEPARATOR)) {
                repository.deleteOperationsInsideFolder(operationType.path)
                    .forEach { notificationManager.dismissNotification(it) }
            }
        } else {
            if (operation.isCreate()) {
                getExecutionPath(operation)?.let { getRemoteFile(it, client) }?.let { saveRemoteFile(it) }
            }
            repository.updateNextOperations(operation)
        }

        deleteOperation(operation)
        notificationManager.update(totalOperations, currentSuccessfulOperationIndex + 1, operation.filename ?: "")
    }

    private fun deleteOperation(operation: OfflineOperationEntity) {
        fileDataStorageManager.offlineOperationDao.delete(operation)
        notificationManager.dismissNotification(operation.id)
    }

    private fun handleExistingRemoteFile(
        operation: OfflineOperationEntity,
        localPath: String,
        remoteFile: RemoteFile,
        ocFile: OCFile?
    ) {
        val serverFile = FileStorageUtils.fillOCFile(remoteFile)
        if (serverFile.isTheSameAs(File(localPath))) {
            Log_OC.d(TAG, "Skipping, same file already exists on server: ${operation.path}")
            saveRemoteFile(remoteFile)
            deleteOperation(operation)
            return
        }

        Log_OC.w(TAG, "⚠️ Conflict detected: different file already exists on server, operation id=${operation.id}")
        ocFile?.let {
            serverFile.fileId = it.fileId
            serverFile.parentId = it.parentId
        }
        notificationManager.showConflictResolveNotification(serverFile, operation)
    }

    private fun saveRemoteFile(remoteFile: RemoteFile) {
        val remotePath = remoteFile.remotePath ?: return
        val parentPath = FileStorageUtils.getParentPath(remotePath) ?: return
        val parent = fileDataStorageManager.getFileByDecryptedRemotePath(parentPath) ?: return
        val existingFile = fileDataStorageManager.getFileByDecryptedRemotePath(remotePath)

        val createdFile = FileStorageUtils.fillOCFile(remoteFile).apply {
            existingFile?.let { fileId = it.fileId }
            parentId = parent.fileId
        }

        fileDataStorageManager.saveFile(createdFile)
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

    private fun getExecutionPath(operation: OfflineOperationEntity): String? {
        val path = operation.path ?: return null

        return if (operation.type is OfflineOperationType.CreateFile) {
            path.removeSuffix(OCFile.PATH_SEPARATOR)
        } else {
            path
        }
    }

    private fun adoptRemoteFolder(operation: OfflineOperationEntity, remoteFile: RemoteFile, ocFile: OCFile?) {
        ocFile?.let {
            val adoptedFile = FileStorageUtils.fillOCFile(remoteFile).apply {
                fileId = it.fileId
                parentId = it.parentId
                decryptedRemotePath = it.decryptedRemotePath
            }

            fileDataStorageManager.saveFile(adoptedFile)
        }

        repository.updateNextOperations(operation)
        deleteOperation(operation)
    }

    // region Operation Execution
    @Suppress("ComplexCondition", "LongMethod", "CyclomaticComplexMethod")
    private suspend fun executeOperation(
        operation: OfflineOperationEntity,
        client: OwnCloudClient
    ): OfflineOperationResult? = withContext(Dispatchers.IO) {
        val path = getExecutionPath(operation)
        if (path == null) {
            Log_OC.w(TAG, "⚠️ Skipped: path is null for operation id=${operation.id}")
            return@withContext null
        }

        val remoteFile = getRemoteFile(path, client)
        val ocFile = fileDataStorageManager.getFileByDecryptedRemotePath(path)

        if (operation.type is OfflineOperationType.CreateFolder && remoteFile != null) {
            Log_OC.d(TAG, "📂 Folder already exists on server, adopting it: $path")
            adoptRemoteFolder(operation, remoteFile, ocFile)
            return@withContext null
        }

        val operationType = operation.type
        if (operationType is OfflineOperationType.CreateFile && remoteFile != null) {
            handleExistingRemoteFile(operation, operationType.localPath, remoteFile, ocFile)
            return@withContext null
        }

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
            deleteOperation(operation)
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
        val localFile = File(operationType.localPath)
        val createFileOperation = UploadFileRemoteOperation(
            operationType.localPath,
            operationType.remotePath,
            operationType.mimeType,
            "",
            localFile.lastModified() / MILLIS_PER_SECOND,
            FileUtil.getCreationTimestamp(localFile),
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
    private fun getRemoteFile(remotePath: String, client: OwnCloudClient): RemoteFile? {
        val mimeType = MimeTypeUtil.getMimeTypeFromPath(remotePath)
        val isFolder = MimeTypeUtil.isFolder(mimeType)
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
