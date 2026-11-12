/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.thumbnail

import com.nextcloud.client.database.dao.UploadDao
import com.nextcloud.client.database.dao.FileSystemDao
import com.nextcloud.client.database.entity.UploadEntity
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus
import com.owncloud.android.db.OCUpload
import com.owncloud.android.db.UploadResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.upload.EncryptedFileComparisonCache
import com.owncloud.android.utils.MimeTypeUtil
import java.io.File

object UploadThumbnailSource {
    internal const val ENCRYPTED_FILE_TAG_BYTES = 16L
    private const val TAG = "UploadThumbnailSource"

    fun forUpload(upload: OCUpload, remote: OCFile?): OCFile {
        val local = readableFile(upload.localPath)
        if (local != null && MimeTypeUtil.isImageOrVideo(upload.mimeType)) {
            return OCFile(upload.remotePath).apply {
                storagePath = local.absolutePath
                mimeType = upload.mimeType
            }
        }
        return remote ?: OCFile(upload.remotePath).apply { mimeType = upload.mimeType }
    }

    @JvmStatic
    fun forEncryptedFile(
        file: OCFile,
        accountName: String,
        uploads: UploadDao,
        filesystem: FileSystemDao? = null
    ): File? {
        if (!file.isEncrypted || !MimeTypeUtil.isImageOrVideo(file)) return null
        val upload = uploads.getByRemotePath(file.remotePath, accountName)
            ?: uploads.getByRemotePath(file.decryptedRemotePath, accountName)
            ?: run {
                Log_OC.d(TAG, "No upload history for encrypted thumbnail: ${file.fileName}")
                return filesystem?.let { AutoUploadThumbnailSource.find(file, accountName, it) }
            }
        if (upload.accountName != accountName || upload.status != UploadStatus.UPLOAD_SUCCEEDED.value) return null
        if (upload.lastResult !in completedResults) return null
        val local = readableFile(upload.localPath) ?: run {
            Log_OC.d(TAG, "Local thumbnail source is unavailable: ${file.fileName}")
            return null
        }
        return local.takeIf { matchesCompletedUpload(it, upload) }.also {
            if (it == null) Log_OC.d(TAG, "Local thumbnail source changed since upload: ${file.fileName}")
        }
    }

    @JvmStatic
    fun cacheKey(file: File): String = "tlocal" + EncryptedFileComparisonCache.fingerprint(
        file.absolutePath,
        file.length().toString(),
        file.lastModified().toString()
    )

    private fun readableFile(path: String?): File? = path?.takeIf { it.isNotEmpty() }
        ?.let(::File)?.takeIf { it.isFile && it.canRead() }

    private fun matchesCompletedUpload(local: File, upload: UploadEntity): Boolean {
        val size = upload.fileSize?.takeIf { it >= 0 }
        // Successful transfers record ciphertext length; verified skips record plaintext length.
        val ciphertextSize = upload.lastResult == UploadResult.UPLOADED.value &&
            size == local.length() + ENCRYPTED_FILE_TAG_BYTES
        if (size != null && size != local.length() && !ciphertextSize) return false
        val completed = upload.uploadEndTimestampLong
        return completed == null || completed <= 0 || local.lastModified() <= completed
    }

    private val completedResults = setOf(
        UploadResult.UPLOADED.value,
        UploadResult.SKIPPED.value,
        UploadResult.SAME_FILE_CONFLICT.value
    )
}
