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

@Entity(tableName = ProviderTableMeta.OCSHARES_TABLE_NAME)
data class ShareEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ProviderTableMeta._ID)
    val id: Int?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_FILE_SOURCE)
    val fileSource: Long?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_ITEM_SOURCE)
    val itemSource: Long?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_SHARE_TYPE)
    val shareType: Int?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_SHARE_WITH)
    val shareWith: String?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_PATH)
    val path: String?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_PERMISSIONS)
    val permissions: Int?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_SHARED_DATE)
    val sharedDate: Long?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_EXPIRATION_DATE)
    val expirationDate: Long?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_TOKEN)
    val token: String?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME)
    val shareWithDisplayName: String?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_IS_DIRECTORY)
    val isDirectory: Int?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_USER_ID)
    val userId: Int?,
    @ColumnInfo(name = ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED)
    val idRemoteShared: Long?,
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
    val shareLabel: String?
)
