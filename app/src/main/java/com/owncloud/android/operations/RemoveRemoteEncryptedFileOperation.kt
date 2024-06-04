/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.operations

import android.content.Context
import androidx.core.util.component1
import androidx.core.util.component2
import com.nextcloud.client.account.User
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
import com.owncloud.android.utils.theme.CapabilityUtils
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.NameValuePair
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod

/**
 * Remote operation performing the removal of a remote encrypted file or folder
 *
 * Constructor
 *
 * @param remotePath   RemotePath of the remote file or folder to remove from the server
 * @param parentFolder parent folder
 */

@Suppress("LongParameterList")
class RemoveRemoteEncryptedFileOperation internal constructor(
    private val remotePath: String,
    private val user: User,
    private val context: Context,
    private val fileName: String,
    private val parentFolder: OCFile,
    private val isFolder: Boolean
) : RemoteOperation<Void>() {

    /**
     * Performs the remove operation.
     */
    @Deprecated("Deprecated in Java")
    @Suppress("TooGenericExceptionCaught")
    override fun run(client: OwnCloudClient): RemoteOperationResult<Void> {
        var result: RemoteOperationResult<Void>
        var delete: DeleteMethod? = null
        var token: String? = null
        val e2eVersion = CapabilityUtils.getCapability(context).endToEndEncryptionApiVersion
        val isE2EVersionAtLeast2 = e2eVersion >= E2EVersion.V2_0

        try {
            token = EncryptionUtils.lockFolder(parentFolder, client)

            return if (isE2EVersionAtLeast2) {
                val deleteResult = deleteForV2(client, token)
                result = deleteResult.first
                delete = deleteResult.second
                result
            } else {
                val deleteResult = deleteForV1(client, token)
                result = deleteResult.first
                delete = deleteResult.second
                result
            }
        } catch (e: Exception) {
            result = RemoteOperationResult(e)
            Log_OC.e(TAG, "Remove " + remotePath + ": " + result.logMessage, e)
        } finally {
            delete?.releaseConnection()
            token?.let { unlockFile(client, it, isE2EVersionAtLeast2) }
        }

        return result
    }

    private fun unlockFile(client: OwnCloudClient, token: String, isE2EVersionAtLeast2: Boolean) {
        val unlockFileOperationResult = if (isE2EVersionAtLeast2) {
            EncryptionUtils.unlockFolder(parentFolder, client, token)
        } else {
            EncryptionUtils.unlockFolderV1(parentFolder, client, token)
        }

        if (!unlockFileOperationResult.isSuccess) {
            Log_OC.e(TAG, "Failed to unlock " + parentFolder.localId)
        }
    }

    private fun deleteRemoteFile(
        client: OwnCloudClient,
        token: String?
    ): Pair<RemoteOperationResult<Void>, DeleteMethod> {
        val delete = DeleteMethod(client.getFilesDavUri(remotePath)).apply {
            setQueryString(arrayOf(NameValuePair(E2E_TOKEN, token)))
        }

        val status = client.executeMethod(delete, REMOVE_READ_TIMEOUT, REMOVE_CONNECTION_TIMEOUT)
        delete.getResponseBodyAsString() // exhaust the response, although not interesting

        val result = RemoteOperationResult<Void>(delete.succeeded() || status == HttpStatus.SC_NOT_FOUND, delete)
        Log_OC.i(TAG, "Remove " + remotePath + ": " + result.logMessage)

        return Pair(result, delete)
    }

    private fun deleteForV1(client: OwnCloudClient, token: String?): Pair<RemoteOperationResult<Void>, DeleteMethod> {
        @Suppress("DEPRECATION")
        val arbitraryDataProvider: ArbitraryDataProvider = ArbitraryDataProviderImpl(context)
        val privateKey = arbitraryDataProvider.getValue(user.accountName, EncryptionUtils.PRIVATE_KEY)
        val publicKey = arbitraryDataProvider.getValue(user.accountName, EncryptionUtils.PUBLIC_KEY)

        val (metadataExists, metadata) = EncryptionUtils.retrieveMetadataV1(
            parentFolder,
            client,
            privateKey,
            publicKey,
            arbitraryDataProvider,
            user
        )

        val (result, delete) = deleteRemoteFile(client, token)

        if (!isFolder) {
            EncryptionUtils.removeFileFromMetadata(fileName, metadata)
        }

        val encryptedFolderMetadata = EncryptionUtils.encryptFolderMetadata(
            metadata,
            publicKey,
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
            E2EVersion.V1_2,
            "",
            arbitraryDataProvider,
            user
        )

        return Pair(result, delete)
    }

    private fun deleteForV2(client: OwnCloudClient, token: String?): Pair<RemoteOperationResult<Void>, DeleteMethod> {
        val encryptionUtilsV2 = EncryptionUtilsV2()

        val (metadataExists, metadata) = encryptionUtilsV2.retrieveMetadata(
            parentFolder,
            client,
            user,
            context
        )

        val (result, delete) = deleteRemoteFile(client, token)

        if (isFolder) {
            encryptionUtilsV2.removeFolderFromMetadata(fileName, metadata)
        } else {
            encryptionUtilsV2.removeFileFromMetadata(fileName, metadata)
        }

        encryptionUtilsV2.serializeAndUploadMetadata(
            parentFolder,
            metadata,
            token!!,
            client,
            metadataExists,
            context,
            user,
            FileDataStorageManager(user, context.contentResolver)
        )

        return Pair(result, delete)
    }

    companion object {
        private val TAG = RemoveRemoteEncryptedFileOperation::class.java.getSimpleName()
        private const val REMOVE_READ_TIMEOUT = 30000
        private const val REMOVE_CONNECTION_TIMEOUT = 5000
    }
}
