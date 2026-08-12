/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

import android.content.Context
import com.nextcloud.client.account.User
import com.nextcloud.utils.extensions.toNextcloudClient
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFileV1
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.DownloadFileRemoteOperation
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.FileStorageUtils
import java.io.File
import javax.crypto.Cipher

class E2eeMediaFileLoader(
    private val context: Context,
    private val user: User,
    private val arbitraryDataProvider: ArbitraryDataProvider = ArbitraryDataProviderImpl(context)
) : E2eePlaintextMediaLoader {
    override fun <T> withPlaintext(file: OCFile, parent: OCFile, client: OwnCloudClient, block: (ByteArray) -> T?): T? {
        val encryptedFile = downloadEncryptedFile(file, client) ?: return null
        val plaintext = runCatching { decryptToBytes(file, parent, client, encryptedFile) }
            .onFailure { Log_OC.w(TAG, "Could not decrypt encrypted media source: ${it.javaClass.simpleName}") }
            .getOrNull()

        return plaintext?.let {
            try {
                block(it)
            } finally {
                it.fill(0)
                deleteEncryptedFile(encryptedFile)
            }
        } ?: run {
            deleteEncryptedFile(encryptedFile)
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun downloadEncryptedFile(file: OCFile, client: OwnCloudClient): File? {
        val temporaryFolder = FileStorageUtils.getTemporalPath(user.accountName)
        val operation = DownloadFileRemoteOperation(file.remotePath, temporaryFolder, file.fileLength)
        val result = operation.execute(client.toNextcloudClient(context)) as RemoteOperationResult<Unit>
        if (!result.isSuccess) {
            return null
        }

        return File(temporaryFolder + file.remotePath).takeIf { it.exists() }
    }

    private fun decryptToBytes(file: OCFile, parent: OCFile, client: OwnCloudClient, encryptedFile: File): ByteArray {
        val metadata = requireMetadata(parent, client)
        val keys = requireEncryptionKeys(file, metadata)
        val key = EncryptionUtils.decodeStringToBase64Bytes(keys.key)
        val iv = EncryptionUtils.decodeStringToBase64Bytes(keys.nonce)
        val cipher = EncryptionUtils.getCipher(Cipher.DECRYPT_MODE, key, iv)

        return EncryptionUtils.decryptFileToBytes(
            cipher,
            encryptedFile,
            keys.authenticationTag,
            arbitraryDataProvider,
            user
        )
    }

    private fun requireMetadata(parent: OCFile, client: OwnCloudClient): Any =
        EncryptionUtils.downloadFolderMetadata(parent, client, context, user)
            ?: throw IllegalStateException("E2EE metadata is unavailable")

    private fun requireEncryptionKeys(file: OCFile, metadata: Any): EncryptionKeys =
        extractEncryptionKeys(file, metadata)
            ?: throw IllegalStateException("E2EE file keys are unavailable")

    private fun extractEncryptionKeys(file: OCFile, metadata: Any): EncryptionKeys? = when (metadata) {
        is DecryptedFolderMetadataFile -> metadata.metadata.files[file.encryptedFileName]?.let {
            EncryptionKeys(it.key, it.nonce, it.authenticationTag)
        }

        is DecryptedFolderMetadataFileV1 -> metadata.files[file.encryptedFileName]?.let {
            EncryptionKeys(it.encrypted.key, it.initializationVector, it.authenticationTag)
        }

        else -> null
    }

    private fun deleteEncryptedFile(file: File) {
        if (!file.delete()) {
            Log_OC.w(TAG, "Could not delete encrypted media source")
        }
    }

    private data class EncryptionKeys(val key: String, val nonce: String, val authenticationTag: String)

    companion object {
        private val TAG = E2eeMediaFileLoader::class.java.simpleName
    }
}
