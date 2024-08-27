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

fun OfflineOperationDao.updatePathAndSubPaths(oldPath: String, newPath: String) {
    val operationsToUpdate = getSubDirs(oldPath)

    operationsToUpdate.forEach { operation ->
        val newOperationPath = operation.path?.replaceFirst(oldPath, newPath)
        operation.path = newOperationPath
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

fun OfflineOperationDao.updatePathsIfParentPathMatches(oldPath: String?, newTopDir: String?, parentPath: String?) {
    if (oldPath.isNullOrEmpty() || newTopDir.isNullOrEmpty()) return

    getAll().forEach {
        val newPath = it.updatePathsIfParentPathMatches(oldPath, newTopDir)
        if (newPath != it.path) {
            it.parentPath = parentPath
            it.path = newPath
            update(it)
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

fun OfflineOperationEntity.updatePathsIfParentPathMatches(oldPath: String?, newTopDir: String?): String? {
    if (oldPath.isNullOrEmpty() || newTopDir.isNullOrEmpty()) return null

    val topDir = getTopParentPathFromPath()
    val oldTopDir = oldPath.getTopParentPathFromPath()
    return if (topDir == oldTopDir) {
        updatePath(newTopDir)
    } else {
        path
    }
}

@Suppress("ReturnCount")
fun OfflineOperationEntity.updatePath(newParentPath: String?): String? {
    if (newParentPath.isNullOrEmpty() || path.isNullOrEmpty()) return null

    val segments = path?.trim(DELIMITER)?.split(DELIMITER)?.toMutableList()

    if (segments?.size == 1) {
        return newParentPath
    }

    segments?.removeAt(0)

    return newParentPath + segments?.joinToString(separator = DELIMITER.toString()) + DELIMITER
}

fun OfflineOperationEntity.getTopParentPathFromPath(): String? = path?.getTopParentPathFromPath()

private fun String?.getTopParentPathFromPath(): String? {
    if (this == null) return null
    val trimmedPath = this.trim(DELIMITER)
    val firstDir = trimmedPath.split(DELIMITER).firstOrNull() ?: return null
    return DELIMITER + firstDir + DELIMITER
}

private fun OfflineOperationEntity.getParentPathFromPath(): String? {
    if (filename == null) return null
    return path.getParentPathFromPath(filename!!)
}

private fun String?.getParentPathFromPath(filename: String): String? {
    val pathParts = this?.trim(DELIMITER)?.split(DELIMITER) ?: return null
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
