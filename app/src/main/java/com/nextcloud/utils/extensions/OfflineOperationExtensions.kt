/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.nextcloud.client.database.dao.OfflineOperationDao
import com.nextcloud.client.database.entity.OfflineOperationEntity

fun OfflineOperationDao.updatePathsIfParentPathMatches(oldPath: String?, newTopDir: String?) {
    if (oldPath.isNullOrEmpty() || newTopDir.isNullOrEmpty()) return

    getAll().forEach {
        val newPath = it.updatePathsIfParentPathMatches(oldPath, newTopDir)
        if (newPath != it.path) {

            // TODO add parent path so it can upload
            it.path = newPath
            update(it)
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

fun OfflineOperationEntity.updatePath(newParentPath: String?): String? {
    if (newParentPath.isNullOrEmpty() || path.isNullOrEmpty()) return null

    val segments = path!!.trim('/').split('/').toMutableList()

    if (segments.size == 1) {
        return newParentPath
    }

    segments.removeAt(0)

    return newParentPath + segments.joinToString(separator = "/") + "/"
}

fun OfflineOperationEntity.getParentPathFromPath(): String? {
    return path?.getParentPathFromPath()
}

private fun String?.getParentPathFromPath(): String {
    val trimmedPath = this?.trim('/')
    val firstDir = trimmedPath?.split('/')?.firstOrNull() ?: ""
    return "/$firstDir/"
}
