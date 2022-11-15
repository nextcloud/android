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

@Entity(tableName = ProviderTableMeta.EXTERNAL_LINKS_TABLE_NAME)
data class ExternalLinkEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ProviderTableMeta._ID)
    val id: Int?,
    @ColumnInfo(name = ProviderTableMeta.EXTERNAL_LINKS_ICON_URL)
    val iconUrl: String?,
    @ColumnInfo(name = ProviderTableMeta.EXTERNAL_LINKS_LANGUAGE)
    val language: String?,
    @ColumnInfo(name = ProviderTableMeta.EXTERNAL_LINKS_TYPE)
    val type: Int?,
    @ColumnInfo(name = ProviderTableMeta.EXTERNAL_LINKS_NAME)
    val name: String?,
    @ColumnInfo(name = ProviderTableMeta.EXTERNAL_LINKS_URL)
    val url: String?,
    @ColumnInfo(name = ProviderTableMeta.EXTERNAL_LINKS_REDIRECT)
    val redirect: Int?
)
