/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter

import androidx.recyclerview.widget.DiffUtil
import com.owncloud.android.datamodel.OCFile

class OCFileDiffCallback : DiffUtil.ItemCallback<OCFile>() {
    override fun areItemsTheSame(oldItem: OCFile, newItem: OCFile): Boolean =
        oldItem.fileId == newItem.fileId && oldItem.remotePath == newItem.remotePath

    override fun areContentsTheSame(oldItem: OCFile, newItem: OCFile): Boolean = hasSameMetadata(oldItem, newItem) &&
        hasSameSyncState(oldItem, newItem) &&
        hasSameSharing(oldItem, newItem) &&
        hasSameIndicators(oldItem, newItem)

    private fun hasSameMetadata(oldItem: OCFile, newItem: OCFile): Boolean = oldItem.fileName == newItem.fileName &&
        oldItem.mimeType == newItem.mimeType &&
        oldItem.isFolder == newItem.isFolder &&
        oldItem.fileLength == newItem.fileLength &&
        oldItem.modificationTimestamp == newItem.modificationTimestamp

    private fun hasSameSyncState(oldItem: OCFile, newItem: OCFile): Boolean = oldItem.etag == newItem.etag &&
        oldItem.etagInConflict == newItem.etagInConflict &&
        oldItem.storagePath == newItem.storagePath &&
        oldItem.isDown == newItem.isDown &&
        oldItem.isUpdateThumbnailNeeded == newItem.isUpdateThumbnailNeeded &&
        oldItem.isOfflineOperation == newItem.isOfflineOperation

    private fun hasSameSharing(oldItem: OCFile, newItem: OCFile): Boolean = oldItem.isShared == newItem.isShared &&
        oldItem.isSharedViaLink == newItem.isSharedViaLink &&
        oldItem.isSharedWithMe == newItem.isSharedWithMe &&
        oldItem.isSharedWithSharee == newItem.isSharedWithSharee &&
        oldItem.firstShareTimestamp == newItem.firstShareTimestamp &&
        oldItem.ownerId == newItem.ownerId &&
        oldItem.sharees == newItem.sharees

    private fun hasSameIndicators(oldItem: OCFile, newItem: OCFile): Boolean =
        oldItem.isFavorite == newItem.isFavorite &&
            oldItem.isHidden == newItem.isHidden &&
            oldItem.isEncrypted == newItem.isEncrypted &&
            oldItem.isLocked == newItem.isLocked &&
            oldItem.isRecommendedFile == newItem.isRecommendedFile &&
            oldItem.unreadCommentsCount == newItem.unreadCommentsCount &&
            oldItem.tags == newItem.tags &&
            oldItem.linkedFileIdForLivePhoto == newItem.linkedFileIdForLivePhoto
}
