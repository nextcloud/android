/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations

import android.content.Context
import androidx.core.util.component1
import androidx.core.util.component2
import com.nextcloud.client.account.User
import com.nextcloud.utils.AppConstants.DEFAULT_CONNECTION_TIMEOUT_MS
import com.nextcloud.utils.e2ee.E2EVersionHelper
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.resources.e2ee.ToggleEncryptionRemoteOperation
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.EncryptionUtilsV2
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod

@Suppress("DEPRECATION")
class FileTransferSource(
    private val context: Context,
    private val user: User,
    private val storageManager: FileDataStorageManager
) {
    fun hasLocalChanges(source: OCFile): Boolean {
        if (source.isInConflict) return true
        return if (source.isFolder) {
            storageManager.getFolderContent(source, false).any(::hasLocalChanges)
        } else {
            source.localModificationTimestamp > source.lastSyncDateForData
        }
    }

    fun remove(source: OCFile, etag: String, client: OwnCloudClient): RemoteOperationResult<*> {
        if (hasLocalChanges(source)) return RemoteOperationResult<Any>(ResultCode.SYNC_CONFLICT)
        val result = if (source.isEncrypted) {
            val parent = storageManager.getFileById(source.parentId)
                ?: return RemoteOperationResult<Any>(ResultCode.FILE_NOT_FOUND)
            RemoveRemoteEncryptedFileOperation(
                source.remotePath,
                user,
                context,
                source.encryptedFileName,
                if (parent.isEncrypted) parent else source,
                source.isFolder,
                etag
            ).execute(client)
        } else {
            delete(source.remotePath, etag, client)
        }
        if (!result.isSuccess) return result
        return if (storageManager.removeFile(source, true, true)) {
            result
        } else {
            RemoteOperationResult<Any>(ResultCode.LOCAL_STORAGE_NOT_REMOVED)
        }
    }

    private fun delete(remotePath: String, etag: String, client: OwnCloudClient): RemoteOperationResult<*> {
        val method = DeleteMethod(client.getFilesDavUri(remotePath))
        try {
            method.setRequestHeader("If-Match", quotedEtag(etag))
            client.executeMethod(method, DEFAULT_CONNECTION_TIMEOUT_MS, DEFAULT_CONNECTION_TIMEOUT_MS)
            method.responseBodyAsString
            return RemoteOperationResult<Any>(method.succeeded(), method)
        } finally {
            method.releaseConnection()
        }
    }

    fun encrypt(folder: OCFile, client: OwnCloudClient): RemoteOperationResult<*> {
        val toggled = ToggleEncryptionRemoteOperation(folder.localId, folder.remotePath, true).execute(client)
        if (!toggled.isSuccess) return toggled
        folder.isEncrypted = true
        storageManager.saveFile(folder)
        val provider = ArbitraryDataProviderImpl(context)
        val publicKey = provider.getValue(user.accountName, EncryptionUtils.PUBLIC_KEY)
        val privateKey = provider.getValue(user.accountName, EncryptionUtils.PRIVATE_KEY)
        val isV2 = E2EVersionHelper.isV2Plus(storageManager.getCapability(user))
        val counter = if (isV2) EncryptionUtils.E2E_V2_INITIAL_COUNTER else EncryptionUtils.E2E_V1_INITIAL_COUNTER
        val token = EncryptionUtils.lockFolder(folder, client, counter)
        // Failed metadata writes must stay staged until the server rolls them back.
        if (isV2) {
            val utils = EncryptionUtilsV2()
            val (exists, metadata) = utils.retrieveMetadata(folder, client, user, context)
            utils.serializeAndUploadMetadata(folder, metadata, token, client, exists, context, user, storageManager)
        } else {
            val (exists, metadata) = EncryptionUtils.retrieveMetadataV1(
                folder,
                client,
                privateKey,
                publicKey,
                provider,
                user,
                storageManager.getE2EEVersion(user)
            )
            val encrypted = EncryptionUtils.encryptFolderMetadata(metadata, publicKey, folder.localId, user, provider)
            EncryptionUtils.uploadMetadata(
                folder, EncryptionUtils.serializeJSON(encrypted), token, client, exists,
                storageManager.getE2EEVersionObject(user), "", provider, user
            )
        }
        val unlocked = if (isV2) {
            EncryptionUtils.unlockFolder(folder, client, token)
        } else {
            EncryptionUtils.unlockFolderV1(folder, client, token)
        }
        if (unlocked.isSuccess && isV2) storageManager.updateE2EECounter(folder, counter)
        return unlocked
    }

    companion object {
        internal fun quotedEtag(etag: String): String = "\"${etag.trim('"')}\""
    }
}
