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
    val hidden: Int?,
    @ColumnInfo(name = ProviderTableMeta.SYNCED_FOLDER_SUBFOLDER_RULE)
    val subFolderRule: Int?,
    @ColumnInfo(name = ProviderTableMeta.SYNCED_FOLDER_EXCLUDE_HIDDEN)
    val excludeHidden: Int?,
    @ColumnInfo(name = ProviderTableMeta.SYNCED_FOLDER_LAST_SCAN_TIMESTAMP_MS)
    val lastScanTimestampMs: Long?,
    @ColumnInfo(name = ProviderTableMeta.SYNCED_FOLDER_ALLOW_WHEN_AIRPLANE_MODE_ACTIVE)
    val allowWhenAirplaneModeActive: Int?
)
