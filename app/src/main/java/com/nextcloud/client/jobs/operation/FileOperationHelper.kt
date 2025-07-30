/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.operation

import android.content.Context
import com.nextcloud.client.account.User
import com.nextcloud.utils.extensions.getErrorMessage
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.db.OCUpload
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.operations.RemoveFileOperation
import com.owncloud.android.utils.FileUtil
import com.owncloud.android.utils.MimeTypeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.io.File

class FileOperationHelper(private val user: User, private val context: Context) {
    companion object {
        private val TAG = FileOperationHelper::class.java.simpleName
    }

    /**
     * Checks if a file with the same remote path (case-insensitive) and unchanged content
     * already exists in local storage by considering both lowercase and uppercase variants
     * of the file extension.
     *
     * ### Example:
     * ```
     * On the server, 0001.WEBP exists and the user tries to upload the same file
     * with the lowercased version 0001.webp — in that case, this will return true.
     * ```
     */
    fun isSameRemoteFileAlreadyPresent(upload: OCUpload, storageManager: FileDataStorageManager): Boolean {
        val (lc, uc) = FileUtil.getRemotePathVariants(upload.remotePath)

        val remoteFile = storageManager.getFileByDecryptedRemotePath(lc)
            ?: storageManager.getFileByDecryptedRemotePath(uc)

        if (remoteFile != null && remoteFile.remotePath.equals(upload.remotePath, ignoreCase = true)) {
            if (isSameFileOnRemote(remoteFile, upload)) {
                Log_OC.w(TAG, "Same file already exists due to lowercase/uppercase extension")
                return true
            }
        }

        return false
    }

    fun isSameFileOnRemote(remoteFile: OCFile, upload: OCUpload): Boolean {
        val localFile = File(upload.localPath)
        if (!localFile.exists()) {
            return false
        }
        val localSize: Long = localFile.length()
        return remoteFile.fileLength == localSize
    }

    @Suppress("DEPRECATION")
    fun getRemoteFile(remotePath: String, client: OwnCloudClient): RemoteFile? {
        val mimeType = MimeTypeUtil.getMimeTypeFromPath(remotePath)
        val isFolder = MimeTypeUtil.isFolder(mimeType)
        val result = if (isFolder) {
            ReadFolderRemoteOperation(remotePath).execute(client)
        } else {
            ReadFileRemoteOperation(remotePath).execute(client)
        }

        return if (result.isSuccess) {
            result.data[0] as? RemoteFile
        } else {
            null
        }
    }

    fun isFileChanged(remoteFile: RemoteFile?, ocFile: OCFile?): Boolean =
        (remoteFile != null && ocFile != null && remoteFile.etag != ocFile.etagOnServer)

    @Suppress("TooGenericExceptionCaught", "Deprecation")
    suspend fun removeFile(
        file: OCFile,
        storageManager: FileDataStorageManager,
        onlyLocalCopy: Boolean,
        inBackground: Boolean,
        client: OwnCloudClient
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val operation = async {
                    RemoveFileOperation(
                        file,
                        onlyLocalCopy,
                        user,
                        inBackground,
                        context,
                        storageManager
                    )
                }
                val operationResult = operation.await()
                val result = operationResult.execute(client)

                return@withContext if (result.isSuccess) {
                    true
                } else {
                    val reason = (result to operationResult).getErrorMessage()
                    Log_OC.e(TAG, "Error occurred while removing file: $reason")
                    false
                }
            } catch (e: Exception) {
                Log_OC.e(TAG, "Error occurred while removing file: $e")
                false
            }
        }
    }
}
