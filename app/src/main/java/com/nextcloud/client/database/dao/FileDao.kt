/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Dariusz Olszewski <starypatyk@users.noreply.github.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.nextcloud.client.database.entity.FileEntity
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta

@Dao
interface FileDao {
    @Query("SELECT * FROM filelist WHERE _id = :id LIMIT 1")
    fun getFileById(id: Long): FileEntity?

    @Query("SELECT * FROM filelist WHERE local_id = :localId LIMIT 1")
    fun getFileByLocalId(localId: Long): FileEntity?

    @Query("SELECT * FROM filelist WHERE path = :path AND file_owner = :fileOwner LIMIT 1")
    fun getFileByEncryptedRemotePath(path: String, fileOwner: String): FileEntity?

    @Query("SELECT * FROM filelist WHERE path_decrypted = :path AND file_owner = :fileOwner LIMIT 1")
    fun getFileByDecryptedRemotePath(path: String, fileOwner: String): FileEntity?

    @Query("SELECT * FROM filelist WHERE media_path = :path AND file_owner = :fileOwner LIMIT 1")
    fun getFileByLocalPath(path: String, fileOwner: String): FileEntity?

    @Query("SELECT * FROM filelist WHERE remote_id = :remoteId AND file_owner = :fileOwner LIMIT 1")
    fun getFileByRemoteId(remoteId: String, fileOwner: String): FileEntity?

    @Query("SELECT * FROM filelist WHERE parent = :parentId")
    fun getFolderContent(parentId: Long): List<FileEntity>

    @RawQuery
    fun getFolderContentByQuery(query: SupportSQLiteQuery): List<FileEntity>

    @Query("SELECT count(_id) == 0 FROM filelist WHERE parent = :parentId")
    fun getFolderIsEmpty(parentId: Long): Boolean

    @Query(
        "SELECT * FROM filelist WHERE modified >= :startDate" +
            " AND modified < :endDate" +
            " AND (content_type LIKE 'image/%' OR content_type LIKE 'video/%')" +
            " AND file_owner = :fileOwner" +
            " ORDER BY ${ProviderTableMeta.FILE_DEFAULT_SORT_ORDER}"
    )
    fun getGalleryItems(startDate: Long, endDate: Long, fileOwner: String): List<FileEntity>

    @Query("SELECT * FROM filelist WHERE file_owner = :fileOwner ORDER BY ${ProviderTableMeta.FILE_DEFAULT_SORT_ORDER}")
    fun getAllFiles(fileOwner: String): List<FileEntity>

    @Query("SELECT * FROM filelist WHERE path LIKE :pathPattern AND file_owner = :fileOwner ORDER BY path ASC")
    fun getFolderWithDescendants(pathPattern: String, fileOwner: String): List<FileEntity>

    @Query("SELECT * FROM filelist where file_owner = :fileOwner AND etag_in_conflict IS NOT NULL")
    fun getFilesWithSyncConflict(fileOwner: String): List<FileEntity>

    @Query(
        "SELECT * FROM filelist where file_owner = :fileOwner AND internal_two_way_sync_timestamp >= 0 " +
            "ORDER BY internal_two_way_sync_timestamp DESC"
    )
    fun getInternalTwoWaySyncFolders(fileOwner: String): List<FileEntity>
}
