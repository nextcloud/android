/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018-2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2012-2013 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.operations

import android.content.Context
import android.content.Intent
import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.nextcloud.client.jobs.folderDownload.FolderDownloadWorkerNotificationManager
import com.nextcloud.utils.ResultParser.data
import com.nextcloud.utils.ResultParser.list
import com.nextcloud.utils.extensions.eTagChanged
import com.nextcloud.utils.extensions.mainThread
import com.nextcloud.utils.extensions.matchesMimeType
import com.nextcloud.utils.share.UnifiedShareSharees
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFileV1
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.OperationCancelledException
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.operations.common.SyncOperation
import com.owncloud.android.operations.synchronize.TwoWaySyncOperation
import com.owncloud.android.services.OperationsService
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.MimeTypeUtil
import java.io.File
import java.util.Optional
import java.util.concurrent.atomic.AtomicBoolean

class SynchronizeFolderOperation(
    private val context: Context,
    val remotePath: String,
    private val user: User,
    storageManager: FileDataStorageManager,
    private val useWorkerWithNotification: Boolean,
    private val syncAll: Boolean
) : SyncOperation(storageManager) {

    @Volatile
    private var localFolder: OCFile? = null

    private var isPartOfInternalTwoWaySync = false
    private var remoteFolderChanged = false
    private var conflicts = 0
    private var failures = 0

    private val filesToDownload = mutableListOf<OCFile>()
    private val fileSyncs = mutableListOf<SynchronizeFileOperation>()
    private val subfolderPaths = mutableListOf<String>()

    private val cancellationRequested = AtomicBoolean(false)
    private val notificationManager = FolderDownloadWorkerNotificationManager(context, false, null)
    private val twoWaySyncOperation = TwoWaySyncOperation(context, user, storageManager, cancellationRequested)

    val accountName: String
        get() = user.accountName

    val folderId: Long?
        get() = localFolder?.fileId

    val folderNameFromPath: Optional<String>
        get() {
            val folder = localFolder ?: return Optional.empty()
            val path = folder.storagePath.takeUnless { it.isNullOrEmpty() }
                ?: FileStorageUtils.getDefaultSavePathFor(user.accountName, folder)

            return Optional.of(File(path).name)
        }

    fun cancel() {
        cancellationRequested.set(true)
    }

    override fun run(client: OwnCloudClient): RemoteOperationResult<*> {
        failures = 0
        conflicts = 0

        val folder = storageManager.getFileByPath(remotePath)
        localFolder = folder
        if (folder == null) {
            Log_OC.e(TAG, "Folder not found in database: $remotePath")
            return RemoteOperationResult<Any>(ResultCode.FILE_NOT_FOUND)
        }

        isPartOfInternalTwoWaySync = storageManager.isPartOfInternalTwoWaySync(folder)
        if (isPartOfInternalTwoWaySync) {
            twoWaySyncOperation.ensureLocalFolderExists(folder)
        }

        return try {
            val result = sync(folder, client)
            throwIfCancelled()
            result
        } catch (e: OperationCancelledException) {
            Log_OC.d(TAG, "Sync cancelled: $remotePath")
            RemoteOperationResult<Any>(e)
        }
    }

    private fun sync(folder: OCFile, client: OwnCloudClient): RemoteOperationResult<*> {
        val checkResult = checkForChanges(folder, client)
        if (!checkResult.isSuccess) return checkResult

        val result = if (remoteFolderChanged || syncAll) {
            syncFromServer(folder, client)
        } else {
            queueFromDatabase(folder)
            checkResult
        }

        if (result.isSuccess) {
            syncContents(folder, client)
        }

        return result
    }

    private fun throwIfCancelled() {
        if (cancellationRequested.get()) throw OperationCancelledException()
    }

    private fun checkForChanges(folder: OCFile, client: OwnCloudClient): RemoteOperationResult<*> {
        Log_OC.d(TAG, "Checking for changes: $remotePath (${user.accountName})")
        remoteFolderChanged = true
        throwIfCancelled()

        val result = ReadFileRemoteOperation(remotePath).execute(client)
        val remoteFolder = result.data(RemoteFile::class.java)
        if (remoteFolder == null) {
            removeIfDeletedOnServer(folder, result)
            logCheckFailure(result)
            return result
        }

        remoteFolderChanged = folder.etag.eTagChanged(FileStorageUtils.fillOCFile(remoteFolder).etag)
        val state = if (remoteFolderChanged) "changed" else "unchanged"
        Log_OC.i(TAG, "Folder $state: $remotePath")

        return RemoteOperationResult<Any>(ResultCode.OK)
    }

    private fun logCheckFailure(result: RemoteOperationResult<*>) {
        val message = "Check for changes failed: $remotePath, ${result.logMessage}"
        if (result.isException) {
            Log_OC.e(TAG, message, result.exception)
        } else {
            Log_OC.e(TAG, message)
        }
    }

    private fun syncFromServer(folder: OCFile, client: OwnCloudClient): RemoteOperationResult<*> {
        throwIfCancelled()

        Log_OC.d(TAG, "Fetching folder: $remotePath, remote id: ${folder.remoteId}")
        val result = ReadFolderRemoteOperation(remotePath).execute(client)
        if (!result.isSuccess) {
            removeIfDeletedOnServer(folder, result)
            return result
        }

        val mergeResult = mergeListing(folder, result.list<RemoteFile>())
        if (!mergeResult.isSuccess) return mergeResult

        if (conflicts > 0 || failures > 0) {
            return RemoteOperationResult<Any>(ResultCode.SYNC_CONFLICT)
        }

        return result
    }

    private fun removeIfDeletedOnServer(folder: OCFile, result: RemoteOperationResult<*>) {
        if (result.code != ResultCode.FILE_NOT_FOUND) return

        if (storageManager.isPendingOfflineCreation(folder)) {
            Log_OC.d(TAG, "Keeping folder not yet created on server: ${folder.remotePath}")
            return
        }

        if (!storageManager.fileExists(folder.fileId)) return

        Log_OC.d(TAG, "Removing folder deleted on server: ${folder.remotePath}")
        val removeLocalContent = folder.isDown &&
            folder.storagePath.startsWith(FileStorageUtils.getSavePath(user.accountName))
        storageManager.removeFolder(folder, true, removeLocalContent)
    }

    private fun mergeListing(folder: OCFile, remoteFiles: List<RemoteFile>): RemoteOperationResult<*> {
        val remoteFolder = FileStorageUtils.fillOCFile(remoteFiles.first()).apply {
            parentId = folder.parentId
            fileId = folder.fileId
        }
        Log_OC.d(TAG, "Updating database: ${folder.remotePath}")

        filesToDownload.clear()
        fileSyncs.clear()
        subfolderPaths.clear()

        throwIfCancelled()

        val encrypted = FileStorageUtils.checkEncryptionStatus(remoteFolder, storageManager)
        folder.isEncrypted = encrypted
        folder.permissions = remoteFolder.permissions
        folder.richWorkspace = remoteFolder.richWorkspace

        val metadata = RefreshFolderOperation.getDecryptedFolderMetadata(encrypted, folder, client, user, context)
        if (folder.isEncrypted && metadata == null) {
            Log_OC.e(TAG, "Metadata missing for encrypted folder: $remotePath")
            return RemoteOperationResult<Any>(ResultCode.METADATA_NOT_FOUND)
        }

        val localFilesByPath =
            RefreshFolderOperation.prefillLocalFilesMap(metadata, storageManager.getFolderContent(folder, false))
        val mergedFiles = (1..remoteFiles.lastIndex).mapNotNull { index ->
            mergeChild(folder, remoteFiles[index], localFilesByPath, metadata)
        }

        decryptFileName(metadata, folder)
        UnifiedShareSharees.fillBlocking(user, mergedFiles)

        storageManager.saveFolder(remoteFolder, mergedFiles, localFilesByPath.values)
        folder.lastSyncDateForData = System.currentTimeMillis()
        storageManager.saveFile(folder)

        syncSubfolders()

        return RemoteOperationResult<Any>(ResultCode.OK)
    }

    private fun mergeChild(
        folder: OCFile,
        remoteChild: RemoteFile,
        localFilesByPath: MutableMap<String, OCFile>,
        metadata: Any?
    ): OCFile? {
        val serverFile = FileStorageUtils.fillOCFile(remoteChild)
        val mergedFile = FileStorageUtils.fillOCFile(remoteChild).apply { parentId = folder.fileId }
        val localFile = localFilesByPath.remove(serverFile.remotePath)
            ?: storageManager.getFileByPath(mergedFile.remotePath)

        copyLocalState(localFile, mergedFile, serverFile)
        FileStorageUtils.searchForLocalFileInDefaultPath(mergedFile, user.accountName)
        decryptFileName(metadata, mergedFile)
        mergedFile.isEncrypted = mergedFile.isEncrypted || folder.isEncrypted

        if (localFile != null && isDeletedLocally(serverFile, localFile)) {
            val deletedOnServer = twoWaySyncOperation.deleteRemoteFile(localFile, client)
            return if (deletedOnServer) null else mergedFile
        }

        queueSync(serverFile, localFile)

        return mergedFile
    }

    private fun isDeletedLocally(serverFile: OCFile, localFile: OCFile): Boolean =
        isPartOfInternalTwoWaySync && twoWaySyncOperation.isDeletedLocally(serverFile, localFile)

    private fun decryptFileName(metadata: Any?, file: OCFile) {
        when (metadata) {
            is DecryptedFolderMetadataFileV1 ->
                RefreshFolderOperation.updateFileNameForEncryptedFileV1(storageManager, metadata, file)

            is DecryptedFolderMetadataFile ->
                RefreshFolderOperation.updateFileNameForEncryptedFile(storageManager, metadata, file)
        }
    }

    private fun copyLocalState(localFile: OCFile?, mergedFile: OCFile, serverFile: OCFile) {
        mergedFile.lastSyncDateForProperties = System.currentTimeMillis()
        if (localFile == null) {
            mergedFile.etag = ""
            return
        }

        mergedFile.apply {
            fileId = localFile.fileId
            lastSyncDateForData = localFile.lastSyncDateForData
            modificationTimestampAtLastSyncForData = localFile.modificationTimestampAtLastSyncForData
            storagePath = localFile.storagePath
            etag = localFile.etag
        }

        if (mergedFile.isFolder) {
            mergedFile.fileLength = localFile.fileLength
        } else if (isImageChanged(serverFile, localFile)) {
            mergedFile.isUpdateThumbnailNeeded = true
            Log_OC.d(TAG, "Image changed on server: ${serverFile.remotePath}")
        }

        mergedFile.apply {
            isSharedViaLink = localFile.isSharedViaLink
            isSharedWithSharee = localFile.isSharedWithSharee
            etagInConflict = localFile.etagInConflict
        }
    }

    private fun isImageChanged(serverFile: OCFile, localFile: OCFile): Boolean = remoteFolderChanged &&
        serverFile.matchesMimeType(MimeTypeUtil::isImage) &&
        serverFile.modificationTimestamp != localFile.modificationTimestamp

    private fun queueSync(serverFile: OCFile, localFile: OCFile?) {
        if (serverFile.isFolder) {
            subfolderPaths.add(serverFile.remotePath)
        } else {
            fileSyncs.add(createFileSync(localFile, serverFile))
        }
    }

    private fun queueFromDatabase(folder: OCFile) {
        for (child in storageManager.getFolderContent(folder, false)) {
            if (child.isFolder) continue

            if (child.isDown) {
                val conflictedFile = child.takeIf { it.etagInConflict != null }
                fileSyncs.add(createFileSync(child, conflictedFile))
            } else {
                filesToDownload.add(child)
            }
        }
    }

    private fun createFileSync(localFile: OCFile?, serverFile: OCFile?) = SynchronizeFileOperation(
        localFile,
        serverFile,
        user,
        true,
        context,
        storageManager,
        useWorkerWithNotification
    )

    private fun syncSubfolders() {
        for (path in subfolderPaths) {
            throwIfCancelled()
            requestFolderSync(path)
        }
    }

    private fun requestFolderSync(path: String) {
        val intent = Intent(context, OperationsService::class.java).apply {
            action = OperationsService.ACTION_SYNC_FOLDER
            putExtra(OperationsService.EXTRA_ACCOUNT, user.toPlatformAccount())
            putExtra(OperationsService.EXTRA_REMOTE_PATH, path)
            putExtra(OperationsService.EXTRA_SYNC_ALL, syncAll)
        }
        context.startService(intent)
    }

    private fun syncContents(folder: OCFile, client: OwnCloudClient) {
        downloadFiles(folder)
        runFileSyncs(folder)
        if (isPartOfInternalTwoWaySync) {
            twoWaySyncOperation.uploadNewLocalEntries(folder, client)
        }

        refreshETag(folder, client)
    }

    private fun refreshETag(folder: OCFile, client: OwnCloudClient) {
        val result = ReadFileRemoteOperation(remotePath).execute(client)
        if (!result.isSuccess) {
            Log_OC.w(TAG, "Cannot refresh eTag: $remotePath")
            return
        }

        val remoteFolder = result.data(RemoteFile::class.java) ?: return
        folder.etag = remoteFolder.etag
        storageManager.saveFile(folder)
    }

    private fun downloadFiles(folder: OCFile) {
        val fileDownloadHelper = FileDownloadHelper.instance()
        if (useWorkerWithNotification) {
            fileDownloadHelper.downloadFolder(folder, user.accountName)
            return
        }

        try {
            for (file in filesToDownload) {
                if (cancellationRequested.get()) break

                downloadFile(file, fileDownloadHelper)
            }
        } catch (e: Exception) {
            Log_OC.d(TAG, "Downloads stopped: $remotePath, $e")
        }
    }

    private fun downloadFile(file: OCFile, fileDownloadHelper: FileDownloadHelper) {
        val operation = DownloadFileOperation(user, file, context)
        val result = operation.execute(client)
        if (result.isSuccess) {
            fileDownloadHelper.saveFile(file, operation, storageManager)
            Log_OC.d(TAG, "Downloaded: ${file.remotePath}")
        } else {
            Log_OC.d(TAG, "Download failed: ${file.remotePath}")
        }
    }

    private fun runFileSyncs(folder: OCFile) {
        val total = fileSyncs.size
        val folderName = folder.fileName
        Log_OC.v(TAG, "Syncing $total files: $remotePath")

        try {
            fileSyncs.forEachIndexed { index, operation ->
                throwIfCancelled()

                val result = operation.execute(context)
                val file = operation.localFile
                when {
                    result.isSuccess && file != null ->
                        notificationManager.showProgressNotification(folderName, file.fileName, index, total)

                    result.code == ResultCode.SYNC_CONFLICT -> conflicts++

                    else -> {
                        failures++
                        logFileSyncFailure(result)
                    }
                }
            }
        } finally {
            mainThread(0L) { notificationManager.dismiss() }
        }
    }

    private fun logFileSyncFailure(result: RemoteOperationResult<*>) {
        val exceptionMessage = result.exception?.let { ", ${it.message}" }.orEmpty()
        Log_OC.e(TAG, "File sync failed: ${result.logMessage}$exceptionMessage")
    }

    companion object {
        private val TAG = SynchronizeFolderOperation::class.java.simpleName
    }
}
