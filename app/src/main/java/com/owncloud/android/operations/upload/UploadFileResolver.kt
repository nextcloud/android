/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.upload

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import com.nextcloud.utils.extensions.toOCFile
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFileV1
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.utils.MimeType
import com.owncloud.android.utils.MimeTypeUtil
import java.io.File

class UploadFileResolver(private val operation: UploadFileOperation) {

    companion object {
        @JvmStatic
        fun obtainNewOCFileToUpload(remotePath: String?, localPath: String?, mimeType: String?): OCFile =
            OCFile(remotePath).apply {
                setStoragePath(localPath)
                lastSyncDateForProperties = 0
                lastSyncDateForData = 0
                if (!localPath.isNullOrEmpty()) {
                    val localFile = File(localPath)
                    fileLength = localFile.length()
                    lastSyncDateForData = localFile.lastModified()
                }
                setMimeType(
                    mimeType.takeUnless { it.isNullOrEmpty() }
                        ?: MimeTypeUtil.getBestMimeTypeByFilename(localPath.orEmpty())
                )
            }
    }

    @Suppress("DEPRECATION")
    fun getCollidedFileNames(metadata: Any?): List<String> = when (metadata) {
        is DecryptedFolderMetadataFileV1 -> metadata.files.values.map { it.encrypted.filename }
        is DecryptedFolderMetadataFile -> metadata.metadata.files.values.map { it.filename }
        else -> emptyList()
    }

    fun updateSize(size: Long) {
        val storageManager = operation.uploadsStorageManager
        storageManager.getUploadById(operation.ocUploadId)?.let {
            it.fileSize = size
            storageManager.updateUpload(it)
        }
    }

    fun createNewOCFile(fileToBeUploaded: OCFile, newRemotePath: String): OCFile = OCFile(newRemotePath).apply {
        creationTimestamp = fileToBeUploaded.creationTimestamp
        fileLength = fileToBeUploaded.fileLength
        mimeType = fileToBeUploaded.mimeType
        modificationTimestamp = fileToBeUploaded.modificationTimestamp
        modificationTimestampAtLastSyncForData = fileToBeUploaded.modificationTimestampAtLastSyncForData
        etag = fileToBeUploaded.etag
        lastSyncDateForProperties = fileToBeUploaded.lastSyncDateForProperties
        lastSyncDateForData = fileToBeUploaded.lastSyncDateForData
        storagePath = fileToBeUploaded.storagePath
        parentId = fileToBeUploaded.parentId
    }

    fun createLocalFolder(remotePath: String): OCFile? {
        val storageManager = operation.storageManager
        val parent = File(remotePath).parent
            ?.let { if (it.endsWith(OCFile.PATH_SEPARATOR)) it else it + OCFile.PATH_SEPARATOR }
            ?.let { storageManager.getFileByPath(it) ?: createLocalFolder(it) }
            ?: return null

        return OCFile(remotePath).apply {
            mimeType = MimeType.DIRECTORY
            parentId = parent.fileId
        }.also { storageManager.saveFile(it) }
    }

    @SuppressFBWarnings(
        value = ["CE_CLASS_ENVY"],
        justification = "Upload completion must refresh remote properties without replacing the local file identity."
    )
    fun updateOCFile(file: OCFile, remoteFile: RemoteFile) {
        val remote = remoteFile.toOCFile()
        file.apply {
            creationTimestamp = remote.creationTimestamp
            fileLength = remote.fileLength
            mimeType = remote.mimeType
            modificationTimestamp = remote.modificationTimestamp
            modificationTimestampAtLastSyncForData = remote.modificationTimestamp
            etag = remote.etag
            etagOnServer = remote.etag
            remoteId = remote.remoteId
            isLocked = remote.isLocked
            lockType = remote.lockType
            lockOwnerId = remote.lockOwnerId
            lockOwnerDisplayName = remote.lockOwnerDisplayName
            lockOwnerEditor = remote.lockOwnerEditor
            lockTimestamp = remote.lockTimestamp
            lockTimeout = remote.lockTimeout
            lockToken = remote.lockToken
            permissions = remote.permissions
            uploadTimestamp = remote.uploadTimestamp
            isPreviewAvailable = remote.isPreviewAvailable
            imageDimension = remote.imageDimension
            geoLocation = remote.geoLocation
        }
    }
}
