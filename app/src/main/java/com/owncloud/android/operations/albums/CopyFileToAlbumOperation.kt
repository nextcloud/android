/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.albums

import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.resources.albums.CopyFileToAlbumRemoteOperation
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.operations.common.SyncOperation

/**
 * Constructor
 *
 * @param srcPath          Remote path of the [OCFile] to move.
 * @param targetParentPath Path to the folder where the file will be copied into.
 */
class CopyFileToAlbumOperation(
    private val srcPath: String,
    private var targetParentPath: String,
    storageManager: FileDataStorageManager
) :
    SyncOperation(storageManager) {
    init {
        if (!targetParentPath.endsWith(OCFile.PATH_SEPARATOR)) {
            this.targetParentPath += OCFile.PATH_SEPARATOR
        }
    }

    /**
     * Performs the operation.
     *
     * @param client Client object to communicate with the remote ownCloud server.
     */
    @Deprecated("Deprecated in Java")
    @Suppress("NestedBlockDepth")
    override fun run(client: OwnCloudClient): RemoteOperationResult<Any> {
        /** 1. check copy validity */
        val result: RemoteOperationResult<Any>

        if (targetParentPath.startsWith(srcPath)) {
            result = RemoteOperationResult<Any>(ResultCode.INVALID_COPY_INTO_DESCENDANT)
        } else {
            val file = storageManager.getFileByPath(srcPath)
            if (file == null) {
                result = RemoteOperationResult(ResultCode.FILE_NOT_FOUND)
            } else {
                /** 2. remote copy */
                var targetPath = "$targetParentPath${file.fileName}"
                if (file.isFolder) {
                    targetPath += OCFile.PATH_SEPARATOR
                }

                // auto rename, to allow copy
                if (targetPath == srcPath) {
                    if (file.isFolder) {
                        targetPath = "$targetParentPath${file.fileName}"
                    }
                    targetPath = UploadFileOperation.getNewAvailableRemotePath(client, targetPath, null, false)

                    if (file.isFolder) {
                        targetPath += OCFile.PATH_SEPARATOR
                    }
                }

                result = CopyFileToAlbumRemoteOperation(srcPath, targetPath).execute(client)

                /** 3. local copy */
                if (result.isSuccess) {
                    storageManager.copyLocalFile(file, targetPath)
                }
            }
        }
        return result
    }
}
