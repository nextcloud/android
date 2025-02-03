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

@Entity(tableName = ProviderTableMeta.OCSHARES_TABLE_NAME)
data class ShareEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ProviderTableMeta._ID)
    val id: Int?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_FILE_SOURCE)
    val fileSource: Int?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_ITEM_SOURCE)
    val itemSource: Int?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_SHARE_TYPE)
    val shareType: Int?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_SHARE_WITH)
    val shareWith: String?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_PATH)
    val path: String?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_PERMISSIONS)
    val permissions: Int?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_SHARED_DATE)
    val sharedDate: Int?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_EXPIRATION_DATE)
    val expirationDate: Int?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_TOKEN)
    val token: String?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME)
    val shareWithDisplayName: String?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_IS_DIRECTORY)
    val isDirectory: Int?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_USER_ID)
    val userId: String?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED)
    val idRemoteShared: Int?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_ACCOUNT_OWNER)
    val accountOwner: String?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_IS_PASSWORD_PROTECTED)
    val isPasswordProtected: Int?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_NOTE)
    val note: String?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_HIDE_DOWNLOAD)
    val hideDownload: Int?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_SHARE_LINK)
    val shareLink: String?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_SHARE_LABEL)
    val shareLabel: String?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_DOWNLOADLIMIT_LIMIT)
    val downloadLimitLimit: Int?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_DOWNLOADLIMIT_COUNT)
    val downloadLimitCount: Int?
)
