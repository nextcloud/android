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
