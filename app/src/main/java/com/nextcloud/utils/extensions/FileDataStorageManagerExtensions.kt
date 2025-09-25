/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile

fun FileDataStorageManager.searchFilesByName(file: OCFile, accountName: String, query: String): List<OCFile> =
    fileDao.searchFilesInFolder(file.fileId, accountName, query).map {
        createFileInstance(it)
    }

fun FileDataStorageManager.getParentIdsOfSubfiles(paths: List<String>): List<Long> =
    fileDao.getParentIdsOfSubfiles(paths)

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

fun FileDataStorageManager.getSubfiles(id: Long, accountName: String): List<OCFile> =
    fileDao.getSubfiles(id, accountName).map {
        createFileInstance(it)
    }

fun FileDataStorageManager.getNonEncryptedSubfolders(id: Long, accountName: String): List<OCFile> =
    fileDao.getNonEncryptedSubfolders(id, accountName).map {
        createFileInstance(it)
    }
