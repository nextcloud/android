/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.files

import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.MimeTypeUtil

class FileSelection(private val files: Collection<OCFile>, private val storageManager: FileDataStorageManager) {

    val isSingleSelection = files.size == SINGLE_SELECT_ITEMS
    val containsFolder = files.any { it.isFolder }
    val containsEncrypted = files.any { it.isEncrypted }
    val containsEncryptedFile = files.any { !it.isFolder && it.isEncrypted }
    val containsEncryptedFolder = files.any { it.isFolder && it.isEncrypted }
    val containsLocked = files.any { it.isLocked }
    val containsShared = files.any { it.isSharedWithMe || it.isSharedViaLink || it.isSharedWithSharee }
    val anyDown = files.any { it.isDown }
    val allDown = files.all { it.isDown }
    val allFavorites = files.all { it.isFavorite }
    val noneFavorite = files.none { it.isFavorite }

    val firstFile: OCFile
        get() = files.first()

    val isSingleFile: Boolean
        get() = isSingleSelection && !firstFile.isFolder

    val isSingleFolder: Boolean
        get() = isSingleSelection && firstFile.isFolder

    val isSingleImage: Boolean
        get() = isSingleSelection && MimeTypeUtil.isImage(firstFile)

    val isSingleMedia: Boolean
        get() = isSingleSelection && (MimeTypeUtil.isVideo(firstFile) || MimeTypeUtil.isAudio(firstFile))

    val isEncryptedFolder: Boolean
        get() = isSingleFolder && firstFile.isEncrypted

    val isGroupFolder: Boolean
        get() = firstFile.mounted()

    fun isEmptyFolder(): Boolean {
        if (!isSingleFolder) return false

        val file = firstFile

        return file.fileLength == EMPTY_FILE_LENGTH && storageManager.getFolderContent(file, false).isEmpty()
    }

    fun hasEncryptedParent(): Boolean = storageManager.getFileById(firstFile.parentId)?.isEncrypted == true

    fun containsReadOnlyFile(): Boolean = files.any { storageManager.isReadOnly(it) }

    fun containsEncryptedFolderContent(): Boolean = containsEncryptedFolder || containsFolderWithEncryptedChild()

    private fun containsFolderWithEncryptedChild(): Boolean = files.any { file ->
        file.isFolder && storageManager.getFolderContent(file, false).any { it.isEncrypted }
    }

    companion object {
        const val SINGLE_SELECT_ITEMS = 1
        private const val EMPTY_FILE_LENGTH = 0L
    }
}
