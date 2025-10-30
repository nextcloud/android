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
import kotlinx.coroutines.runBlocking
import java.io.File

class FileSystemRepository(private val dao: FileSystemDao) {

    companion object {
        private const val TAG = "FilesystemRepository"
        const val BATCH_SIZE = 50
    }

    @Suppress("NestedBlockDepth")
    suspend fun getFilePathsWithIds(syncedFolder: SyncedFolder, lastId: Int): List<Pair<String, Int>> {
        val syncedFolderId = syncedFolder.id.toString()
        Log_OC.d(TAG, "Fetching candidate files for syncedFolderId = $syncedFolderId")

        val entities = dao.getAutoUploadFilesEntities(syncedFolderId, BATCH_SIZE, lastId)
        val filtered = mutableListOf<Pair<String, Int>>()

        entities.forEach {
            it.localPath?.let { path ->
                val file = File(path)
                if (!file.exists()) {
                    Log_OC.w(TAG, "Ignoring file for upload (doesn't exist): $path")
                } else if (!SyncedFolderUtils.isQualifiedFolder(file.parent)) {
                    Log_OC.w(TAG, "Ignoring file for upload (unqualified folder): $path")
                } else if (!SyncedFolderUtils.isFileNameQualifiedForAutoUpload(file.name)) {
                    Log_OC.w(TAG, "Ignoring file for upload (unqualified file): $path")
                } else {
                    Log_OC.d(TAG, "Adding path to upload: $path")

                    if (it.id != null) {
                        filtered.add(path to it.id)
                    } else {
                        Log_OC.w(TAG, "cant adding path to upload, id is null")
                    }
                }
            }
        }

        return filtered
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun markFileAsUploaded(localPath: String, syncedFolder: SyncedFolder) {
        val syncedFolderIdStr = syncedFolder.id.toString()

        try {
            dao.markFileAsUploaded(localPath, syncedFolderIdStr)
            Log_OC.d(TAG, "Marked file as uploaded: $localPath for syncedFolderId=$syncedFolderIdStr")
        } catch (e: Exception) {
            Log_OC.e(TAG, "Error marking file as uploaded: ${e.message}", e)
        }
    }

    fun markFileAsUploadedBlocking(localPath: String, syncedFolder: SyncedFolder) {
        runBlocking {
            markFileAsUploaded(localPath, syncedFolder)
        }
    }
}
