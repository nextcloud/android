/*
 * Nextcloud Android client application
 *
 *  @author Álvaro Brey
 *  Copyright (C) 2022 Álvaro Brey
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

package com.nextcloud.client.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta

@Entity(tableName = ProviderTableMeta.FILESYSTEM_TABLE_NAME)
data class FilesystemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ProviderTableMeta._ID)
    val id: Int?,
    @ColumnInfo(name = ProviderTableMeta.FILESYSTEM_FILE_LOCAL_PATH)
    val localPath: String?,
    @ColumnInfo(name = ProviderTableMeta.FILESYSTEM_FILE_IS_FOLDER)
    val fileIsFolder: Int?,
    @ColumnInfo(name = ProviderTableMeta.FILESYSTEM_FILE_FOUND_RECENTLY)
    val fileFoundRecently: Long?,
    @ColumnInfo(name = ProviderTableMeta.FILESYSTEM_FILE_SENT_FOR_UPLOAD)
    val fileSentForUpload: Int?,
    @ColumnInfo(name = ProviderTableMeta.FILESYSTEM_SYNCED_FOLDER_ID)
    val syncedFolderId: String?,
    @ColumnInfo(name = ProviderTableMeta.FILESYSTEM_CRC32)
    val crc32: String?,
    @ColumnInfo(name = ProviderTableMeta.FILESYSTEM_FILE_MODIFIED)
    val fileModified: Long?
)
