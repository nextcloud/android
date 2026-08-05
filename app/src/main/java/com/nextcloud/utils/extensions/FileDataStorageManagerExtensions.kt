/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.os.RemoteException
import com.nextcloud.client.database.entity.toOCCapability
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.status.OCCapability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.asSequence

// matches FileDataStorageManager.getSharesWithForAFile, excludes public link shares
private val shareableShareTypes = listOf(
    ShareType.USER,
    ShareType.GROUP,
    ShareType.EMAIL,
    ShareType.FEDERATED,
    ShareType.FEDERATED_GROUP,
    ShareType.ROOM,
    ShareType.CIRCLE
)

// keeps queries well under SQLite's bound-parameter limit
private const val SHARE_PATH_QUERY_CHUNK_SIZE = 400

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

    val existingShareesByPath = queryLocalShareeKeysByPath(newShareesByPath.keys, user.accountName)

    return newShareesByPath.keys.any { path ->
        newShareesByPath[path].orEmpty() != existingShareesByPath[path].orEmpty()
    }
}

private fun FileDataStorageManager.queryLocalShareeKeysByPath(
    paths: Set<String>,
    accountName: String
): Map<String, Set<String>> {
    val result = mutableMapOf<String, MutableSet<String>>()

    paths.toList().chunked(SHARE_PATH_QUERY_CHUNK_SIZE).forEach { chunk ->
        queryLocalShareeKeysChunk(chunk, accountName, result)
    }

    return result
}

private fun FileDataStorageManager.queryLocalShareeKeysChunk(
    paths: List<String>,
    accountName: String,
    result: MutableMap<String, MutableSet<String>>
) {
    val pathPlaceholders = paths.joinToString(",") { "?" }
    val shareTypeFilter = shareableShareTypes.joinToString(" OR ") { "${ProviderTableMeta.OCSHARES_SHARE_TYPE} = ?" }
    val selection = "${ProviderTableMeta.OCSHARES_PATH} IN ($pathPlaceholders) AND " +
        "${ProviderTableMeta.OCSHARES_ACCOUNT_OWNER} = ? AND ($shareTypeFilter)"
    val selectionArgs = (paths + accountName + shareableShareTypes.map { it.value.toString() }).toTypedArray()

    val cursor = if (contentResolver != null) {
        contentResolver.query(ProviderTableMeta.CONTENT_URI_SHARE, null, selection, selectionArgs, null)
    } else {
        try {
            contentProviderClient?.query(ProviderTableMeta.CONTENT_URI_SHARE, null, selection, selectionArgs, null)
        } catch (e: RemoteException) {
            Log_OC.e(javaClass.simpleName, "Could not get list of shares: ${e.message}", e)
            null
        }
    }

    cursor?.use {
        val pathIndex = it.getColumnIndex(ProviderTableMeta.OCSHARES_PATH)
        val shareWithIndex = it.getColumnIndex(ProviderTableMeta.OCSHARES_SHARE_WITH)
        val shareTypeIndex = it.getColumnIndex(ProviderTableMeta.OCSHARES_SHARE_TYPE)

        while (it.moveToNext()) {
            val path = it.getString(pathIndex) ?: continue
            val key = "${it.getString(shareWithIndex)}:${it.getInt(shareTypeIndex)}"
            result.getOrPut(path) { mutableSetOf() }.add(key)
        }
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
