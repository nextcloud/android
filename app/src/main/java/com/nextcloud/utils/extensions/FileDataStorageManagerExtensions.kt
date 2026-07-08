/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.nextcloud.client.database.entity.toOCCapability
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.status.OCCapability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun FileDataStorageManager.saveShares(shares: List<OCShare>, accountName: String) {
    withContext(Dispatchers.IO) {
        val entities = shares.map { share ->
            share.toEntity(accountName)
        }

        shareDao.insertAll(entities)
    }
}

private const val GALLERY_DB_CHUNK_SIZE = 500

/**
 * Loads gallery items ordered by modification date (newest first), bounded to [limit] entries.
 *
 * The database is read in chunks of [GALLERY_DB_CHUNK_SIZE] rows so that a single query never
 * materializes a result set larger than the Android [android.database.CursorWindow] can hold,
 * which otherwise crashes on very large galleries.
 *
 * @param pathPrefix only items whose remote path starts with this prefix are returned ("/" matches all)
 * @param mimeFilter optional content-type LIKE pattern (e.g. "image/%" or "video/%"); null keeps images and videos
 * @param limit maximum number of items to load
 */
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
