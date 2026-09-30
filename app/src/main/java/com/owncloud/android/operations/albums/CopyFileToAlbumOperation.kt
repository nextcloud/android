/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
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

class CopyFileToAlbumOperation(private val srcPath: String, albumPath: String, storageManager: FileDataStorageManager) :
    SyncOperation(storageManager) {

    private val targetParentPath = albumPath.removeSuffix(OCFile.PATH_SEPARATOR) + OCFile.PATH_SEPARATOR

    @Deprecated("Deprecated in Java")
    @Suppress("ReturnCount")
    override fun run(client: OwnCloudClient): RemoteOperationResult<Any> {
        if (targetParentPath.startsWith(srcPath)) {
            return RemoteOperationResult(ResultCode.INVALID_COPY_INTO_DESCENDANT)
        }

        val file = storageManager.getFileByPath(srcPath)
            ?: return RemoteOperationResult(ResultCode.FILE_NOT_FOUND)

        return CopyFileToAlbumRemoteOperation(srcPath, targetPathFor(file, client)).execute(client)
    }

    /**
     * Path the file gets in the album. When it would collide with the source itself, the server picks the next free
     * name. Folders keep their trailing separator, so it is stripped before the rename and appended again after.
     */
    private fun targetPathFor(file: OCFile, client: OwnCloudClient): String {
        val basePath = targetParentPath + file.fileName
        val separator = if (file.isFolder) OCFile.PATH_SEPARATOR else ""

        if (basePath + separator != srcPath) {
            return basePath + separator
        }

        return UploadFileOperation.getNewAvailableRemotePath(client, basePath, null, false) + separator
    }
}
