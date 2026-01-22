/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.operations

import android.content.Context
import com.nextcloud.client.account.User
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.SearchRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.operations.common.SyncOperation
import com.owncloud.android.utils.FileStorageUtils

/**
 * fetch OCFile meta data if not present in local db
 *
 * @see com.owncloud.android.ui.asynctasks.FetchRemoteFileTask reference for this operation
 *
 * @param ocFile file for which metadata has to retrieve
 * @param removeFileFromDb if you want to remove ocFile from local db to avoid duplicate entries for same fileId
 */
class FetchRemoteFileOperation(
    private val context: Context,
    private val user: User,
    private val ocFile: OCFile,
    private val removeFileFromDb: Boolean = false,
    storageManager: FileDataStorageManager
) : SyncOperation(storageManager) {

    @Deprecated("Deprecated in Java")
    @Suppress("ReturnCount")
    override fun run(client: OwnCloudClient?): RemoteOperationResult<*>? {
        val searchRemoteOperation = SearchRemoteOperation(
            ocFile.localId.toString(),
            SearchRemoteOperation.SearchType.FILE_ID_SEARCH,
            false,
            storageManager.getCapability(user)
        )
        val remoteOperationResult: RemoteOperationResult<List<RemoteFile>> =
            searchRemoteOperation.execute(user, context)

        if (remoteOperationResult.isSuccess && remoteOperationResult.resultData != null) {
            if (remoteOperationResult.resultData.isEmpty()) {
                Log_OC.e(TAG, "No remote file found with id: ${ocFile.localId}.")
                return remoteOperationResult
            }
            val remotePath = (remoteOperationResult.resultData[0]).remotePath

            val operation = ReadFileRemoteOperation(remotePath)
            val result = operation.execute(user, context)

            if (!result.isSuccess) {
                val exception = result.exception
                val message =
                    "Fetching file " + remotePath + " fails with: " + result.getLogMessage(MainApp.getAppContext())
                Log_OC.e(TAG, exception?.message ?: message)

                return result
            }

            val remoteFile = result.data[0] as RemoteFile

            // remove file from local db
            if (removeFileFromDb) {
                storageManager.removeFile(ocFile, true, true)
            }

            var ocFile = FileStorageUtils.fillOCFile(remoteFile)
            FileStorageUtils.searchForLocalFileInDefaultPath(ocFile, user.accountName)
            ocFile = storageManager.saveFileWithParent(ocFile, context)

            // also sync folder content
            val toSync: OCFile? = if (ocFile?.isFolder == true) {
                ocFile
            } else {
                ocFile?.parentId?.let { storageManager.getFileById(it) }
            }

            val currentSyncTime = System.currentTimeMillis()
            val refreshFolderOperation: RemoteOperation<Any> = RefreshFolderOperation(
                toSync,
                currentSyncTime,
                true,
                true,
                storageManager,
                user,
                context
            )
            val refreshOperationResult = refreshFolderOperation.execute(user, context)

            // set the fetched ocFile to resultData to be handled at ui end
            refreshOperationResult.resultData = ocFile

            return refreshOperationResult
        }
        return remoteOperationResult
    }

    companion object {
        private val TAG = FetchRemoteFileOperation::class.java.simpleName
    }
}
