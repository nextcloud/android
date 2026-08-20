/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.upload

import android.content.Context
import com.nextcloud.client.account.User
import com.nextcloud.client.jobs.autoUpload.SyncFolderHelper
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.utils.SyncedFolderUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DeleteUploadedFileOperation(
    private val syncedFolder: SyncedFolder,
    private val user: User,
    private val context: Context,
    private val storageManager: FileDataStorageManager
) {
    
    companion object {
        const val TAG = "DeleteUploadedFileOperation"
    }
    private val syncFolderHelper = SyncFolderHelper(context)

    suspend fun run(): RemoteOperationResult<*> {
        // TODO: Refresh remote data with RefreshFolderOperation (and do recursively for inner folders)

        val folder = storageManager.getFileByRemotePath(syncedFolder.remotePath)
        if (folder == null) {
            Log_OC.e(TAG, "Unable to obtain remote folder to refresh metadata")
            return RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.METADATA_NOT_FOUND)
        }
        val metadataRefreshSuccess = refreshFolder(folder, storageManager)
        if (!metadataRefreshSuccess) {
            Log_OC.e(TAG, "Unable to refresh folder metadata")
            return RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.METADATA_NOT_FOUND)
        }



        val localFolder = File(syncedFolder.localPath)
        val files = SyncedFolderUtils.getFileList(localFolder)
        files.forEach { localFile ->
            /*
            val entity = fileSystemDao.getFileByPathAndFolder(syncedFolder.localPath, syncedFolder.id.toString())
            val fileSentForUpload = entity?.fileSentForUpload == 1

            if (!fileSentForUpload) {
                Log_OC.i(TAG, "File ${localFile.name} never uploaded, leaving in place")
                return@forEach
            }

            val hasNotChanged = entity.fileModified == localFile.lastModified()
            if (!hasNotChanged) {
                Log_OC.i(TAG, "File ${localFile.name} has changed, leaving in place")
                return@forEach
            }

            */

            //val ocFile = storageManager.getFileByLocalPath(localFile.path)
            val remotePath = syncFolderHelper.getAutoUploadRemotePath(syncedFolder, localFile)
            val ocFile = storageManager.getFileByRemotePath(remotePath)
            if (ocFile == null) {
                Log_OC.i(TAG, "Unable to compare file ${localFile.name} with its remote counterpart, leaving in place")
                return@forEach
            }
            Log_OC.d(TAG, "LastSyncDate: ${ocFile?.lastSyncDateForProperties}, modificationTimestamp: ${ocFile.modificationTimestamp}")

            ocFile.lastSyncDateForProperties < localFile.lastModified()
            ocFile.modificationTimestamp == localFile.lastModified()

            // File deletion
            /*
            val success = filesOperationHelper.removeFile(
                file = ocFile,
                onlyLocalCopy = true,
                inBackground = true,
                client = client
            )
            if (success) {
                Log_OC.i(TAG, "deleteUploadedItemFromSyncFolder: Removed local file ${it.absolutePath}")
            } else {
                Log_OC.e(TAG, "deleteUploadedItemFromSyncFolder: Error removing local file ${it.absolutePath}")
            }
            */
            Log_OC.d(TAG, "DELETING FILE ${localFile.name}")
        }

        return RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.OK)
    }


    private suspend fun refreshFolder(folder: OCFile, storageManager: FileDataStorageManager): Boolean =
        withContext(Dispatchers.IO) {
            val operation = RefreshFolderOperation(folder, storageManager, user, context)
            return@withContext try {
                val result = operation.execute(user, context)
                if (result.isSuccess) {
                    Log_OC.d(TAG, "Successfully fetched metadata for: ${folder.remotePath}")
                    true
                } else {
                    Log_OC.e(TAG, "Failed to fetch metadata for: ${folder.remotePath}")
                    false
                }
            } catch (e: Exception) {
                Log_OC.e(TAG, "Exception refreshing folder ${folder.remotePath}: ${e.message}", e)
                false
            }
        }
}