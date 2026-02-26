/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.albums

import com.nextcloud.common.SessionTimeOut
import com.nextcloud.common.defaultSessionTimeOut
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.albums.ReadAlbumItemsRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.utils.FileStorageUtils

class ReadAlbumItemsOperation
@JvmOverloads
constructor(
    private val mRemotePath: String,
    private val storageManager: FileDataStorageManager?,
    private val sessionTimeOut: SessionTimeOut = defaultSessionTimeOut
) : RemoteOperation<List<RemoteFile>>() {
    @Deprecated("Deprecated in Java")
    @Suppress("TooGenericExceptionCaught")
    override fun run(client: OwnCloudClient): RemoteOperationResult<List<RemoteFile>> {
        var result: RemoteOperationResult<List<RemoteFile>>? = null
        try {
            result = ReadAlbumItemsRemoteOperation(mRemotePath, sessionTimeOut).execute(client)
            if (result.isSuccess) {
                // get data from remote folder
                val mFolderAndFiles = saveAlbumData(result.resultData, storageManager)

                // Result of the operation
                result.apply {
                    // Add data to the result
                    resultData = mFolderAndFiles
                }
            }
        } catch (e: Exception) {
            result = RemoteOperationResult(e)
        }

        return result
    }

    companion object {
        private val TAG: String = ReadAlbumItemsRemoteOperation::class.java.simpleName

        private fun saveAlbumData(
            remoteFiles: List<RemoteFile>,
            storageManager: FileDataStorageManager?
        ): List<RemoteFile> {
            val files = mutableListOf<RemoteFile>()

            for (remoteFile in remoteFiles) {
                // if no fileId received then skip adding
                if (remoteFile.localId <= 0) {
                    continue
                }
                // check if file already exit in db or not
                // if not then store it in db to show thumbnail and image preview
                var ocFile = storageManager?.getFileByLocalId(remoteFile.localId)
                if (ocFile == null) {
                    ocFile = FileStorageUtils.fillOCFile(remoteFile)
                    // no remote id is received from response
                    // so localId will be remoteId else thumb will not generated
                    ocFile.remoteId = remoteFile.localId.toString()
                    ocFile.lastSyncDateForProperties = System.currentTimeMillis()
                    storageManager?.saveFile(ocFile)
                }
                files.add(remoteFile)
            }

            return files
        }
    }
}
