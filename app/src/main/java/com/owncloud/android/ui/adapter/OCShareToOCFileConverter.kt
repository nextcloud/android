/*
 * Nextcloud Android client application
 *
 * @author Álvaro Brey Vilas
 * Copyright (C) 2022 Álvaro Brey Vilas
 * Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
