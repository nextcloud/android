/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter

import com.nextcloud.utils.extensions.saveShares
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.shares.ShareeUser
import com.owncloud.android.utils.FileStorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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
    fun buildOCFilesFromShares(shares: List<OCShare>): List<OCFile> = shares
        .filter { !it.path.isNullOrEmpty() }
        .groupBy { it.path!! }
        .filterKeys { path ->
            path.isNotEmpty() && path.startsWith(OCFile.PATH_SEPARATOR)
        }
        .map { (path, sharesForPath) ->
            buildOcFile(path, sharesForPath)
        }
        .sortedByDescending { it.firstShareTimestamp }

    suspend fun parseAndSaveShares(
        cachedFiles: List<OCFile>,
        data: List<Any>,
        storageManager: FileDataStorageManager?,
        accountName: String
    ): List<OCFile> = withContext(Dispatchers.IO) {
        if (data.isEmpty()) {
            return@withContext emptyList()
        }

        val shares = data.filterIsInstance<OCShare>()
        if (shares.isEmpty()) {
            return@withContext emptyList()
        }

        val newShares = shares.filter { share ->
            cachedFiles.none { file -> file.decryptedRemotePath == share.path }
        }

        if (newShares.isEmpty()) {
            return@withContext cachedFiles
        }

        val files = buildOCFilesFromShares(newShares)
        val baseSavePath = FileStorageUtils.getSavePath(accountName)

        val newFiles = files.map { file ->
            if (!file.isFolder && (file.storagePath == null || !File(file.storagePath).exists())) {
                val fullPath = baseSavePath + file.decryptedRemotePath
                val candidate = File(fullPath)
                if (candidate.exists()) {
                    file.storagePath = candidate.absolutePath
                    file.lastSyncDateForData = candidate.lastModified()
                }
            }
            storageManager?.saveFile(file)
            file
        }

        storageManager?.saveShares(newShares, accountName)
        cachedFiles + newFiles
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
