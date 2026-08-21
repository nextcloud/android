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
        // Obtain synced folder data
        val folder = storageManager.getFileByRemotePath(syncedFolder.remotePath)
        if (folder == null) {
            Log_OC.e(TAG, "Unable to obtain remote folder to refresh metadata")
            return RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.METADATA_NOT_FOUND)
        }

        // Refresh synced folder metadata
        val metadataRefreshSuccess = refreshFolder(folder, storageManager)
        if (!metadataRefreshSuccess) {
            Log_OC.e(TAG, "Unable to refresh folder metadata")
            return RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.METADATA_NOT_FOUND)
        }
        val refreshedFolders = hashSetOf<String>(folder.remotePath)

        val localFolder = File(syncedFolder.localPath)
        val files = SyncedFolderUtils.getFileList(localFolder)
        files.forEach { localFile ->
            val remotePath = syncFolderHelper.getAutoUploadRemotePath(syncedFolder, localFile)
            val ocFile = storageManager.getFileByRemotePath(remotePath)
            if (ocFile == null) {
                Log_OC.i(TAG, "Unable to compare file ${localFile.name} with its remote counterpart, leaving in place")
                return@forEach
            }

            val parentFolderRemotePath = ocFile.parentRemotePath
            if (syncedFolder.isSubfolderByDate && parentFolderRemotePath !in refreshedFolders) {
                // Files are stored in subfolder by date on the server.
                // Refresh only subfolders containing one of the files to be checked
                val subFolder = storageManager.getFileByRemotePath(parentFolderRemotePath)
                if (subFolder == null) {
                    Log_OC.e(TAG, "Subfolder $parentFolderRemotePath not found on the server")
                    return RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.METADATA_NOT_FOUND)
                }
                val metadataRefreshSuccess = refreshFolder(subFolder, storageManager)
                if (!metadataRefreshSuccess) {
                    Log_OC.e(TAG, "Unable to refresh folder metadata for $parentFolderRemotePath")
                    return RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.METADATA_NOT_FOUND)
                }
            }

            // Check the file wasn't modified after uploading
            // TODO: Is this redundant?
            val localLastMod = localFile.lastModified()
            val lastSyncDate = ocFile.lastSyncDateForProperties
            if (lastSyncDate < localLastMod) {
                Log_OC.i(
                    TAG,
                    "File ${localFile.name} has been modified ($localLastMod " +
                        "after it was synced ($lastSyncDate), leaving in place"
                )
                return@forEach
            }

            // Check the file has same mod date. Note that the remote mod date is rounded to the second.
            val remoteLastMod = ocFile.modificationTimestamp
            if (remoteLastMod / 1000 != localLastMod / 1000) {
                Log_OC.i(
                    TAG,
                    "Local and remote mod date differs for file file ${localFile.name}: " +
                        "$localLastMod : $remoteLastMod, leaving in place"
                )
                return@forEach
            }

            // Check the file has same size
            val localSize = localFile.length()
            val remoteSize = ocFile.fileLength
            if (localSize != remoteSize) {
                Log_OC.d(
                    TAG,
                    "Local and remote file sizes differs for file ${localFile.name}: " +
                        "$localSize : $remoteSize, leaving in place"
                )
                return@forEach
            }

            // File deletion
            val deleted = true //localFile.delete()
            if (deleted) {
                Log_OC.i(TAG, "Deleted file ${localFile.name}")
            } else {
                Log_OC.e(TAG, "Error deleting file ${localFile.name}")
            }
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
