/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.nextcloud.client.database.dao.FileDao
import com.nextcloud.client.database.entity.toOCCapability
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.MimeTypeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

suspend fun FileDataStorageManager.saveShares(shares: List<OCShare>, accountName: String) {
    withContext(Dispatchers.IO) {
        val entities = shares.map { share ->
            share.toEntity(accountName)
        }

        shareDao.insertAll(entities)
    }
}

suspend fun FileDataStorageManager.getAllGalleryItemsSuspended(): List<OCFile> {
    val fileEntities = fileDao.getGalleryItemsSuspended(0, Long.MAX_VALUE, user.accountName)
    return fileEntities.map { createFileInstance(it) }
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

fun FileDataStorageManager.moveFiles(ocFile: OCFile?, targetPath: String, targetParentPath: String) {
    Log_OC.d(
        FileDataStorageManager.TAG,
        ("moveLocalFile ==> ocFile: "
            + (ocFile?.remotePath)
            + " targetPath: "
            + targetPath
            + " targetParentPath: "
            + targetParentPath)
    )

    if (ocFile == null) {
        Log_OC.e(FileDataStorageManager.TAG, "moveLocalFile: file is null, skipping")
        return
    }

    if (!ocFile.fileExists()) {
        Log_OC.e(FileDataStorageManager.TAG, "moveLocalFile: file does not exist, skipping")
        return
    }

    if (OCFile.ROOT_PATH == ocFile.fileName) {
        Log_OC.e(FileDataStorageManager.TAG, "moveLocalFile: cannot move root path")
        return
    }

    if (ocFile.remotePath == targetPath) {
        Log_OC.e(FileDataStorageManager.TAG, "moveLocalFile: source and target paths are identical, skipping")
        return
    }

    val targetParent = getFileByPath(targetParentPath)
    if (targetParent == null) {
        Log_OC.e(FileDataStorageManager.TAG, "moveLocalFile: target parent folder not found: $targetParentPath")
        return
    }

    if (!targetParent.isFolder) {
        Log_OC.e(FileDataStorageManager.TAG, "moveLocalFile: target parent is not a folder: $targetParentPath")
        return
    }

    val oldPath: String = ocFile.remotePath
    val accountName = user.accountName
    val defaultSavePath = FileStorageUtils.getSavePath(accountName)

    val originalMediaPaths =
        fileDao.moveFilesInDb(oldPath, targetPath, defaultSavePath, targetParent.fileId, accountName)

    moveLocalFiles(accountName, ocFile, defaultSavePath, targetPath)

    for (originalMediaPath in originalMediaPaths) {
        deleteFileInMediaScan(originalMediaPath)
        val newMediaPath = defaultSavePath + targetPath + originalMediaPath.substring(
            (defaultSavePath + oldPath).length
        )
        FileDataStorageManager.triggerMediaScan(newMediaPath)
    }
}

private fun moveLocalFiles(accountName: String, ocFile: OCFile, defaultSavePath: String, targetPath: String) {
    val localFile = File(FileStorageUtils.getDefaultSavePathFor(accountName, ocFile))
    if (!localFile.exists()) {
        Log_OC.d(FileDataStorageManager.TAG, "moveLocalFile: no local file to move at " + localFile.absolutePath)
        return
    }

    val targetFile = File(defaultSavePath + targetPath)
    val targetFolder = targetFile.getParentFile()
    if (targetFolder != null && !targetFolder.exists() && !targetFolder.mkdirs()) {
        Log_OC.e(
            FileDataStorageManager.TAG,
            "moveLocalFile: failed to create parent folder " + targetFolder.absolutePath
        )
    }

    if (!localFile.renameTo(targetFile)) {
        Log_OC.e(
            FileDataStorageManager.TAG, ("moveLocalFile: failed to rename " + localFile.absolutePath
                + " to " + targetFile.absolutePath)
        )
        return
    }
}

private fun FileDao.moveFilesInDb(
    oldPath: String,
    targetPath: String,
    defaultSavePath: String,
    targetParentId: Long,
    accountName: String
): List<String> {
    val entities = getFolderWithDescendants("$oldPath%", accountName)
    val oldStoragePrefix = defaultSavePath + oldPath
    val newStoragePrefix = defaultSavePath + targetPath

    val originalMediaPaths = entities
        .filter { MimeTypeUtil.isMedia(it.contentType) && it.storagePath?.startsWith(oldStoragePrefix) == true }
        .mapNotNull { it.storagePath }

    val updated = entities.map { entity ->
        val currentPath = entity.path.orEmpty()
        val newPath = targetPath + currentPath.substring(oldPath.length)
        entity.copy(
            path = newPath,
            pathDecrypted = if (entity.isEncrypted == 0) newPath else entity.pathDecrypted,
            storagePath = if (entity.storagePath?.startsWith(oldStoragePrefix) == true) {
                newStoragePrefix + entity.storagePath.substring(oldStoragePrefix.length)
            } else {
                entity.storagePath
            },
            parent = if (currentPath == oldPath) targetParentId else entity.parent
        )
    }

    updateAll(updated)
    return originalMediaPaths
}
