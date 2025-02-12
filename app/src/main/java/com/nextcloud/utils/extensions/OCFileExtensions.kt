/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile

fun List<OCFile>.getHiddenFilenames(): List<String> {
    return filter { it.shouldHide() }.map { it.fileName }
}

fun List<OCFile>.filterHiddenFiles(): List<OCFile> = filterNot { it.isHidden }.distinct()

fun List<OCFile>.filterByMimeType(mimeType: String): List<OCFile> =
    filter { it.isFolder || it.mimeType.startsWith(mimeType) }

fun List<OCFile>.limitToPersonalFiles(userId: String): List<OCFile> =
    filter { file ->
        file.ownerId?.let { ownerId ->
            ownerId == userId && !file.isSharedWithMe && !file.isGroupFolder
        } ?: false
    }

fun List<OCFile>.addOfflineOperations(
    storageManager: FileDataStorageManager,
    fileId: Long
) {
    val offlineOperations = storageManager.offlineOperationsRepository.convertToOCFiles(fileId)
    if (offlineOperations.isEmpty()) {
        return
    }

    val result = offlineOperations.filter { offlineFile ->
        none { file -> file.decryptedRemotePath == offlineFile.decryptedRemotePath }
    }

    this.toMutableList().addAll(result)
}
