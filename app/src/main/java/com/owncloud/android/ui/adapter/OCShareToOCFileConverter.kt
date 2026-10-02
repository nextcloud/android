/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter

import com.nextcloud.client.account.User
import com.nextcloud.utils.extensions.saveShares
import com.nextcloud.utils.share.UnifiedShareSharees
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
    private val LINK_SHARE_TYPES = setOf(ShareType.PUBLIC_LINK, ShareType.EMAIL)

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
    fun buildOCFilesFromShares(shares: List<OCShare>, storageManager: FileDataStorageManager): List<OCFile> = shares
        .filter { !it.path.isNullOrEmpty() }
        .groupBy { it.path!! }
        .filterKeys { path ->
            path.isNotEmpty() && path.startsWith(OCFile.PATH_SEPARATOR)
        }
        .map { (path, sharesForPath) ->
            buildOCFile(path, sharesForPath, storageManager)
        }
        .sortedByDescending { it.firstShareTimestamp }

    suspend fun parseAndSaveShares(
        cachedFiles: List<OCFile>,
        data: List<Any>,
        storageManager: FileDataStorageManager,
        user: User
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
            UnifiedShareSharees.fill(user, cachedFiles)
            return@withContext cachedFiles
        }

        val baseSavePath = FileStorageUtils.getSavePath(user.accountName)
        val newFiles = buildOCFilesFromShares(newShares, storageManager).onEach { it.resolveStoragePath(baseSavePath) }
        val files = (cachedFiles + newFiles).distinctBy { it.remotePath }

        UnifiedShareSharees.fill(user, files)
        newFiles.forEach(storageManager::saveFile)
        storageManager.saveShares(newShares, user.accountName)

        files
    }

    private fun OCFile.resolveStoragePath(baseSavePath: String) {
        if (isFolder || (storagePath != null && File(storagePath).exists())) {
            return
        }

        val candidate = File(baseSavePath + decryptedRemotePath)
        if (candidate.exists()) {
            storagePath = candidate.absolutePath
            lastSyncDateForData = candidate.lastModified()
        }
    }

    private fun buildOCFile(path: String, shares: List<OCShare>, storageManager: FileDataStorageManager): OCFile {
        require(shares.all { it.path == path })

        val firstShare = shares.first()
        val firstShareTimestamp = shares.minOf { it.sharedDate * MILLIS_PER_SECOND }

        val linkShares = shares.filter { it.shareType in LINK_SHARE_TYPES }
        val userShares = shares.filter { it.shareType !in LINK_SHARE_TYPES }

        val file = (storageManager.getFileByDecryptedRemotePath(path) ?: newOCFile(path))

        file.applyShareData(firstShare, firstShareTimestamp)

        file.isSharedViaLink = linkShares.isNotEmpty()
        file.isSharedWithSharee = userShares.isNotEmpty()
        file.sharees = userShares.map { ShareeUser(it.userId, it.sharedWithDisplayName, it.shareType) }

        return file
    }

    private fun newOCFile(path: String) = OCFile(path).also {
        it.decryptedRemotePath = path
        it.fileLength = -1
        it.modificationTimestamp = -1
    }

    private fun OCFile.applyShareData(firstShare: OCShare, firstShareTimestamp: Long) = apply {
        ownerId = firstShare.userId
        ownerDisplayName = firstShare.ownerDisplayName
        isPreviewAvailable = firstShare.isHasPreview
        mimeType = firstShare.mimetype
        note = firstShare.note
        fileId = firstShare.fileSource
        remoteId = firstShare.remoteId.toString()
        localId = firstShare.fileSource
        this.firstShareTimestamp = firstShareTimestamp
        isFavorite = firstShare.isFavorite
    }
}
