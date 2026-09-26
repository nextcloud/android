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

    @Suppress("NestedBlockDepth", "ReturnCount")
    suspend fun run(): RemoteOperationResult<Stats> {
        Log_OC.d(TAG, "Analyzing folder ${syncedFolder.remotePath} from user ${syncedFolder.account}")

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

        val localFolder = File(syncedFolder.localPath)
        val files = SyncedFolderUtils.getFileList(localFolder)

        var filesPreserved = 0L
        var filesRemoved = 0L
        var spaceFreed = 0L

        files.forEach { localFile ->
            Log_OC.d(TAG, "Analyzing file $localFile from folder ${folder.remotePath}")
            try {
                val fileCanBeDeleted = fileCanBeDeleted(localFile, folder)

                if (fileCanBeDeleted) {
                    val fileSize = localFile.length()
                    // File deletion
                    val deleted = localFile.delete()
                    if (deleted) {
                        Log_OC.i(TAG, "Deleted file ${localFile.name}")
                        filesRemoved++
                        spaceFreed += fileSize
                    } else {
                        Log_OC.e(TAG, "Error deleting file ${localFile.name}")
                    }
                } else {
                    filesPreserved++
                }
            } catch (e: NoSuchElementException) {
                Log_OC.e(TAG, e.toString())
                return RemoteOperationResult<Stats>(RemoteOperationResult.ResultCode.METADATA_NOT_FOUND)
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

    @Suppress("ReturnCount")
    private suspend fun fileCanBeDeleted(localFile: File, folder: OCFile): Boolean {
        val remotePath = syncFolderHelper.getAutoUploadRemotePath(syncedFolder, localFile)
        val ocFile =
            storageManager.getFileByRemotePath(remotePath)
                // If file is null, search in the parent folder, in case it was uploaded before enabling subfolderByDate
                ?: storageManager.getFileByRemotePath("${folder.remotePath}${localFile.name}")
        if (ocFile == null) {
            Log_OC.i(TAG, "Unable to compare file ${localFile.name} with its remote counterpart, leaving in place")
            return false
        }

        val refreshedFolders = hashSetOf<String>(folder.remotePath)
        val parentFolderRemotePath = ocFile.parentRemotePath
        if (parentFolderRemotePath !in refreshedFolders) {
            // Files are stored in subfolder by date on the server. Refresh only subfolders containing files to check
            val subFolder = storageManager.getFileByRemotePath(parentFolderRemotePath)
                ?: throw NoSuchElementException("Subfolder $parentFolderRemotePath not found on the server")
            val metadataRefreshSuccess = refreshFolder(subFolder, storageManager)
            if (!metadataRefreshSuccess) {
                throw NoSuchElementException("Unable to refresh folder metadata for $parentFolderRemotePath")
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
            return false
        }

        // Check the file has same mod date. Note that the remote mod date is rounded to the second.
        val remoteLastMod = ocFile.modificationTimestamp
        if (remoteLastMod / MS_IN_SECOND != localLastMod / MS_IN_SECOND) {
            Log_OC.i(
                TAG,
                "Local and remote mod date differs for file ${localFile.name}: " +
                    "$localLastMod : $remoteLastMod, leaving in place"
            )
            return false
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
            return false
        }
        return true
    }

    private suspend fun refreshFolder(folder: OCFile, storageManager: FileDataStorageManager): Boolean =
        withContext(Dispatchers.IO) {
            val operation = RefreshFolderOperation(folder, storageManager, storageManager.user, context)
            val result = operation.execute(storageManager.user, context)
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
