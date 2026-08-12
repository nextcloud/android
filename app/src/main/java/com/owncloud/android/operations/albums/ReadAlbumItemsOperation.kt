/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.albums

import com.nextcloud.common.SessionTimeOut
import com.nextcloud.common.defaultSessionTimeOut
import com.nextcloud.utils.extensions.toAlbumItem
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.albums.ReadAlbumItemsRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile

class ReadAlbumItemsOperation
@JvmOverloads
constructor(
    private val remotePath: String,
    private val storageManager: FileDataStorageManager?,
    private val sessionTimeOut: SessionTimeOut = defaultSessionTimeOut
) : RemoteOperation<List<RemoteFile>>() {

    @Deprecated("Deprecated in Java")
    @Suppress("TooGenericExceptionCaught")
    override fun run(client: OwnCloudClient): RemoteOperationResult<List<RemoteFile>> = try {
        val result = ReadAlbumItemsRemoteOperation(remotePath, sessionTimeOut).execute(client)

        when {
            result.isSuccess -> result.apply { resultData = cacheAlbumItems(resultData) }
            else -> result
        }
    } catch (e: Exception) {
        RemoteOperationResult(e)
    }

    /**
     * Thumbnails and previews resolve album items through the database, so items it does not know yet are stored.
     * Items without a file id cannot be resolved later on and are dropped.
     */
    private fun cacheAlbumItems(remoteFiles: List<RemoteFile>): List<RemoteFile> {
        val albumItems = remoteFiles.filter { it.localId > 0 }
        val storageManager = storageManager ?: return albumItems

        albumItems
            .filter { storageManager.getFileByLocalId(it.localId) == null }
            .forEach { remoteFile ->
                val albumItem = remoteFile.toAlbumItem().apply {
                    lastSyncDateForProperties = System.currentTimeMillis()
                }
                storageManager.saveFile(albumItem)
            }

        return albumItems
    }
}
