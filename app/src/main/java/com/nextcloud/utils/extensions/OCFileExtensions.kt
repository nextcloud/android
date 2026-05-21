/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.GalleryItems
import com.owncloud.android.datamodel.GalleryRow
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.OCFileDepth
import com.owncloud.android.datamodel.OCFileDepth.DeepLevel
import com.owncloud.android.datamodel.OCFileDepth.FirstLevel
import com.owncloud.android.datamodel.OCFileDepth.Root
import com.owncloud.android.utils.FileStorageUtils
import java.util.Calendar
import java.util.Date

fun List<OCFile>.filterFilenames(): List<OCFile> = distinctBy { it.fileName }

fun OCFile.isTempFile(): Boolean {
    val context = MainApp.getAppContext()
    val appTempPath = FileStorageUtils.getAppTempDirectoryPath(context)
    return storagePath?.startsWith(appTempPath) == true
}

fun OCFile?.isPNG(): Boolean {
    if (this == null) {
        return false
    }
    return "image/png".equals(mimeType, ignoreCase = true)
}

@Suppress("ReturnCount")
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

fun List<OCFile>.toGalleryItems(columns: Int, defaultSize: Int): List<GalleryItems> {
    if (isEmpty()) return emptyList()

    val calendar = Calendar.getInstance()
    return groupBy {
        calendar.timeInMillis = it.modificationTimestamp
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.timeInMillis
    }
        .map { (date, filesList) ->
            GalleryItems(date, transformToRows(filesList, columns, defaultSize))
        }
        .sortedByDescending { it.date }
}

private fun transformToRows(list: List<OCFile>, columns: Int, defaultSize: Int): List<GalleryRow> {
    if (list.isEmpty()) return emptyList()

    return list
        .sortedByDescending { it.modificationTimestamp }
        .chunked(columns)
        .map { chunk -> GalleryRow(chunk, defaultSize, defaultSize) }
}
