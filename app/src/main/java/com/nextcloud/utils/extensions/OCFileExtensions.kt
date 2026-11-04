/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.google.gson.Gson
import com.nextcloud.client.database.entity.FileEntity
import com.nextcloud.client.database.entity.model.FolderMetadataUpdate
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.GalleryItems
import com.owncloud.android.datamodel.GalleryRow
import com.owncloud.android.datamodel.GalleryRowLayout
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.OCFileDepth
import com.owncloud.android.datamodel.OCFileDepth.DeepLevel
import com.owncloud.android.datamodel.OCFileDepth.FirstLevel
import com.owncloud.android.datamodel.OCFileDepth.Root
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareeUser
import com.owncloud.android.ui.events.EncryptionEvent
import com.owncloud.android.utils.FileStorageUtils
import java.io.File
import java.util.Calendar

private const val TAG = "OCFileExtensions"
private const val NO_LOCK_TYPE = -1
private const val FOLDER_CONTENT_LENGTH = 0L

private val gson = Gson()

private fun OCShare.toShareeUser() = ShareeUser(shareWith, sharedWithDisplayName, shareType)

fun OCFile.addShare(share: OCShare) {
    val sharee = share.toShareeUser()
    if (sharee !in sharees) sharees.add(sharee)
}

fun OCFile.removeShare(share: OCShare) {
    val sharee = share.toShareeUser()
    sharees.removeAll { it == sharee }
}

fun List<OCFile>.filterFilenames(): List<OCFile> = distinctBy { it.fileName }

fun OCFile.isTempFile(): Boolean {
    val context = MainApp.getAppContext()
    val appTempPath = FileStorageUtils.getAppTempDirectoryPath(context)
    return storagePath?.startsWith(appTempPath) == true
}

fun OCFile?.isPNG(): Boolean = this != null && "image/png".equals(mimeType, ignoreCase = true)

fun OCFile?.getDepth(): OCFileDepth? {
    if (this == null) {
        return null
    }

    // Check if it's the root directory
    if (this.isRootDirectory) {
        return Root
    }

    // If parent is root ("/"), this is a direct child of root
    val parentPath = this.parentRemotePath ?: return null
    if (parentPath == OCFile.ROOT_PATH) {
        return FirstLevel
    }

    // Otherwise, it's a subdirectory of a subdirectory
    return DeepLevel
}

fun List<OCFile>.toGalleryItems(layout: GalleryRowLayout): List<GalleryItems> {
    if (isEmpty()) return emptyList()

    val calendar = Calendar.getInstance()
    return distinctBy { it.fileId }.groupBy {
        calendar.timeInMillis = it.modificationTimestamp
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.timeInMillis
    }
        .map { (date, filesList) ->
            GalleryItems(date, transformToRows(filesList, layout))
        }
        .sortedByDescending { it.date }
}

private fun transformToRows(list: List<OCFile>, layout: GalleryRowLayout): List<GalleryRow> {
    if (list.isEmpty()) return emptyList()

    return list
        .sortedByDescending { it.modificationTimestamp }
        .chunked(layout.columns)
        .map { chunk -> GalleryRow(chunk, layout.measure(chunk)) }
}

fun OCFile?.isDownloadedFileChanged(serverFile: OCFile): Boolean {
    if (this == null || !isDown || serverFile.isFolder) {
        return false
    }

    return etag.eTagChanged(serverFile.etag)
}

fun OCFile.toEncryptionEvent(encrypt: Boolean): EncryptionEvent = EncryptionEvent(
    localId,
    remoteId,
    remotePath,
    encrypt
)

fun OCFile.createStoragePath(accountName: String) {
    val savePath = FileStorageUtils.getDefaultSavePathFor(accountName, this)
    val localDirectory = File(savePath)
    if (!localDirectory.exists() && !localDirectory.mkdirs()) {
        Log_OC.e(TAG, "Could not create local directory for internal two-way sync folder: $savePath")
        return
    }

    storagePath = savePath
}

fun OCFile.isLocalETagOutdated(): Boolean {
    val serverETag = etagInConflict
    return !serverETag.isNullOrEmpty() && etag.eTagChanged(serverETag)
}

