/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2012 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.operations
import android.content.Context
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.RemoveFileRemoteOperation
import com.owncloud.android.operations.common.SyncOperation
import com.owncloud.android.utils.MimeTypeUtil

/**
 * Remote operation to remove a remote file or folder from an ownCloud server.
 *
 * @param file  OCFile instance representing the remote file or folder to remove.
 * @param onlyLocalCopy  If true, only the local copy will be removed (if it exists).
 * @param user  User account associated with the operation.
 * @param isInBackground  Flag indicating if the operation runs in the background.
 * @param context  Android context.
 * @param storageManager  Storage manager handling local file operations.
 */
@Suppress("LongParameterList")
class RemoveFileOperation(
    val file: OCFile,
    private val onlyLocalCopy: Boolean,
    private val user: User,
    val isInBackground: Boolean,
    private val context: Context,
    storageManager: FileDataStorageManager
) : SyncOperation(storageManager) {

    /**
     * Executes the remove operation.
     *
     * If the file is an image, it will also be removed from the thumbnail cache.
     * Handles both encrypted and non-encrypted files. Removes the file locally if needed.
     *
     * @param client  OwnCloudClient used to communicate with the remote server.
     * @return RemoteOperationResult indicating success or failure of the operation.
     */
    override fun run(client: OwnCloudClient?): RemoteOperationResult<*> {
        var result: RemoteOperationResult<*>? = null
        val operation: RemoteOperation<*>?

        var localRemovalFailed = false

        if (onlyLocalCopy) {
            // generate resize image if image is deleted only locally, to save server request
            if (MimeTypeUtil.isImage(file.mimeType)) {
                ThumbnailsCacheManager.generateResizedImage(file)
            }

            localRemovalFailed = !storageManager.removeFile(file, false, true)
            if (!localRemovalFailed) {
                result = RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.OK)
            }
        } else {
            operation = if (file.isEncrypted) {
                val parent = storageManager.getFileById(file.parentId)
                if (parent == null) {
                    return RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.LOCAL_FILE_NOT_FOUND)
                }
                RemoveRemoteEncryptedFileOperation(
                    file.remotePath,
                    user,
                    context,
                    file.getEncryptedFileName(),
                    parent,
                    file.isFolder
                )
            } else {
                RemoveFileRemoteOperation(file.remotePath)
            }

            result = operation.execute(client)
            if (result.isSuccess || result.code == RemoteOperationResult.ResultCode.FILE_NOT_FOUND) {
                localRemovalFailed = !storageManager.removeFile(file, true, true)
            }
        }

        if (localRemovalFailed) {
            result = RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.LOCAL_STORAGE_NOT_REMOVED)
        }

        return result ?: RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.CANCELLED)
    }
}
