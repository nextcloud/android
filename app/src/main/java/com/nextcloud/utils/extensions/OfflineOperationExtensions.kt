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
        val nextOperationParentPath = nextOperation.getParentPathFromPath()
        val currentOperationParentPath = currentOperation.getParentPathFromPath()
        if (nextOperationParentPath == currentOperationParentPath) {
            nextOperation.parentPath = currentOperationParentPath
            update(nextOperation)
        }
    }
}

fun OfflineOperationEntity.updatePathsIfParentPathMatches(oldPath: String?, newTopDir: String?): String? {
    if (oldPath.isNullOrEmpty() || newTopDir.isNullOrEmpty()) return null

    val topDir = getParentPathFromPath()
    val oldTopDir = oldPath.getParentPathFromPath()
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

fun OfflineOperationEntity.getParentPathFromPath(): String? = path?.getParentPathFromPath()

private fun String?.getParentPathFromPath(): String {
    val trimmedPath = this?.trim(DELIMITER)
    val firstDir = trimmedPath?.split(DELIMITER)?.firstOrNull() ?: ""
    return DELIMITER + firstDir + DELIMITER
}
