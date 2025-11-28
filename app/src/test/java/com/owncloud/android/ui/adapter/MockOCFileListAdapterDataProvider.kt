/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter

import com.nextcloud.client.database.entity.FileEntity
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.adapter.helper.OCFileListAdapterDataProvider
import com.owncloud.android.utils.MimeType

@Suppress("LongParameterList", "MagicNumber")
class MockOCFileListAdapterDataProvider : OCFileListAdapterDataProvider {

    private var offlineOCFile: OCFile? = null
    private var files = listOf<OCFile>()

    private fun getEntities(): List<FileEntity> = files.map { file ->
        FileEntity(
            id = file.fileId,
            name = file.fileName,
            path = file.remotePath ?: file.fileName,
            pathDecrypted = file.remotePath ?: file.fileName,
            contentType = file.mimeType ?: MimeType.FILE,
            accountOwner = file.ownerId ?: "unknown",
            favorite = if (file.isFavorite) 1 else 0,
            hidden = if (file.isHidden) 1 else 0,
            sharedViaLink = if (file.isSharedViaLink) 1 else 0,
            encryptedName = null,
            parent = file.parentId,
            creation = 0L,
            modified = 0L,
            contentLength = 0L,
            storagePath = file.storagePath,
            lastSyncDate = 0L,
            lastSyncDateForData = 0L,
            modifiedAtLastSyncForData = 0L,
            etag = file.etag,
            etagOnServer = null,
            permissions = null,
            remoteId = file.remoteId,
            localId = file.localId,
            updateThumbnail = 0,
            isDownloading = 0,
            isEncrypted = 0,
            etagInConflict = null,
            sharedWithSharee = 0,
            mountType = 0,
            hasPreview = 0,
            unreadCommentsCount = 0,
            ownerId = file.ownerId ?: "unknown",
            ownerDisplayName = null,
            note = null,
            sharees = null,
            richWorkspace = null,
            metadataSize = null,
            metadataLivePhoto = null,
            locked = 0,
            lockType = 0,
            lockOwner = null,
            lockOwnerDisplayName = null,
            lockOwnerEditor = null,
            lockTimestamp = 0L,
            lockTimeout = 0,
            lockToken = null,
            tags = null,
            metadataGPS = null,
            e2eCounter = 0L,
            internalTwoWaySync = 0L,
            internalTwoWaySyncResult = null,
            uploaded = 0L
        )
    }

    fun setEntities(files: List<OCFile>) {
        this.files = files
    }

    fun setOfflineFile(file: OCFile) {
        offlineOCFile = file
    }

    override fun convertToOCFiles(id: Long): List<OCFile> = if (offlineOCFile != null) {
        listOf(offlineOCFile!!)
    } else {
        listOf()
    }

    override suspend fun getFolderContent(id: Long): List<FileEntity> = getEntities().filter {
        it.parent == id && it.path != OCFile.ROOT_PATH
    }

    override fun createFileInstance(entity: FileEntity): OCFile = files.first { it.fileId == entity.id }
}
