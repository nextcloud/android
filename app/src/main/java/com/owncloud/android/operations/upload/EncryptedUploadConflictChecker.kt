/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.upload

import android.content.Context
import com.nextcloud.client.account.User
import com.nextcloud.utils.extensions.toNextcloudClient
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.OperationCancelledException
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.DownloadFileRemoteOperation
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.FileStorageUtils
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.util.function.BooleanSupplier
import javax.crypto.Cipher

class EncryptedUploadConflictChecker(private val context: Context, private val user: User) {
    @JvmOverloads
    @Throws(Exception::class)
    fun compare(
        client: OwnCloudClient,
        parent: OCFile,
        metadata: Any?,
        local: File,
        filename: String,
        cancelled: BooleanSupplier = BooleanSupplier { false },
        requirePreview: Boolean = false
    ): EncryptedFileComparison? {
        val entry = EncryptedFileMetadata.find(metadata, filename) ?: return null
        checkCancelled(cancelled)
        require(local.isFile) { "Local file is unavailable for encrypted comparison" }
        val remote = readRemoteFile(client, parent, entry)
        val length = local.length()
        val modified = local.lastModified()
        val identity = EncryptedFileComparisonCache.fingerprint(user.accountName, remote.remotePath, local.absolutePath)
        val remoteVersion = EncryptedFileComparisonCache.fingerprint(
            remote.etag.orEmpty(),
            remote.remoteId.orEmpty(),
            remote.fileLength.toString(),
            remote.modificationTimestamp.toString(),
            entry.key,
            entry.nonce
        )
        val version = EncryptedFileComparisonCache.version(length, modified, remoteVersion)
        val cache = EncryptedFileComparisonCache(ArbitraryDataProviderImpl(context), user.accountName)
        if (!remote.etag.isNullOrEmpty()) {
            cache.read(identity, version)?.let { (same, size) ->
                if (same || !requirePreview) {
                    remote.fileLength = size
                    Log_OC.d(TAG, "Reusing encrypted comparison for unchanged local file and remote version")
                    return EncryptedFileComparison(same, remote, remoteVersion)
                }
            }
        }

        val same = downloadComparison(client, remote, entry, local, cancelled, requirePreview)
        try {
            if (length != local.length() || modified != local.lastModified()) {
                throw IOException("Local file changed during encrypted comparison")
            }
            if (!remote.etag.isNullOrEmpty()) {
                cache.write(identity, version, same, remote.fileLength)
            }
            return EncryptedFileComparison(same, remote, remoteVersion)
        } catch (e: Exception) {
            releasePreview(context, remote)
            throw e
        }
    }

    private fun readRemoteFile(client: OwnCloudClient, parent: OCFile, entry: EncryptedFileMetadata): OCFile {
        require(entry.remoteName.isNotBlank() && entry.remoteName != "." && entry.remoteName != "..")
        require(!entry.remoteName.contains('/') && !entry.remoteName.contains('\\'))
        val result = ReadFileRemoteOperation(parent.remotePath + entry.remoteName).execute(client)
        if (!result.isSuccess) throw IOException("Could not read encrypted conflict target: ${result.code}")
        return FileStorageUtils.fillOCFile(result.data[0] as RemoteFile).apply {
            isEncrypted = true
            decryptedRemotePath = parent.decryptedRemotePath + entry.filename
            mimeType = entry.mimeType
        }
    }

    private fun checkCancelled(cancelled: BooleanSupplier) {
        if (cancelled.asBoolean) throw OperationCancelledException()
    }

