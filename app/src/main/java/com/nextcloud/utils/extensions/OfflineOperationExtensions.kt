/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.nextcloud.client.database.dao.OfflineOperationDao
import com.nextcloud.client.database.entity.OfflineOperationEntity

private const val DELIMITER = '/'

fun OfflineOperationDao.updatePathAndSubPaths(
    oldPath: String,
    newPath: String,
    oldFileName: String,
    newFileName: String
) {
    val operationsToUpdate = getSubDirs(oldPath, oldFileName)

    operationsToUpdate.forEach { operation ->
        val newOperationFileName = operation.filename?.replaceFirst(oldFileName, newFileName)
        val newOperationPath = operation.path?.replaceFirst(oldPath, newPath)
        operation.path = newOperationPath
        operation.filename = newOperationFileName
        update(operation)
    }
}

fun OfflineOperationDao.deleteSubDirIfParentPathMatches(filename: String) {
    val targetPath = DELIMITER + filename + DELIMITER
    getAll().forEach {
        val entityParentPath = it.getParentPathFromPath()
        if (entityParentPath == targetPath) {
            delete(it)
        }
    }
}


fun OfflineOperationDao.updateNextOperationsParentPaths(currentOperation: OfflineOperationEntity) {
    getAll().forEach { nextOperation ->
        val nextOperationParentPath = nextOperation.getTopParentPathFromPath()
        val currentOperationParentPath = currentOperation.getTopParentPathFromPath()
        if (nextOperationParentPath == currentOperationParentPath) {
            nextOperation.parentPath = currentOperationParentPath
            update(nextOperation)
        }
    }
}

fun OfflineOperationEntity.getTopParentPathFromPath(): String? {
    if (path == null) return null
    val trimmedPath = path!!.trim(DELIMITER)
    val firstDir = trimmedPath.split(DELIMITER).firstOrNull() ?: return null
    return DELIMITER + firstDir + DELIMITER
}

private fun OfflineOperationEntity.getParentPathFromPath(): String? {
    if (filename == null) return null

    val pathParts = path?.trim(DELIMITER)?.split(DELIMITER) ?: return null
    val targetIndex = pathParts.indexOf(filename)
    val result = if (targetIndex >= 0) {
        if (targetIndex == 0) filename else pathParts[targetIndex - 1]
    } else {
        null
    }

    return if (result != null) {
        DELIMITER + result + DELIMITER
    } else {
        null
    }
}
