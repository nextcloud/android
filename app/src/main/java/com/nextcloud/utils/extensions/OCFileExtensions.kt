/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.FileStorageUtils

fun List<OCFile>.filterFilenames(): List<OCFile> = distinctBy { it.fileName }

fun List<OCFile>.filterTempFilter(): List<OCFile> = filterNot { it.isTempFile() }

fun OCFile.isTempFile(): Boolean {
    val context = MainApp.getAppContext()
    val appTempPath = FileStorageUtils.getAppTempDirectoryPath(context)
    return storagePath?.startsWith(appTempPath) == true
}

fun List<OCFile>.setEncryptionAttributeForItemId(
    fileId: String,
    encrypted: Boolean,
    storageManager: FileDataStorageManager
) {
    find { it.remoteId == fileId }?.let { file ->
        file.apply {
            isEncrypted = encrypted
            setE2eCounter(0L)
            storageManager.saveFile(this)
        }
    }
}

fun List<OCFile>.refreshCommentsCount(remoteId: String) {
    find { it.remoteId == remoteId }?.let { file ->
        file.unreadCommentsCount = 0
    }
}

fun List<OCFile>.getHiddenFilenames(): List<String> {
    return filter { it.shouldHide() }.map { it.fileName }
}

fun List<OCFile>.filterHiddenFiles(): List<OCFile> = filterNot { it.isHidden }.distinct()

fun List<OCFile>.filterByMimeType(mimeType: String): List<OCFile> =
    filter { it.isFolder || it.mimeType.startsWith(mimeType) }

fun List<OCFile>.limitToPersonalFiles(userId: String): List<OCFile> = filter { file ->
    file.ownerId?.let { ownerId ->
        ownerId == userId && !file.isSharedWithMe && !file.isGroupFolder
    } == true
}

fun List<OCFile>.addOfflineOperations(storageManager: FileDataStorageManager, fileId: Long) {
    val offlineOperations = storageManager.offlineOperationsRepository.convertToOCFiles(fileId)
    if (offlineOperations.isEmpty()) {
        return
    }

    val result = offlineOperations.filter { offlineFile ->
        none { file -> file.decryptedRemotePath == offlineFile.decryptedRemotePath }
    }

    toMutableList().addAll(result)
}
