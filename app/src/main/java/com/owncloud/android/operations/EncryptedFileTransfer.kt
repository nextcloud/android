/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations

import android.content.Context
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.operations.factory.UploadFileOperationFactory

@Suppress("DEPRECATION")
class EncryptedFileTransfer(
    private val storageManager: FileDataStorageManager,
    private val user: User,
    private val context: Context,
    uploadFactory: UploadFileOperationFactory,
    private val keepEncryption: Boolean
) {
    private val content = FileTransferContent(context, user, storageManager, uploadFactory)

    fun copy(file: OCFile, parent: OCFile, client: OwnCloudClient): RemoteOperationResult<*> =
        transfer(file, parent, client, false)

    fun move(file: OCFile, parent: OCFile, client: OwnCloudClient): RemoteOperationResult<*> =
        transfer(file, parent, client, true)

    private fun transfer(
        file: OCFile,
        parent: OCFile,
        client: OwnCloudClient,
        removeSource: Boolean
    ): RemoteOperationResult<*> {
        invalidTarget(file, parent, removeSource)?.let { return RemoteOperationResult<Any>(it) }

        return try {
            val source = FileTransferSource(context, user, storageManager)
            if (source.hasLocalChanges(file)) return RemoteOperationResult<Any>(ResultCode.SYNC_CONFLICT)
            val stat = ReadFileRemoteOperation(file.remotePath).execute(client)
            if (!stat.isSuccess) return stat
            val original = stat.data.firstOrNull() as? RemoteFile
                ?: return RemoteOperationResult<Any>(ResultCode.FILE_NOT_FOUND)
            val sourceEtag = original.etag.orEmpty()
            if (removeSource && sourceEtag.isEmpty()) {
                return RemoteOperationResult<Any>(ResultCode.UNKNOWN_ERROR)
            }

            val refreshed = refresh(parent, client)
            if (!refreshed.isSuccess) return refreshed
            val filename = targetName(file, parent, client, !removeSource)
                ?: return RemoteOperationResult<Any>(ResultCode.INVALID_OVERWRITE)
            val copied = copyTree(file, parent, filename, client)
            if (!copied.isSuccess || !removeSource) return copied

            source.remove(file, sourceEtag, client)
        } catch (e: Exception) {
            RemoteOperationResult<Any>(e)
        }
    }

    private fun invalidTarget(file: OCFile, parent: OCFile, removeSource: Boolean): ResultCode? = when {
        file.remotePath == OCFile.ROOT_PATH || (file.isFolder && parent.remotePath.startsWith(file.remotePath)) ->
            if (removeSource) ResultCode.INVALID_MOVE_INTO_DESCENDANT else ResultCode.INVALID_COPY_INTO_DESCENDANT

        removeSource && file.parentId == parent.fileId -> ResultCode.INVALID_OVERWRITE

        else -> null
    }

    private fun targetName(file: OCFile, parent: OCFile, client: OwnCloudClient, allowRename: Boolean): String? {
        val names = storageManager.getFolderContent(parent, false).asSequence().map { it.decryptedFileName }.toList()
        if (names.none { it.equals(file.decryptedFileName, ignoreCase = true) }) return file.decryptedFileName
        if (!allowRename || file.parentId != parent.fileId) return null
        return UploadFileOperation.getNewAvailableRemotePath(
            client,
            file.decryptedFileName,
            names,
            true
        )
    }

    private fun copyTree(
        source: OCFile,
        parent: OCFile,
        filename: String,
        client: OwnCloudClient
    ): RemoteOperationResult<*> {
        if (source.isLocked) return RemoteOperationResult<Any>(ResultCode.LOCK_FAILED)
        if (!source.isFolder) return content.copy(source, parent, filename, client)

        val refreshed = refresh(source, client)
        if (!refreshed.isSuccess) return refreshed
        // Capture the complete server listing before creating anything in the destination.
        val children = storageManager.getFolderContent(source, false).toList()
        val path = parent.decryptedRemotePath + filename + OCFile.PATH_SEPARATOR
        val created = CreateFolderOperation(path, user, context, storageManager).execute(client)
        if (!created.isSuccess) return created
        val destination = storageManager.getFileByDecryptedRemotePath(path)
            ?: return RemoteOperationResult<Any>(ResultCode.FILE_NOT_FOUND)
        if (keepEncryption && source.isEncrypted && !parent.isEncrypted) {
            val encrypted = FileTransferSource(context, user, storageManager).encrypt(destination, client)
            if (!encrypted.isSuccess) return encrypted
        }
        for (child in children) {
            val copied = copyTree(child, destination, child.decryptedFileName, client)
            if (!copied.isSuccess) return copied
        }
        return RemoteOperationResult<Any>(ResultCode.OK)
    }

    private fun refresh(folder: OCFile, client: OwnCloudClient): RemoteOperationResult<*> = RefreshFolderOperation(
        folder,
        System.currentTimeMillis(),
        false,
        true,
        true,
        storageManager,
        user,
        context
    ).execute(client)
}
