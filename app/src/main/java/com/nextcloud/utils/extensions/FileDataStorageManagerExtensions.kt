/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.nextcloud.client.database.entity.FileEntity
import com.nextcloud.client.database.entity.model.ShareeKey
import com.nextcloud.client.database.entity.toOCCapability
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.status.OCCapability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val SHARE_PATH_QUERY_CHUNK_SIZE = 400

/**
 * Detects sharee additions/removals (by userId + shareType) for [remoteFiles], compared to what is stored
 * locally.
 */
fun FileDataStorageManager.areShareesChanged(remoteFiles: List<RemoteFile>): Boolean {
    if (remoteFiles.isEmpty()) {
        return false
    }

    val newShareesByPath = remoteFiles.asSequence()
        .filter { it.remotePath != null }
        .groupBy { it.remotePath as String }
        .mapValues { (_, files) ->
            files.asSequence()
                .flatMap { file -> file.sharees.orEmpty().asSequence() }
                .mapNotNull { sharee -> sharee.shareType?.let { "${sharee.userId}:${it.value}" } }
                .toSet()
        }

    val existingShareesByPath = newShareesByPath.keys
        .chunked(SHARE_PATH_QUERY_CHUNK_SIZE)
        .flatMap { chunk -> shareDao.getShareeKeys(chunk, user.accountName, ShareeKey.shareableShareTypeValues) }
        .groupBy(ShareeKey::path) { "${it.shareWith}:${it.shareType}" }
        .mapValues { (_, keys) -> keys.toSet() }

    return newShareesByPath.keys.any { path ->
        newShareesByPath[path].orEmpty() != existingShareesByPath[path].orEmpty()
    }
}

suspend fun FileDataStorageManager.saveShares(shares: List<OCShare>, accountName: String) {
    withContext(Dispatchers.IO) {
        val entities = shares.map { share ->
            share.toEntity(accountName)
        }

        shareDao.insertAll(entities)
    }
}

private const val GALLERY_DB_CHUNK_SIZE = 500

suspend fun FileDataStorageManager.getGalleryItemsPageSuspended(
    pathPrefix: String,
    mimeFilter: String?,
    limit: Int
): List<OCFile> {
    val result = ArrayList<OCFile>(minOf(limit, GALLERY_DB_CHUNK_SIZE))
    var offset = 0

    while (offset < limit) {
        val chunkLimit = minOf(GALLERY_DB_CHUNK_SIZE, limit - offset)
        val entities = fileDao.getGalleryItemsPageSuspended(
            user.accountName,
            pathPrefix,
            mimeFilter,
            chunkLimit,
            offset
        )

        entities.mapTo(result) { createFileInstance(it) }
        offset += entities.size

        if (entities.size < chunkLimit) break
    }

    return result
}

fun FileDataStorageManager.searchFilesByName(file: OCFile, accountName: String, query: String): List<OCFile> =
    fileDao.searchFilesInFolder(file.fileId, accountName, query).map {
        createFileInstance(it)
    }

fun FileDataStorageManager.getDecryptedPath(file: OCFile): String {
    val paths = mutableListOf<String>()
    var entity = fileDao.getFileByEncryptedRemotePath(file.remotePath, user.accountName)

    while (entity != null) {
        entity.name?.takeIf { it.isNotEmpty() }?.let {
            paths.add(it.removePrefix(OCFile.PATH_SEPARATOR))
        }
        entity = entity.parent?.let { fileDao.getFileById(it) } ?: break
    }

    return paths
        .reversed()
        .joinToString(OCFile.PATH_SEPARATOR)
}

fun FileDataStorageManager.getNonEncryptedSubfolders(id: Long, accountName: String): List<OCFile> =
    fileDao.getNonEncryptedSubfolders(id, accountName).map {
        createFileInstance(it)
    }

suspend fun FileDataStorageManager.getCapabilitiesByAccountName(accountName: String): OCCapability =
    capabilityDao.getByAccountName(accountName).toOCCapability()

private const val FILE_ID_CHUNK_SIZE = 100

private fun List<Long>.toEntitiesInOrder(loadChunk: (List<Long>) -> List<FileEntity>): List<FileEntity> {
    val byId = chunked(FILE_ID_CHUNK_SIZE).flatMap(loadChunk).associateBy { it.id }
    return mapNotNull { byId[it] }
}

fun FileDataStorageManager.getFolderContentEntities(parentId: Long): List<FileEntity> =
    fileDao.getFolderContentIds(parentId).toEntitiesInOrder(fileDao::getFilesByIds)

suspend fun FileDataStorageManager.getFolderContentEntitiesSuspended(parentId: Long): List<FileEntity> =
    fileDao.getFolderContentIdsSuspended(parentId).toEntitiesInOrderSuspended(this)

suspend fun FileDataStorageManager.getSharedFileEntities(accountName: String): List<FileEntity> =
    fileDao.getSharedFileIds(accountName).toEntitiesInOrderSuspended(this)

suspend fun FileDataStorageManager.getFavoriteFileEntities(accountName: String): List<FileEntity> =
    fileDao.getFavoriteFileIds(accountName).toEntitiesInOrderSuspended(this)

private suspend fun List<Long>.toEntitiesInOrderSuspended(storageManager: FileDataStorageManager): List<FileEntity> {
    val entities = chunked(FILE_ID_CHUNK_SIZE).flatMap { storageManager.fileDao.getFilesByIdsSuspended(it) }
    val byId = entities.associateBy { it.id }
    return mapNotNull { byId[it] }
}