    private fun downloadComparison(
        client: OwnCloudClient,
        remote: OCFile,
        entry: EncryptedFileMetadata,
        local: File,
        cancelled: BooleanSupplier,
        requirePreview: Boolean
    ): Boolean {
        val temporaryDirectory = Files.createTempDirectory(context.cacheDir.toPath(), "encrypted-comparison-").toFile()
        var retainedPreview: File? = null
        var completed = false
        try {
            val encrypted = File(temporaryDirectory.absolutePath + remote.remotePath)
            require(encrypted.canonicalPath.startsWith(temporaryDirectory.canonicalPath + File.separator))
            val download =
                DownloadFileRemoteOperation(remote.remotePath, temporaryDirectory.absolutePath, remote.fileLength)
            download.addProgressListener { _, _, _, _ -> if (cancelled.asBoolean) download.cancel() }
            val downloaded = download.execute(client.toNextcloudClient(context))
            checkCancelled(cancelled)
            if (!downloaded.isSuccess) {
                throw IOException(
                    "Could not download encrypted conflict target: ${downloaded.code}"
                )
            }
            if (!remote.etag.isNullOrEmpty() && remote.etag != download.etag) {
                throw IOException("Encrypted conflict target changed during comparison")
            }
            val plaintext = File(temporaryDirectory, "plaintext")
            val cipher = EncryptionUtils.getCipher(
                Cipher.DECRYPT_MODE,
                EncryptionUtils.decodeStringToBase64Bytes(entry.key),
                EncryptionUtils.decodeStringToBase64Bytes(entry.nonce)
            )
            val same = EncryptedFileContent.decryptAndCompare(
                encrypted,
                plaintext,
                local,
                cipher,
                cancelled
            )
            checkCancelled(cancelled)
            remote.fileLength = plaintext.length()
            if (!same && requirePreview) {
                val preview = retainPreview(plaintext, entry.filename)
                remote.storagePath = preview.absolutePath
                retainedPreview = preview
            }
            Log_OC.d(TAG, "Authenticated encrypted content comparison completed: identical=$same")
            completed = true
            return same
        } finally {
            if (!completed) retainedPreview?.delete()
            temporaryDirectory.deleteRecursively()
        }
    }

    private fun retainPreview(plaintext: File, filename: String): File {
        val previewDirectory = File(context.cacheDir, PREVIEW_DIRECTORY).apply { mkdirs() }
        val extension = filename.substringAfterLast('.', "bin")
            .takeIf { it.matches(Regex("[a-zA-Z0-9]{1,12}")) } ?: "bin"
        val preview = File.createTempFile("conflict-", ".$extension", previewDirectory)
        try {
            Files.move(plaintext.toPath(), preview.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            return preview
        } catch (e: Exception) {
            preview.delete()
            throw e
        }
    }

    companion object {
        private const val PREVIEW_DIRECTORY = "encrypted-conflicts"
        private const val TAG = "EncryptedUploadConflictChecker"

        fun rememberVerifiedCopy(context: Context, user: User, remote: OCFile, local: File, remoteVersion: String) {
            require(local.isFile && local.length() == remote.fileLength && !remote.etag.isNullOrEmpty())
            val identity = EncryptedFileComparisonCache.fingerprint(
                user.accountName,
                remote.remotePath,
                local.absolutePath
            )
            val version = EncryptedFileComparisonCache.version(local.length(), local.lastModified(), remoteVersion)
            EncryptedFileComparisonCache(ArbitraryDataProviderImpl(context), user.accountName)
                .write(identity, version, true, local.length())
        }

        @JvmStatic
        fun releasePreview(context: Context, file: OCFile?) {
            val path = file?.takeIf { it.isEncrypted }?.storagePath ?: return
            val preview = File(path)
            if (preview.parentFile?.canonicalFile == File(context.cacheDir, PREVIEW_DIRECTORY).canonicalFile) {
                preview.delete()
            }
        }

        @JvmStatic
        fun clearOrphanedPreviews(context: Context) {
            File(context.cacheDir, PREVIEW_DIRECTORY).listFiles()?.filter { it.isFile }?.forEach { it.delete() }
            context.cacheDir.listFiles()?.filter {
                it.isDirectory && it.name.startsWith("encrypted-comparison-")
            }?.forEach { it.deleteRecursively() }
        }
    }
}
