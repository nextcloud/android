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

@Entity(tableName = ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME)
data class SyncedFolderEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ProviderTableMeta._ID)
    val id: Int?,
    @ColumnInfo(name = ProviderTableMeta.SYNCED_FOLDER_LOCAL_PATH)
    val localPath: String?,
    @ColumnInfo(name = ProviderTableMeta.SYNCED_FOLDER_REMOTE_PATH)
    val remotePath: String?,
    @ColumnInfo(name = ProviderTableMeta.SYNCED_FOLDER_WIFI_ONLY)
    val wifiOnly: Int?,
    @ColumnInfo(name = ProviderTableMeta.SYNCED_FOLDER_CHARGING_ONLY)
    val chargingOnly: Int?,
    @ColumnInfo(name = ProviderTableMeta.SYNCED_FOLDER_EXISTING)
    val existing: Int?,
    @ColumnInfo(name = ProviderTableMeta.SYNCED_FOLDER_ENABLED)
    val enabled: Int?,
    @ColumnInfo(name = ProviderTableMeta.SYNCED_FOLDER_ENABLED_TIMESTAMP_MS)
    val enabledTimestampMs: Int?,
    @ColumnInfo(name = ProviderTableMeta.SYNCED_FOLDER_SUBFOLDER_BY_DATE)
    val subfolderByDate: Int?,
    @ColumnInfo(name = ProviderTableMeta.SYNCED_FOLDER_ACCOUNT)
    val account: String?,
    @ColumnInfo(name = ProviderTableMeta.SYNCED_FOLDER_UPLOAD_ACTION)
    val uploadAction: Int?,
    @ColumnInfo(name = ProviderTableMeta.SYNCED_FOLDER_NAME_COLLISION_POLICY)
    val nameCollisionPolicy: Int?,
    @ColumnInfo(name = ProviderTableMeta.SYNCED_FOLDER_TYPE)
    val type: Int?,
    @ColumnInfo(name = ProviderTableMeta.SYNCED_FOLDER_HIDDEN)
    val hidden: Int?
)
