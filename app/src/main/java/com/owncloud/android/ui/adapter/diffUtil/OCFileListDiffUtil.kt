/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.diffUtil

import androidx.recyclerview.widget.DiffUtil
import com.owncloud.android.datamodel.OCFile

class OCFileListDiffUtil() : DiffUtil.Callback() {
    private var oldList: List<OCFile> = listOf()
    private var newList: List<OCFile> = listOf()
    private var showHeader: Boolean = false

    fun updateLists(oldList: List<OCFile>, newList: List<OCFile>, showHeader: Boolean) {
        this.oldList = oldList
        this.newList = newList
        this.showHeader = showHeader
    }

    override fun getOldListSize(): Int {
        return oldList.size + if (showHeader) 2 else 1
    }

    override fun getNewListSize(): Int {
        return newList.size + if (showHeader) 2 else 1
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        if (oldItemPosition >= oldList.size || newItemPosition >= newList.size) return false
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        if (oldItemPosition >= oldList.size || newItemPosition >= newList.size) return false

        val oldFile = oldList[oldItemPosition]
        val newFile = newList[newItemPosition]

        return oldFile.modificationTimestamp == newFile.modificationTimestamp &&
            oldFile.fileLength == newFile.fileLength &&
            oldFile.fileName == newFile.fileName &&
            oldFile.isFavorite == newFile.isFavorite &&
            oldFile.isEncrypted == newFile.isEncrypted &&
            oldFile.isSharedWithMe == newFile.isSharedWithMe &&
            oldFile.isSharedWithSharee == newFile.isSharedWithSharee &&
            oldFile.isSharedViaLink == newFile.isSharedViaLink &&
            oldFile.isLocked == newFile.isLocked &&
            oldFile.unreadCommentsCount == newFile.unreadCommentsCount &&
            oldFile.etag == newFile.etag &&
            oldFile.linkedFileIdForLivePhoto == newFile.linkedFileIdForLivePhoto &&
            oldFile.tags == newFile.tags &&
            oldFile.isOfflineOperation == newFile.isOfflineOperation
    }
}
