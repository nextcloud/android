/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2017 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.operations

import android.content.Context
import androidx.core.util.component1
import androidx.core.util.component2
import com.nextcloud.client.account.User
import com.nextcloud.utils.e2ee.E2EVersionHelper
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.status.E2EVersion
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.EncryptionUtilsV2
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.NameValuePair
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod
import com.nextcloud.utils.AppConstants.DEFAULT_CONNECTION_TIMEOUT_MS

class RemoveRemoteEncryptedFileOperation @JvmOverloads internal constructor(
    private val remotePath: String,
    private val user: User,
    private val context: Context,
    private val fileName: String,
    private val parentFolder: OCFile,
    private val isFolder: Boolean,
    private val expectedEtag: String? = null
) : RemoteOperation<Void>() {
    @Volatile
    private var canCommit = true

    @Deprecated("Deprecated in Java")
    override fun run(client: OwnCloudClient): RemoteOperationResult<Void> {
        var result = RemoteOperationResult<Void>(RemoteOperationResult.ResultCode.UNKNOWN_ERROR)
        var token: String? = null
        val storageManager = FileDataStorageManager(user, context.contentResolver)
        val capability = storageManager.getCapability(user)
        val isE2EVersionAtLeast2 = (E2EVersionHelper.isV2Plus(capability))
        val e2eeVersion = capability.endToEndEncryptionApiVersion
        var counter = parentFolder.e2eCounter + 1
        canCommit = true

        try {
            if (isE2EVersionAtLeast2) {
                val (_, metadata) = EncryptionUtilsV2().retrieveMetadata(parentFolder, client, user, context)
                counter = metadata.metadata.counter + 1
            }
            token = EncryptionUtils.lockFolder(parentFolder, client, counter)

            result = if (isE2EVersionAtLeast2) {
                deleteForV2(counter, storageManager, client, token)
            } else {
                deleteForV1(client, token, e2eeVersion)
            }
        } catch (e: Exception) {
            result = RemoteOperationResult(e)
            Log_OC.e(TAG, "Remove " + remotePath + ": " + result.logMessage, e)
        } finally {
            if (canCommit && token != null) {
                val unlocked = unlockFile(
                    counter.takeIf { result.isSuccess },
                    storageManager,
                    client,
                    token,
                    isE2EVersionAtLeast2
                )
                if (result.isSuccess && !unlocked.isSuccess) result = unlocked
            }
        }

        return result
    }

    private fun unlockFile(
        counter: Long?,
        storageManager: FileDataStorageManager,
        client: OwnCloudClient,
        token: String,
        isE2EVersionAtLeast2: Boolean
    ): RemoteOperationResult<Void> {
        val unlockFileOperationResult = if (isE2EVersionAtLeast2) {
            EncryptionUtils.unlockFolder(parentFolder, client, token)
        } else {
            EncryptionUtils.unlockFolderV1(parentFolder, client, token)
        }

        if (unlockFileOperationResult.isSuccess) {
            if (isE2EVersionAtLeast2 && counter != null) storageManager.updateE2EECounter(parentFolder, counter)
        } else {
            Log_OC.e(TAG, "Failed to unlock " + parentFolder.localId)
        }
        return unlockFileOperationResult
    }

    private fun deleteRemoteFile(client: OwnCloudClient, token: String?): RemoteOperationResult<Void> {
        val delete = DeleteMethod(client.getFilesDavUri(remotePath)).apply {
            setQueryString(arrayOf(NameValuePair(E2E_TOKEN, token)))
            expectedEtag?.let { setRequestHeader("If-Match", FileTransferSource.quotedEtag(it)) }
        }

        try {
            canCommit = false
            val status = client.executeMethod(delete, REMOVE_READ_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT_MS)
            delete.getResponseBodyAsString() // exhaust the response, although not interesting
            val result = RemoteOperationResult<Void>(delete.succeeded() || status == HttpStatus.SC_NOT_FOUND, delete)
            canCommit = !result.isSuccess
            Log_OC.i(TAG, "Remove " + remotePath + ": " + result.logMessage)
            return result
        } finally {
            delete.releaseConnection()
        }
    }

    private fun deleteForV1(
        client: OwnCloudClient,
        token: String?,
        e2eeVersion: E2EVersion
    ): RemoteOperationResult<Void> {
        @Suppress("DEPRECATION")
        val arbitraryDataProvider: ArbitraryDataProvider = ArbitraryDataProviderImpl(context)
        val privateKey = arbitraryDataProvider.getValue(user.accountName, EncryptionUtils.PRIVATE_KEY)
        val certificate = arbitraryDataProvider.getValue(user.accountName, EncryptionUtils.CERTIFICATE)

        val (metadataExists, metadata) = EncryptionUtils.retrieveMetadataV1(
            parentFolder,
            client,
            privateKey,
            certificate,
            arbitraryDataProvider,
            user,
            e2eeVersion.value
        )

        val result = deleteRemoteFile(client, token)
        if (!result.isSuccess) return result
        canCommit = false

        if (remotePath != parentFolder.remotePath) {
            EncryptionUtils.removeFileFromMetadata(fileName, metadata)
        }

        val encryptedFolderMetadata = EncryptionUtils.encryptFolderMetadata(
            metadata,
            certificate,
            parentFolder.localId,
            user,
            arbitraryDataProvider
        )

        val serializedFolderMetadata = EncryptionUtils.serializeJSON(encryptedFolderMetadata)

        EncryptionUtils.uploadMetadata(
            parentFolder,
            serializedFolderMetadata,
            token,
            client,
            metadataExists,
            e2eeVersion,
            "",
            arbitraryDataProvider,
            user
        )
        canCommit = true

        return result
    }

    private fun deleteForV2(
        counter: Long,
        storageManager: FileDataStorageManager,
        client: OwnCloudClient,
        token: String?
    ): RemoteOperationResult<Void> {
        val encryptionUtilsV2 = EncryptionUtilsV2()

        val (metadataExists, metadata) = encryptionUtilsV2.retrieveMetadata(
            parentFolder,
            client,
            user,
            context
        )

        val result = deleteRemoteFile(client, token)
        if (!result.isSuccess) return result
        canCommit = false

        if (isFolder && remotePath != parentFolder.remotePath) {
            encryptionUtilsV2.removeFolderFromMetadata(fileName, metadata)
        } else if (!isFolder) {
            encryptionUtilsV2.removeFileFromMetadata(fileName, metadata)
        }
        metadata.metadata.counter = counter

        encryptionUtilsV2.serializeAndUploadMetadata(
            parentFolder,
            metadata,
            token!!,
            client,
            metadataExists,
            context,
            user,
            storageManager
        )
        canCommit = true

        return result
    }

    companion object {
        private val TAG = RemoveRemoteEncryptedFileOperation::class.java.getSimpleName()
        private const val REMOVE_READ_TIMEOUT = 30000
    }
}
