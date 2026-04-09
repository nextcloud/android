/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.uploadList.helper

import com.nextcloud.utils.extensions.isSame
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.db.UploadResult
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
        storageManager: UploadsStorageManager
    ): ConflictHandlingResult = withContext(Dispatchers.IO) {
        val operationResult = ReadFileRemoteOperation(upload.remotePath).execute(client)

        if (!operationResult.isSuccess) {
            return@withContext when (operationResult.code) {
                RemoteOperationResult.ResultCode.FILE_NOT_FOUND -> {
                    onConflictNotExists(upload, storageManager)
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

        val ocFile = FileStorageUtils.fillOCFile(remoteFile)

        if (remoteFile.isSame(ocFile.storagePath)) {
            onConflictNotExists(upload, storageManager)
        } else {
            ConflictHandlingResult.ShowConflictResolveDialog(ocFile, upload)
        }
    }

    private fun onConflictNotExists(
        upload: OCUpload,
        storageManager: UploadsStorageManager
    ): ConflictHandlingResult.ConflictNotExists {
        upload.lastResult = UploadResult.UNKNOWN
        storageManager.updateUpload(upload)
        return ConflictHandlingResult.ConflictNotExists
    }
}
