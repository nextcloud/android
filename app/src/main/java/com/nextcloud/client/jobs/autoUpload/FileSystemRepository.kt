/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.autoUpload

import com.nextcloud.client.database.dao.FileSystemDao
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.SyncedFolderUtils
import java.io.File

class FilesystemRepository(private val dao: FileSystemDao) {

    suspend fun getAutoUploadFiles(syncedFolder: SyncedFolder): Set<String> {
        val syncedFolderId = syncedFolder.id.toString()
        Log_OC.d(TAG, "Fetching candidate files for syncedFolderId = $syncedFolderId")

        val candidatePaths = dao.getAutoUploadFiles(syncedFolderId)
        val localPathsToUpload = mutableSetOf<String>()

        candidatePaths.forEach { path ->
            val file = File(path)
            if (!file.exists()) {
                Log_OC.w(TAG, "Ignoring file for upload (doesn't exist): $path")
            } else if (!SyncedFolderUtils.isQualifiedFolder(file.parent)) {
                Log_OC.w(TAG, "Ignoring file for upload (unqualified folder): $path")
            } else if (!SyncedFolderUtils.isFileNameQualifiedForAutoUpload(file.name)) {
                Log_OC.w(TAG, "Ignoring file for upload (unqualified file): $path")
            } else {
                Log_OC.d(TAG, "Adding path to upload: $path")
                localPathsToUpload.add(path)
            }
        }

        return localPathsToUpload
    }

    suspend fun markFileAsUploaded(localPath: String, syncedFolder: SyncedFolder) {
        val syncedFolderIdStr = syncedFolder.id.toString()

        try {
            dao.markFileAsUploaded(localPath, syncedFolderIdStr)
            Log_OC.d(TAG, "Marked file as uploaded: $localPath for syncedFolderId=$syncedFolderIdStr")
        } catch (e: Exception) {
            Log_OC.e(TAG, "Error marking file as uploaded: ${e.message}", e)
        }
    }

    companion object {
        private const val TAG = "FilesystemRepository"
    }
}
