/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.shares.ShareeUser

object OCShareToOCFileConverter {
    private const val MILLIS_PER_SECOND = 1000

    /**
     * Generates a list of incomplete [OCFile] from a list of [OCShare]. Retrieving OCFile directly by path may fail
     * in cases like
     * when a shared file is located at a/b/c/d/a.txt. To display a.txt in the shared tab, the device needs the OCFile.
     * On first launch, the app may not be aware of the file until the exact path is accessed.
     *
     * Server implementation needed to get file size, thumbnails e.g. :
     * <a href="https://github.com/nextcloud/server/issues/4456g</a>.
     *
     * Note: This works only for files shared *by* the user, not files shared *with* the user.
     */
    @JvmStatic
    fun buildOCFilesFromShares(shares: List<OCShare>): List<OCFile> {
        val groupedByPath: Map<String, List<OCShare>> = shares
            .filter { it.path != null }
            .groupBy { it.path!! }
        return groupedByPath
            .map { (path: String, shares: List<OCShare>) -> buildOcFile(path, shares) }
            .sortedByDescending { it.firstShareTimestamp }
    }

    private fun buildOcFile(path: String, shares: List<OCShare>): OCFile {
        require(shares.all { it.path == path })
        // common attributes
        val firstShare = shares.first()
        val file = OCFile(path).apply {
            decryptedRemotePath = path
            ownerId = firstShare.userId
            ownerDisplayName = firstShare.ownerDisplayName
            isPreviewAvailable = firstShare.isHasPreview
            mimeType = firstShare.mimetype
            note = firstShare.note
            fileId = firstShare.fileSource
            remoteId = firstShare.remoteId.toString()
            // use first share timestamp as timestamp
            firstShareTimestamp = shares.minOf { it.sharedDate * MILLIS_PER_SECOND }
            // don't have file length or mod timestamp
            fileLength = -1
            modificationTimestamp = -1
            isFavorite = firstShare.isFavorite
        }
        if (shares.any { it.shareType in listOf(ShareType.PUBLIC_LINK, ShareType.EMAIL) }) {
            file.isSharedViaLink = true
        }
        if (shares.any { it.shareType !in listOf(ShareType.PUBLIC_LINK, ShareType.EMAIL) }) {
            file.isSharedWithSharee = true
            file.sharees = shares
                .filter { it.shareType != ShareType.PUBLIC_LINK && it.shareType != ShareType.EMAIL }
                .map {
                    ShareeUser(
                        userId = it.userId,
                        displayName = it.sharedWithDisplayName,
                        shareType = it.shareType
                    )
                }
        }
        return file
    }
}
