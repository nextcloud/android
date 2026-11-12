/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.uploadList.helper

import com.nextcloud.utils.extensions.isSame
import com.nextcloud.utils.extensions.updateStatus
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.db.OCUpload
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.utils.FileStorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UploadListAdapterActionHandler : UploadListAdapterAction {
    companion object {
        private const val TAG = "UploadListAdapterActionHandler"
    }

    override suspend fun handleConflict(
        upload: OCUpload,
        client: OwnCloudClient,
        storageManager: UploadsStorageManager,
        existingFile: OCFile?
    ): ConflictHandlingResult = withContext(Dispatchers.IO) {
        val operationResult = ReadFileRemoteOperation(existingFile?.remotePath ?: upload.remotePath).execute(client)

        if (!operationResult.isSuccess) {
            return@withContext when (operationResult.code) {
                RemoteOperationResult.ResultCode.FILE_NOT_FOUND -> {
                    updateStatus(upload, storageManager, success = false)
                    ConflictHandlingResult.ConflictNotExistsRemoteFileNotFound
                }

                else -> {
                    Log_OC.e(TAG, "cannot check conflict, operation result is not success")
                    ConflictHandlingResult.CannotCheckConflict
                }
            }
        }

        val remoteFile = operationResult.data[0] as? RemoteFile
            ?: run {
                Log_OC.e(TAG, "cannot check conflict, operation result cannot be cast to RemoteFile")
                return@withContext ConflictHandlingResult.CannotCheckConflict
            }

        val ocFile = existingFile ?: FileStorageUtils.fillOCFile(remoteFile)

        if (!ocFile.isEncrypted && remoteFile.isSame(upload.localPath)) {
            updateStatus(upload, storageManager, success = true)
            ConflictHandlingResult.ConflictNotExistsSameFile
        } else {
            ConflictHandlingResult.ShowConflictResolveDialog(ocFile, upload)
        }
    }

    private fun updateStatus(upload: OCUpload, storageManager: UploadsStorageManager, success: Boolean) {
        val entity = storageManager.uploadDao.getUploadById(upload.uploadId, upload.accountName)
        storageManager.updateStatus(entity, success)
    }
}
