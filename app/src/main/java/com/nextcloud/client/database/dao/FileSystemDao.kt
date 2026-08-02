/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nextcloud.client.database.entity.FilesystemEntity
import com.owncloud.android.db.ProviderMeta
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta

@Dao
interface FileSystemDao {
    @Query(
        """
    UPDATE ${ProviderMeta.ProviderTableMeta.FILESYSTEM_TABLE_NAME}
    SET ${ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_REMOTE_PATH} = :remotePath
    WHERE ${ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_LOCAL_PATH} = :localPath
      AND ${ProviderMeta.ProviderTableMeta.FILESYSTEM_SYNCED_FOLDER_ID} = :syncedFolderId
    """
    )
    suspend fun updateRemotePath(remotePath: String, localPath: String, syncedFolderId: String)

    @Query(
        """
    SELECT *
    FROM ${ProviderMeta.ProviderTableMeta.FILESYSTEM_TABLE_NAME}
    WHERE ${ProviderMeta.ProviderTableMeta.FILESYSTEM_SYNCED_FOLDER_ID} = :syncedFolderId
    """
    )
    suspend fun getBySyncedFolderId(syncedFolderId: String): List<FilesystemEntity>

    @Query(
        """
    SELECT COUNT(*) > 0 FROM ${ProviderMeta.ProviderTableMeta.FILESYSTEM_TABLE_NAME}
    WHERE ${ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_LOCAL_PATH} = :localPath
      AND ${ProviderMeta.ProviderTableMeta.FILESYSTEM_SYNCED_FOLDER_ID} IS NOT NULL
    LIMIT 1
"""
    )
    suspend fun isBelongToAnyAutoFolder(localPath: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertOrReplace(filesystemEntity: FilesystemEntity)

    @Delete
    fun delete(entity: FilesystemEntity)

    @Query(
        """
        DELETE FROM ${ProviderMeta.ProviderTableMeta.FILESYSTEM_TABLE_NAME}
        WHERE ${ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_LOCAL_PATH} = :localPath
          AND ${ProviderMeta.ProviderTableMeta._ID} = :id
        """
    )
    suspend fun deleteByLocalPathAndId(localPath: String, id: Int)

    @Query(
        """
        SELECT *
        FROM ${ProviderMeta.ProviderTableMeta.FILESYSTEM_TABLE_NAME}
        WHERE ${ProviderMeta.ProviderTableMeta.FILESYSTEM_SYNCED_FOLDER_ID} = :syncedFolderId
          AND ${ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_SENT_FOR_UPLOAD} = 0
          AND ${ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_IS_FOLDER} = 0
          AND ${ProviderMeta.ProviderTableMeta._ID} > :lastId
        ORDER BY ${ProviderMeta.ProviderTableMeta._ID}
        LIMIT :limit
    """
    )
    suspend fun getAutoUploadFilesEntities(syncedFolderId: String, limit: Int, lastId: Int): List<FilesystemEntity>

    @Query(
        """
        UPDATE ${ProviderMeta.ProviderTableMeta.FILESYSTEM_TABLE_NAME}
        SET ${ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_SENT_FOR_UPLOAD} = 1
        WHERE ${ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_LOCAL_PATH} = :localPath
          AND ${ProviderMeta.ProviderTableMeta.FILESYSTEM_SYNCED_FOLDER_ID} = :syncedFolderId
    """
    )
    suspend fun markFileAsUploaded(localPath: String, syncedFolderId: String)

    @Query(
        """
    SELECT *
    FROM ${ProviderMeta.ProviderTableMeta.FILESYSTEM_TABLE_NAME}
    WHERE ${ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_LOCAL_PATH} = :localPath
      AND ${ProviderMeta.ProviderTableMeta.FILESYSTEM_SYNCED_FOLDER_ID} = :syncedFolderId
    LIMIT 1
    """
    )
    fun getFileByPathAndFolder(localPath: String, syncedFolderId: String): FilesystemEntity?

    @Query(
        """
    SELECT COUNT(*) > 0
    FROM ${ProviderMeta.ProviderTableMeta.FILESYSTEM_TABLE_NAME}
    WHERE ${ProviderMeta.ProviderTableMeta.FILESYSTEM_SYNCED_FOLDER_ID} = :syncedFolderId
      AND ${ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_SENT_FOR_UPLOAD} = 0
      AND ${ProviderMeta.ProviderTableMeta.FILESYSTEM_FILE_IS_FOLDER} = 0
    LIMIT 1
    """
    )
    suspend fun hasPendingFiles(syncedFolderId: String): Boolean

    /**
     * Queues every file of the given folder again that was marked as sent for upload but has no successful upload
     * to prove it. Files with a successful upload keep their state so a rescan never uploads them twice.
     */
    @Query(
        """
    UPDATE ${ProviderTableMeta.FILESYSTEM_TABLE_NAME}
    SET ${ProviderTableMeta.FILESYSTEM_FILE_SENT_FOR_UPLOAD} = 0
    WHERE ${ProviderTableMeta.FILESYSTEM_SYNCED_FOLDER_ID} = :syncedFolderId
      AND ${ProviderTableMeta.FILESYSTEM_FILE_SENT_FOR_UPLOAD} = 1
      AND ${ProviderTableMeta.FILESYSTEM_FILE_IS_FOLDER} = 0
      AND NOT EXISTS (
          SELECT 1 FROM ${ProviderTableMeta.UPLOADS_TABLE_NAME} upload
          WHERE upload.${ProviderTableMeta.UPLOADS_LOCAL_PATH} =
                ${ProviderTableMeta.FILESYSTEM_TABLE_NAME}.${ProviderTableMeta.FILESYSTEM_FILE_LOCAL_PATH}
            AND upload.${ProviderTableMeta.UPLOADS_STATUS} = :succeededStatus
      )
    """
    )
    suspend fun requeueFilesWithoutSuccessfulUpload(syncedFolderId: String, succeededStatus: Int): Int
}
