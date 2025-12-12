/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.autoUpload

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.nextcloud.client.database.dao.FileSystemDao
import com.nextcloud.client.database.entity.FilesystemEntity
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.SyncedFolderUtils
import java.io.File
import java.util.zip.CRC32

class FileSystemRepository(private val dao: FileSystemDao, private val context: Context) {

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

    fun insert(uri: Uri, syncedFolder: SyncedFolder) {
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        val cursor = context.contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            val columnIndexData = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)

            while (cursor.moveToNext()) {
                val filePath = cursor.getString(columnIndexData)
                Log_OC.d(TAG, "attempt to insert new file entity uri: $uri")
                insertOrReplace(filePath, null, syncedFolder)
            }
        } ?: Log_OC.w(TAG, "Cursor is null for URI: $uri")
    }

    fun insertOrReplace(localPath: String?, lastModified: Long?, syncedFolder: SyncedFolder) {
        try {
            if (localPath == null) {
                Log_OC.w(TAG, "localPath path not exists: $localPath")
                return
            }

            val file = File(localPath)
            if (!file.exists()) {
                Log_OC.w(TAG, "local file does not exist, cannot insert or replace: $localPath")
                return
            }

            val entity = dao.getFileByPathAndFolder(localPath, syncedFolder.id.toString())
            if (entity != null && entity.fileSentForUpload == 1) {
                Log_OC.w(TAG, "file already uploaded path: $localPath, syncedFolder: ${syncedFolder.localPath}, ${syncedFolder.id}")
                return
            }

            val crc = getFileChecksum(file)

            val newEntity = FilesystemEntity(
                id = entity?.id,
                localPath = localPath,
                fileIsFolder = if (file.isDirectory) 1 else 0,
                fileFoundRecently = System.currentTimeMillis(),
                fileSentForUpload = 0,
                syncedFolderId = syncedFolder.id.toString(),
                crc32 = crc?.toString(),
                fileModified = lastModified ?: file.lastModified()
            )

            Log_OC.d(TAG, "inserting new file system entity: $newEntity")

            dao.insertOrReplace(newEntity)
        } catch (e: Exception) {
            Log_OC.e(TAG, "Failed to insert/update file: $localPath", e)
        }
    }

    private fun getFileChecksum(file: File): Long? {
        return try {
            file.inputStream().use { fis ->
                val crc = CRC32()
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } > 0) {
                    crc.update(buffer, 0, bytesRead)
                }
                crc.value
            }
        } catch (_: Exception) {
            null
        }
    }
}
