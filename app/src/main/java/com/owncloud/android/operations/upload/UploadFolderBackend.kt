/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.upload

import com.nextcloud.client.database.NextcloudDatabase
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.operations.CreateFolderOperation
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.FileStorageUtils

class UploadFolderBackend(
    private val operation: UploadFileOperation,
    private val client: OwnCloudClient,
    private val keys: ArbitraryDataProvider = ArbitraryDataProviderImpl(NextcloudDatabase.instance().arbitraryDataDao())
) {
    private val storage = operation.storageManager
    private val user = operation.user
    private val context = operation.context

    fun resolve(path: String): OCFile = UploadFolderResolver(::root, ::refresh, ::child, ::createChild).resolve(path)

    private fun root(): OCFile = storage.getFileByEncryptedRemotePath(OCFile.ROOT_PATH) ?: run {
        val result = ReadFileRemoteOperation(OCFile.ROOT_PATH).execute(client)
        requireSuccess(result)
        FileStorageUtils.fillOCFile(result.data.first() as RemoteFile).also { storage.saveFile(it) }
    }

    private fun checkKeys() {
        if (keys.getValue(user.accountName, EncryptionUtils.PRIVATE_KEY).isEmpty() ||
            keys.getValue(user.accountName, EncryptionUtils.PUBLIC_KEY).isEmpty()
        ) {
            throw UploadFolderException(RemoteOperationResult<Any>(UploadFileException.EncryptionKeysUnavailable()))
        }
    }

    private fun refresh(folder: OCFile) {
        checkCancellation()
        val properties = ReadFileRemoteOperation(folder.remotePath).execute(client)
        requireSuccess(properties)
        val remote = properties.data.first() as RemoteFile
        folder.remoteId = remote.remoteId
        val parent = storage.getFileById(folder.parentId)
        folder.isEncrypted = remote.isEncrypted ||
            (parent != null && FileStorageUtils.checkEncryptionStatus(parent, storage))
        if (folder.isEncrypted) {
            checkKeys()
        }
        storage.saveFile(folder)
        requireSuccess(
            RefreshFolderOperation(
                folder,
                System.currentTimeMillis(),
                false,
                true,
                true,
                storage,
                user,
                context
            ).execute(client)
        )
    }

    private fun child(parent: OCFile, name: String): OCFile? =
        storage.getFolderContent(parent, false).orEmpty().firstOrNull {
            it.isFolder && it.decryptedFileName.equals(name, ignoreCase = parent.isEncrypted)
        }

    private fun createChild(parent: OCFile, name: String): OCFile {
        checkCancellation()
        val target = parent.decryptedRemotePath + name + "/"
        if (parent.isEncrypted) checkKeys()
        val result = CreateFolderOperation(target, user, context, storage).apply {
            setDeferFailureToServerRollback(true)
        }.execute(client)
        if (!result.isSuccess && result.code != ResultCode.FOLDER_ALREADY_EXISTS) throw UploadFolderException(result)
        // A concurrent creator may have chosen a new ciphertext name; adopt it from fresh metadata.
        refresh(parent)
        return child(parent, name)
            ?: throw UploadFolderException(RemoteOperationResult<Any>(ResultCode.CANNOT_CREATE_FILE))
    }

    private fun requireSuccess(result: RemoteOperationResult<*>) {
        if (!result.isSuccess) throw UploadFolderException(result)
    }

    private fun checkCancellation() {
        if (operation.isCancellationRequested) {
            throw UploadFolderException(RemoteOperationResult<Any>(ResultCode.USER_CANCELLED))
        }
    }
}
