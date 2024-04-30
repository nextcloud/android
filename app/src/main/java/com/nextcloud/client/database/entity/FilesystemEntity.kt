/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
