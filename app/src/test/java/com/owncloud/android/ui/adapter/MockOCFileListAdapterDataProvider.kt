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

    private val directory = OCFile(OCFile.ROOT_PATH).apply {
        setFolder()
        fileId = 101L
        ownerId = "user123"
        remoteId = "0"
        fileId = 0
    }
    private val hidden = createTestOCFile(
        directory.fileId,
        "/.hidden.jpg",
        1,
        ownerId = "user123",
        mimeType = MimeType.JPEG,
        isHidden = true,
        localPath = "/local/hidden.jpg"
    )
    private val image = createTestOCFile(
        directory.fileId,
        "/image.jpg",
        2,
        ownerId = "user123",
        mimeType = MimeType.JPEG,
        localPath = "/local/image.jpg"
    )
    private val video = createTestOCFile(
        directory.fileId,
        "/video.mp4",
        3,
        ownerId = "user123",
        mimeType = "video/mp4",
        localPath = "/local/video.mp4"
    )
    private val temp = createTestOCFile(
        directory.fileId,
        "/temp.tmp",
        4,
        ownerId = "user123",
        mimeType = MimeType.FILE,
        localPath = "/local/temp.tmp"
    )
    private val otherUsersFile =
        createTestOCFile(202, "/other.jpg", 5, ownerId = "x", mimeType = MimeType.JPEG, localPath = "/local/other.jpg")
    private val personal = createTestOCFile(
        directory.fileId,
        "/personal.jpg",
        6,
        ownerId = "user123",
        mimeType = MimeType.JPEG,
        localPath = "/local/personal.jpg"
    )
    private val shared = createTestOCFile(
        directory.fileId,
        "/shared.jpg",
        7,
        ownerId = "user123",
        mimeType = MimeType.JPEG,
        isSharedViaLink = true,
        localPath = "/local/shared.jpg"
    )
    private val favorite = createTestOCFile(
        directory.fileId,
        "/favorite.jpg",
        8,
        ownerId = "user123",
        mimeType = MimeType.JPEG,
        isFavorite = true,
        localPath = "/local/favorite.jpg"
    )
    private val livePhotoImg = createTestOCFile(
        directory.fileId,
        "/live.jpg",
        9,
        ownerId = "user123",
        mimeType = MimeType.JPEG,
        localId = 77,
        localPath = "/local/live.jpg"
    )
    private val livePhotoVideo = createTestOCFile(
        directory.fileId,
        "/live_video.mp4",
        10,
        ownerId = "user123",
        mimeType = "video/mp4",
        localPath = "/local/live_video.mp4"
    ).apply {
        setLivePhoto("77")
    }
    private val offlineOCFile = createTestOCFile(
        directory.fileId,
        "/offline.jpg",
        11,
        ownerId = "user123",
        mimeType = MimeType.JPEG,
        localPath = "/local/offline.jpg"
    )
    private val files =
        listOf(hidden, image, video, temp, otherUsersFile, personal, shared, favorite, livePhotoImg, livePhotoVideo)
    private val entities: List<FileEntity> = files.map { file ->
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

    override fun convertToOCFiles(id: Long): List<OCFile> = listOf(offlineOCFile)

    override suspend fun getFolderContent(id: Long): List<FileEntity> = entities.filter { it.parent == id }

    override fun createFileInstance(entity: FileEntity): OCFile = files.first { it.fileId == entity.id }

    private fun createTestOCFile(
        parentId: Long,
        path: String,
        fileId: Long,
        ownerId: String? = null,
        mimeType: String? = MimeType.FILE,
        isHidden: Boolean = false,
        isFavorite: Boolean = false,
        isSharedViaLink: Boolean = false,
        localId: Long = -1,
        etag: String = "etag_$fileId",
        localPath: String? = null
    ): OCFile = OCFile(path).apply {
        this.parentId = parentId
        this.fileId = fileId
        this.remotePath = path
        this.ownerId = ownerId
        this.mimeType = mimeType
        this.isHidden = isHidden
        this.isFavorite = isFavorite
        this.isSharedViaLink = isSharedViaLink
        this.localId = localId
        this.etag = etag
        this.storagePath = localPath
    }
}
