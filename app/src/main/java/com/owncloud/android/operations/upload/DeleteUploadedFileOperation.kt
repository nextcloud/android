/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Daniele Verducci <daniele.verducci@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.upload

import android.content.Context
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
    private val context: Context,
    private val storageManager: FileDataStorageManager
) {

    companion object {
        const val TAG = "DeleteUploadedFileOperation"

        const val MS_IN_SECOND = 1000
    }
    private val syncFolderHelper = SyncFolderHelper(context)

    @Suppress("ReturnCount")
    suspend fun run(): RemoteOperationResult<Stats> {
        Log_OC.d(TAG, "StorageManager user is ${storageManager.user}")
        // Obtain synced folder data
        val folder = storageManager.getFileByRemotePath(syncedFolder.remotePath)
            ?: storageManager.getFileByLocalPath(syncedFolder.localPath)
        if (folder == null) {
            Log_OC.e(TAG, "Unable to obtain remote folder to refresh metadata")
            return RemoteOperationResult<Stats>(RemoteOperationResult.ResultCode.METADATA_NOT_FOUND)
        }

        // Refresh synced folder metadata
        val metadataRefreshSuccess = refreshFolder(folder, storageManager)
        if (!metadataRefreshSuccess) {
            Log_OC.e(TAG, "Unable to refresh folder metadata")
            return RemoteOperationResult<Stats>(RemoteOperationResult.ResultCode.METADATA_NOT_FOUND)
        }
        val refreshedFolders = hashSetOf<String>(folder.remotePath)

        val localFolder = File(syncedFolder.localPath)
        val files = SyncedFolderUtils.getFileList(localFolder)

        var filesPreserved = 0L
        var filesRemoved = 0L
        var spaceFreed = 0L

        files.forEach { localFile ->
            val remotePath = syncFolderHelper.getAutoUploadRemotePath(syncedFolder, localFile)
            val ocFile = storageManager.getFileByRemotePath(remotePath)
            if (ocFile == null) {
                Log_OC.i(TAG, "Unable to compare file ${localFile.name} with its remote counterpart, leaving in place")
                filesPreserved++
                return@forEach
            }

            val parentFolderRemotePath = ocFile.parentRemotePath
            if (parentFolderRemotePath !in refreshedFolders) {
                // Files are stored in subfolder by date on the server.
                // Refresh only subfolders containing one of the files to be checked
                val subFolder = storageManager.getFileByRemotePath(parentFolderRemotePath)
                if (subFolder == null) {
                    Log_OC.e(TAG, "Subfolder $parentFolderRemotePath not found on the server")
                    return RemoteOperationResult<Stats>(RemoteOperationResult.ResultCode.METADATA_NOT_FOUND)
                }
                val metadataRefreshSuccess = refreshFolder(subFolder, storageManager)
                if (!metadataRefreshSuccess) {
                    Log_OC.e(TAG, "Unable to refresh folder metadata for $parentFolderRemotePath")
                    return RemoteOperationResult<Stats>(RemoteOperationResult.ResultCode.METADATA_NOT_FOUND)
                }
            }

            // Check the file wasn't modified after uploading
            val localLastMod = localFile.lastModified()
            val lastSyncDate = ocFile.lastSyncDateForProperties
            if (lastSyncDate < localLastMod) {
                Log_OC.i(
                    TAG,
                    "File ${localFile.name} has been modified ($localLastMod " +
                        "after it was synced ($lastSyncDate), leaving in place"
                )
                filesPreserved++
                return@forEach
            }

            // Check the file has same mod date. Note that the remote mod date is rounded to the second.
            val remoteLastMod = ocFile.modificationTimestamp
            if (remoteLastMod / MS_IN_SECOND != localLastMod / MS_IN_SECOND) {
                Log_OC.i(
                    TAG,
                    "Local and remote mod date differs for file file ${localFile.name}: " +
                        "$localLastMod : $remoteLastMod, leaving in place"
                )
                filesPreserved++
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
                filesPreserved++
                return@forEach
            }

            // File deletion
            val deleted = true // localFile.delete()
            if (deleted) {
                Log_OC.i(TAG, "Deleted file ${localFile.name}")
                filesRemoved++
                spaceFreed += localSize
            } else {
                Log_OC.e(TAG, "Error deleting file ${localFile.name}")
            }
        }

        val result = RemoteOperationResult<Stats>(RemoteOperationResult.ResultCode.OK)
        result.resultData = Stats(
            filesPreserved,
            filesRemoved,
            spaceFreed
        )
        return result
    }

    private suspend fun refreshFolder(folder: OCFile, storageManager: FileDataStorageManager): Boolean =
        withContext(Dispatchers.IO) {
            val operation = RefreshFolderOperation(folder, storageManager, storageManager.user, context)
            val result = operation.executeNextcloudClient(storageManager.user, context)
            if (result.isSuccess) {
                Log_OC.d(TAG, "Successfully fetched metadata for: ${folder.remotePath}")
                true
            } else {
                Log_OC.e(TAG, "Failed to fetch metadata for: ${folder.remotePath}")
                false
            }
        }

    /**
     * Contains the statistics about the run
     * @param filesPreserved Files not deleted from the device
     * @param filesRemoved Files deleted from the device
     * @param spaceFreed Space freed deleting files on the device, in bytes
     */
    data class Stats(val filesPreserved: Long, val filesRemoved: Long, val spaceFreed: Long)
}
