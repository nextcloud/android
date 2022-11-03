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

@Entity(tableName = ProviderTableMeta.UPLOADS_TABLE_NAME)
data class UploadEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ProviderTableMeta._ID)
    val id: Int?,
    @ColumnInfo(name = ProviderTableMeta.UPLOADS_LOCAL_PATH)
    val localPath: String?,
    @ColumnInfo(name = ProviderTableMeta.UPLOADS_REMOTE_PATH)
    val remotePath: String?,
    @ColumnInfo(name = ProviderTableMeta.UPLOADS_ACCOUNT_NAME)
    val accountName: String?,
    @ColumnInfo(name = ProviderTableMeta.UPLOADS_FILE_SIZE)
    val fileSize: Long?,
    @ColumnInfo(name = ProviderTableMeta.UPLOADS_STATUS)
    val status: Int?,
    @ColumnInfo(name = ProviderTableMeta.UPLOADS_LOCAL_BEHAVIOUR)
    val localBehaviour: Int?,
    @ColumnInfo(name = ProviderTableMeta.UPLOADS_UPLOAD_TIME)
    val uploadTime: Int?,
    @ColumnInfo(name = ProviderTableMeta.UPLOADS_NAME_COLLISION_POLICY)
    val nameCollisionPolicy: Int?,
    @ColumnInfo(name = ProviderTableMeta.UPLOADS_IS_CREATE_REMOTE_FOLDER)
    val isCreateRemoteFolder: Int?,
    @ColumnInfo(name = ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP)
    val uploadEndTimestamp: Int?,
    @ColumnInfo(name = ProviderTableMeta.UPLOADS_LAST_RESULT)
    val lastResult: Int?,
    @ColumnInfo(name = ProviderTableMeta.UPLOADS_IS_WHILE_CHARGING_ONLY)
    val isWhileChargingOnly: Int?,
    @ColumnInfo(name = ProviderTableMeta.UPLOADS_IS_WIFI_ONLY)
    val isWifiOnly: Int?,
    @ColumnInfo(name = ProviderTableMeta.UPLOADS_CREATED_BY)
    val createdBy: Int?,
    @ColumnInfo(name = ProviderTableMeta.UPLOADS_FOLDER_UNLOCK_TOKEN)
    val folderUnlockToken: String?
)
