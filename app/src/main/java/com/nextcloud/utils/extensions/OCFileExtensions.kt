/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.owncloud.android.datamodel.OCFile

fun List<OCFile>.filterFilenames(): List<OCFile> = distinctBy { it.fileName }

fun List<OCFile>.filterTempFilter(): List<OCFile> = filterNot { it.isTempFile() }.distinct()

fun OCFile.isTempFile(): Boolean {
    return storagePath
        ?.split(OCFile.PATH_SEPARATOR)
        ?.any { it.isNotEmpty() && StringConstants.TEMP.contains(it.lowercase()) } == true
}

fun List<OCFile>.filterHiddenFiles(): List<OCFile> = filterNot { it.isHidden }.distinct()

fun List<OCFile>.filterByMimeType(mimeType: String): List<OCFile> =
    filter { it.isFolder || it.mimeType.startsWith(mimeType) }

fun List<OCFile>.limitToPersonalFiles(userId: String): List<OCFile> = filter { file ->
    file.ownerId?.let { ownerId ->
        ownerId == userId && !file.isSharedWithMe && !file.isGroupFolder
    } == true
}
