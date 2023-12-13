/*
 * Nextcloud Android client application
 *
 *  @author Dariusz Olszewski
 *  Copyright (C) 2022 Dariusz Olszewski
 *  Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.nextcloud.client.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.nextcloud.client.database.entity.FileEntity
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta

@Dao
interface FileDao {
    @Query("SELECT * FROM filelist WHERE _id = :id LIMIT 1")
    fun getFileById(id: Long): FileEntity?

    @Query("SELECT * FROM filelist WHERE path = :path AND file_owner = :fileOwner LIMIT 1")
    fun getFileByEncryptedRemotePath(path: String, fileOwner: String): FileEntity?

    @Query("SELECT * FROM filelist WHERE path_decrypted = :path AND file_owner = :fileOwner LIMIT 1")
    fun getFileByDecryptedRemotePath(path: String, fileOwner: String): FileEntity?

    @Query("SELECT * FROM filelist WHERE media_path = :path AND file_owner = :fileOwner LIMIT 1")
    fun getFileByLocalPath(path: String, fileOwner: String): FileEntity?

    @Query("SELECT * FROM filelist WHERE remote_id = :remoteId AND file_owner = :fileOwner LIMIT 1")
    fun getFileByRemoteId(remoteId: String, fileOwner: String): FileEntity?

    @Query("SELECT * FROM filelist WHERE parent = :parentId ORDER BY ${ProviderTableMeta.FILE_DEFAULT_SORT_ORDER}")
    fun getFolderContent(parentId: Long): List<FileEntity>

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
}
