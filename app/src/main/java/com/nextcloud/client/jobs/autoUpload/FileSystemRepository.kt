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
import com.nextcloud.utils.extensions.shouldSkipFile
import com.nextcloud.utils.extensions.toFile
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.SyncedFolderUtils
import java.io.File
import java.util.zip.CRC32

@Suppress("TooGenericExceptionCaught", "NestedBlockDepth", "MagicNumber", "ReturnCount")
class FileSystemRepository(
    private val dao: FileSystemDao,
    private val uploadsStorageManager: UploadsStorageManager,
    private val context: Context
) {
    private val syncFolderHelper = SyncFolderHelper(context)

    companion object {
        private const val TAG = "FilesystemRepository"
        const val BATCH_SIZE = 50
    }

    fun deleteAutoUploadAndUploadEntity(syncedFolder: SyncedFolder, localPath: String, entity: FilesystemEntity) {
        Log_OC.d(TAG, "deleting auto upload entity and upload entity")

        val file = File(localPath)
        val remotePath = syncFolderHelper.getAutoUploadRemotePath(syncedFolder, file)
        uploadsStorageManager.uploadDao.deleteByRemotePathAndAccountName(
            remotePath = remotePath,
            accountName = syncedFolder.account
        )
        dao.delete(entity)
    }

    suspend fun hasPendingFiles(syncedFolder: SyncedFolder): Boolean = dao.hasPendingFiles(syncedFolder.id.toString())

    suspend fun deleteByLocalPathAndId(path: String, id: Int) {
        dao.deleteByLocalPathAndId(path, id)
    }

    suspend fun getFilePathsWithIds(syncedFolder: SyncedFolder, lastId: Int): List<Pair<String, Int>> {
        val syncedFolderId = syncedFolder.id.toString()
        Log_OC.d(TAG, "Fetching candidate files for syncedFolderId = $syncedFolderId")

        val entities = dao.getAutoUploadFilesEntities(syncedFolderId, BATCH_SIZE, lastId)
        val filtered = mutableListOf<Pair<String, Int>>()

        entities.forEach { entity ->
            entity.localPath?.let { path ->
                val file = File(path)
                if (!file.exists()) {
                    Log_OC.w(TAG, "Ignoring file for upload (doesn't exist): $path")
                    deleteAutoUploadAndUploadEntity(syncedFolder, path, entity)
                } else if (!SyncedFolderUtils.isQualifiedFolder(file.parent)) {
                    Log_OC.w(TAG, "Ignoring file for upload (unqualified folder): $path")
                    deleteAutoUploadAndUploadEntity(syncedFolder, path, entity)
                } else if (!SyncedFolderUtils.isFileNameQualifiedForAutoUpload(file.name)) {
                    Log_OC.w(TAG, "Ignoring file for upload (unqualified file): $path")
                    deleteAutoUploadAndUploadEntity(syncedFolder, path, entity)
                } else {
                    Log_OC.d(TAG, "Adding path to upload: $path")

                    if (entity.id != null) {
                        filtered.add(path to entity.id)
                    } else {
                        Log_OC.w(TAG, "cant adding path to upload, id is null")
                    }
                }
            }
        }

        return filtered
    }

    suspend fun markFileAsHandled(localPath: String, syncedFolder: SyncedFolder) {
        val syncedFolderIdStr = syncedFolder.id.toString()

        try {
            dao.markFileAsUploaded(localPath, syncedFolderIdStr)
            Log_OC.d(TAG, "Marked file as uploaded: $localPath for syncedFolderId=$syncedFolderIdStr")
        } catch (e: Exception) {
            Log_OC.e(TAG, "markFileAsHandled(): ${e.message}", e)
        }
    }

    @JvmOverloads
    fun insertFromUri(uri: Uri, syncedFolder: SyncedFolder, checkFileType: Boolean = false) {
        val projection = arrayOf(
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.DATE_ADDED
        )

        var syncedPath = syncedFolder.localPath
        if (syncedPath.isNullOrEmpty()) {
            Log_OC.w(TAG, "Synced folder path is null or empty")
            return
        }

        if (!syncedPath.endsWith(File.separator)) {
            syncedPath += File.separator
        }

        val selection = "${MediaStore.MediaColumns.DATA} LIKE ?"
        val selectionArgs = arrayOf("$syncedPath%")

        Log_OC.d(TAG, "Querying MediaStore for files in: $syncedPath, uri: $uri")

        val cursor = context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            val idxData = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
            val idxModified = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
            val idxAdded = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)

            if (idxData == -1) {
                Log_OC.e(TAG, "MediaStore column DATA missing — cannot process URI: $uri")
                return
            }

            while (cursor.moveToNext()) {
                val filePath = cursor.getString(idxData)

                val lastModifiedMs = if (idxModified != -1) {
                    cursor.getLong(idxModified) * 1000
                } else {
                    null
                }

                val creationTimeMs = if (idxAdded != -1) {
                    cursor.getLong(idxAdded) * 1000
                } else {
                    null
                }

                Log_OC.d(
                    TAG,
                    "Found file: $filePath (created=$creationTimeMs, modified=$lastModifiedMs)"
                )

                insertOrReplace(filePath, lastModifiedMs, creationTimeMs, syncedFolder, checkFileType)
            }
        }
    }

    fun insertOrReplace(
        localPath: String?,
        lastModified: Long?,
        creationTime: Long?,
        syncedFolder: SyncedFolder,
        checkFileType: Boolean = false
    ) {
        try {
            val file = localPath?.toFile()
            if (file == null) {
                Log_OC.w(TAG, "file null, cannot insert or replace: $localPath")
                return
            }

            if (checkFileType && !syncedFolder.containsTypedFile(file, localPath)) {
                Log_OC.w(TAG, "synced folder not contains typed file: $localPath")
                return
            }

            val entity = dao.getFileByPathAndFolder(localPath, syncedFolder.id.toString())

            val fileModified = (lastModified ?: file.lastModified())
            val hasNotChanged = entity?.fileModified == fileModified
            val fileSentForUpload = entity?.fileSentForUpload == 1

            if (hasNotChanged && fileSentForUpload) {
                Log_OC.d(TAG, "File hasn't changed since last scan. skipping: $localPath")
                return
            }

            if (syncedFolder.shouldSkipFile(file, fileModified, creationTime, fileSentForUpload)) {
                return
            }

            val crc = getFileChecksum(file)
            val newEntity = FilesystemEntity(
                id = entity?.id,
                localPath = localPath,
                fileIsFolder = if (file.isDirectory) 1 else 0,
                fileFoundRecently = System.currentTimeMillis(),
                fileSentForUpload = 0, // Reset to 0 to queue for upload
                syncedFolderId = syncedFolder.id.toString(),
                crc32 = crc?.toString(),
                fileModified = fileModified
            )

            Log_OC.d(TAG, "inserting new file system entity: $newEntity")

            dao.insertOrReplace(newEntity)
        } catch (e: Exception) {
            Log_OC.e(TAG, "Failed to insert/update file: $localPath", e)
        }
    }

    private fun getFileChecksum(file: File): Long? = try {
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