fun OCFile.toFileEntity(accountName: String): FileEntity = FileEntity(
    id = fileId.takeIf { it > 0 },
    name = fileName,
    encryptedName = encryptedFileName,
    path = remotePath,
    pathDecrypted = decryptedRemotePath,
    parent = parentId,
    creation = creationTimestamp,
    modified = modificationTimestamp,
    contentType = mimeType,
    contentLength = fileLength,
    storagePath = storagePath,
    accountOwner = accountName,
    lastSyncDate = lastSyncDateForProperties,
    lastSyncDateForData = lastSyncDateForData,
    modifiedAtLastSyncForData = modificationTimestampAtLastSyncForData,
    etag = etag,
    etagOnServer = etagOnServer,
    sharedViaLink = isSharedViaLink.toInt(),
    permissions = permissions,
    remoteId = remoteId,
    localId = localId,
    updateThumbnail = isUpdateThumbnailNeeded.toInt(),
    isDownloading = isDownloading.toInt(),
    favorite = isFavorite.toInt(),
    hidden = shouldHide().toInt(),
    isEncrypted = isEncrypted.toInt(),
    etagInConflict = etagInConflict,
    sharedWithSharee = isSharedWithSharee.toInt(),
    mountType = null,
    hasPreview = isPreviewAvailable.toInt(),
    unreadCommentsCount = unreadCommentsCount,
    ownerId = ownerId,
    ownerDisplayName = ownerDisplayName,
    note = note,
    sharees = gson.toJson(sharees),
    richWorkspace = richWorkspace,
    metadataSize = gson.toJson(imageDimension),
    metadataLivePhoto = linkedFileIdForLivePhoto,
    locked = isLocked.toInt(),
    lockType = lockType?.value ?: NO_LOCK_TYPE,
    lockOwner = lockOwnerId,
    lockOwnerDisplayName = lockOwnerDisplayName,
    lockOwnerEditor = lockOwnerEditor,
    lockTimestamp = lockTimestamp,
    lockTimeout = lockTimeout.toInt(),
    lockToken = lockToken,
    tags = gson.toJson(tags),
    metadataGPS = gson.toJson(geoLocation),
    e2eCounter = e2eCounter,
    internalTwoWaySync = internalFolderSyncTimestamp,
    internalTwoWaySyncResult = internalFolderSyncResult,
    uploaded = uploadTimestamp,
    isReadOnly = isReadOnly.toInt()
)

fun OCFile.toFolderMetadataUpdate(accountName: String): FolderMetadataUpdate {
    val entity = toFileEntity(accountName)
    return FolderMetadataUpdate(
        id = fileId,
        modified = entity.modified,
        modifiedAtLastSyncForData = entity.modifiedAtLastSyncForData,
        parent = entity.parent,
        uploaded = entity.uploaded,
        creation = entity.creation,
        contentType = entity.contentType,
        contentLength = FOLDER_CONTENT_LENGTH,
        name = entity.name,
        path = entity.path,
        pathDecrypted = entity.pathDecrypted,
        accountOwner = entity.accountOwner,
        isEncrypted = entity.isEncrypted,
        isReadOnly = entity.isReadOnly,
        lastSyncDate = entity.lastSyncDate,
        lastSyncDateForData = entity.lastSyncDateForData,
        etag = entity.etag,
        etagOnServer = entity.etagOnServer,
        sharedViaLink = entity.sharedViaLink,
        sharedWithSharee = entity.sharedWithSharee,
        permissions = entity.permissions,
        remoteId = entity.remoteId,
        localId = entity.localId,
        favorite = entity.favorite,
        hidden = entity.hidden,
        unreadCommentsCount = entity.unreadCommentsCount,
        ownerId = entity.ownerId,
        ownerDisplayName = entity.ownerDisplayName,
        note = entity.note,
        sharees = entity.sharees,
        tags = entity.tags,
        richWorkspace = entity.richWorkspace,
        internalTwoWaySync = entity.internalTwoWaySync,
        internalTwoWaySyncResult = entity.internalTwoWaySyncResult
    )
}

private fun Boolean.toInt(): Int = if (this) 1 else 0
