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
     * Generates a list of incomplete [OCFile] from a list of [OCShare]
     *
     * This is actually pretty complex as we get one [OCShare] item for each shared instance for the same folder
     *
     * **THIS ONLY WORKS WITH FILES SHARED *BY* THE USER, NOT FOR SHARES *WITH* THE USER**
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
                .map { ShareeUser(it.shareWith, it.sharedWithDisplayName, it.shareType) }
        }
        return file
    }
}
