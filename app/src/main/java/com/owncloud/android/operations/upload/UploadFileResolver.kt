/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.upload

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFileV1
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.utils.MimeType
import com.owncloud.android.utils.MimeTypeUtil
import java.io.File

@Suppress("DEPRECATION")
class UploadFileResolver(private val operation: UploadFileOperation) {

    companion object {
        fun obtainNewOCFileToUpload(remotePath: String?, localPath: String?, mimeType: String?): OCFile {
            val newFile = OCFile(remotePath).apply {
                setStoragePath(localPath)
                lastSyncDateForProperties = 0
                lastSyncDateForData = 0
            }

            if (!localPath.isNullOrEmpty()) {
                val localFile = File(localPath)
                newFile.fileLength = localFile.length()
                newFile.lastSyncDateForData = localFile.lastModified()
            }

            if (mimeType.isNullOrEmpty()) {
                newFile.mimeType = MimeTypeUtil.getBestMimeTypeByFilename(localPath)
            } else {
                newFile.mimeType = mimeType
            }

            return newFile
        }
    }

    val Any?.collidedFileNames: List<String>
        get() = when (this) {
            is DecryptedFolderMetadataFileV1 -> files.values.map { it.encrypted.filename }
            is DecryptedFolderMetadataFile -> metadata.files.values.map { it.filename }
            else -> emptyList()
        }

    fun updateSize(size: Long) {
        val storageManager = operation.uploadsStorageManager
        val ocUpload = storageManager.getUploadById(operation.ocUploadId)
        if (ocUpload != null) {
            ocUpload.fileSize = size
            storageManager.updateUpload(ocUpload)
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
        val parentPath = File(remotePath).parent
            ?.let { if (it.endsWith(OCFile.PATH_SEPARATOR)) it else it + OCFile.PATH_SEPARATOR }
            ?: return null

        val storageManager = operation.storageManager
        val parent = storageManager.getFileByPath(parentPath)
            ?: createLocalFolder(parentPath)
            ?: return null

        return OCFile(remotePath).apply {
            mimeType = MimeType.DIRECTORY
            parentId = parent.fileId
        }.also { storageManager.saveFile(it) }
    }

    fun updateOCFile(file: OCFile, remoteFile: RemoteFile) {
        file.creationTimestamp = remoteFile.creationTimestamp
        file.fileLength = remoteFile.length
        file.setMimeType(remoteFile.mimeType)
        file.modificationTimestamp = remoteFile.modifiedTimestamp
        file.modificationTimestampAtLastSyncForData = remoteFile.modifiedTimestamp
        file.setEtag(remoteFile.etag)
        file.setEtagOnServer(remoteFile.etag)
        file.setRemoteId(remoteFile.remoteId)
        file.setPermissions(remoteFile.permissions)
        file.uploadTimestamp = remoteFile.uploadTimestamp
        file.isPreviewAvailable = remoteFile.isHasPreview
    }
}
