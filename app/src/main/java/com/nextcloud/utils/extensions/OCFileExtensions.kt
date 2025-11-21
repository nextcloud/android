/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.FileStorageUtils

fun List<OCFile>.hasSameContentAs(other: List<OCFile>): Boolean {
    if (this.size != other.size) return false

    if (this === other) return true

    for (i in this.indices) {
        val a = this[i]
        val b = other[i]

        if (a != b) return false
        if (a.fileId != b.fileId) return false
        if (a.etag != b.etag) return false
        if (a.modificationTimestamp != b.modificationTimestamp) return false

        if (a.fileLength != b.fileLength) return false
        if (a.isFavorite != b.isFavorite) return false

        if (a.fileName != b.fileName) return false
    }

    return true
}

fun List<OCFile>.filterFilenames(): List<OCFile> = distinctBy { it.fileName }

fun List<OCFile>.filterTempFilter(): List<OCFile> = filterNot { it.isTempFile() }

fun OCFile.isTempFile(): Boolean {
    val context = MainApp.getAppContext()
    val appTempPath = FileStorageUtils.getAppTempDirectoryPath(context)
    return storagePath?.startsWith(appTempPath) == true
}

fun List<OCFile>.filterHiddenFiles(): List<OCFile> = filterNot { it.isHidden }.distinct()

fun List<OCFile>.filterByMimeType(mimeType: String): List<OCFile> =
    filter { it.isFolder || it.mimeType.startsWith(mimeType) }

fun List<OCFile>.limitToPersonalFiles(userId: String): List<OCFile> = filter { file ->
    file.ownerId?.let { ownerId ->
        ownerId == userId && !file.isSharedWithMe && !file.mounted()
    } == true
}

fun OCFile.mediaSize(defaultThumbnailSize: Float): Pair<Int, Int> {
    val width = (imageDimension?.width?.toInt() ?: defaultThumbnailSize.toInt())
    val height = (imageDimension?.height?.toInt() ?: defaultThumbnailSize.toInt())
    return width to height
}
