/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations

import android.content.Context
import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.utils.extensions.toNextcloudClient
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFileV1
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.DownloadFileRemoteOperation
import com.owncloud.android.operations.factory.UploadFileOperationFactory
import com.owncloud.android.utils.EncryptionUtils
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import javax.crypto.Cipher

@Suppress("DEPRECATION")
class FileTransferContent(
    private val context: Context,
    private val user: User,
    private val storageManager: FileDataStorageManager,
    private val uploadFactory: UploadFileOperationFactory
) {
    fun copy(source: OCFile, parent: OCFile, filename: String, client: OwnCloudClient): RemoteOperationResult<*> {
        val directoryPath = Files.createTempDirectory(context.cacheDir.toPath(), "file-transfer-")
        val directory = directoryPath.toFile()
        try {
            val raw = directoryPath.resolve(source.remotePath.trimStart('/')).normalize()
            require(raw != directoryPath && raw.startsWith(directoryPath))
            val download = DownloadFileRemoteOperation(source.remotePath, directory.absolutePath, source.fileLength)
            val downloaded = download.execute(client.toNextcloudClient(context))
            if (!downloaded.isSuccess) return downloaded

            val plaintext = directoryPath.resolve("content")
            if (source.isEncrypted) {
                decrypt(source, raw, plaintext, client)
            } else {
                Files.move(raw, plaintext)
            }
            if (!plaintext.toFile().setLastModified(download.modificationTimestamp)) {
                Log_OC.w(TAG, "Unable to preserve transfer modification time")
            }
            val upload = OCUpload(plaintext.toString(), parent.remotePath + filename, user.accountName).apply {
                localAction = FileUploadWorker.LOCAL_BEHAVIOUR_FORGET
                nameCollisionPolicy = NameCollisionPolicy.ASK_USER
                setUseWifiOnly(false)
            }
            val operation = uploadFactory.create(context, upload, user = user, storageManager = storageManager)
            operation.setKeepE2EELockOnFailure(true)
            operation.file.mimeType = source.mimeType
            return operation.execute(client)
        } finally {
            if (!directory.deleteRecursively()) Log_OC.w(TAG, "Unable to remove temporary transfer content")
        }
    }

    private fun decrypt(source: OCFile, raw: Path, plaintext: Path, client: OwnCloudClient) {
        val parent = storageManager.getFileById(source.parentId)
            ?: throw IOException("Encrypted transfer source has no parent")
        val metadata = EncryptionUtils.downloadFolderMetadata(parent, client, context, user)
        val (key, nonce) = when (metadata) {
            is DecryptedFolderMetadataFile -> metadata.metadata.files[source.encryptedFileName]?.let {
                it.key to it.nonce
            }

            is DecryptedFolderMetadataFileV1 -> metadata.files[source.encryptedFileName]?.let {
                it.encrypted.key to it.initializationVector
            }

            else -> null
        } ?: throw IOException("Encrypted transfer source is missing from metadata")
        val cipher = EncryptionUtils.getCipher(
            Cipher.DECRYPT_MODE,
            EncryptionUtils.decodeStringToBase64Bytes(key),
            EncryptionUtils.decodeStringToBase64Bytes(nonce)
        )
        Files.newInputStream(raw).use { input ->
            Files.newOutputStream(plaintext).use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var count = input.read(buffer)
                while (count != -1) {
                    output.write(cipher.update(buffer, 0, count) ?: byteArrayOf())
                    count = input.read(buffer)
                }
                output.write(cipher.doFinal())
            }
        }
    }

    companion object {
        private val TAG = FileTransferContent::class.java.simpleName
    }
}
