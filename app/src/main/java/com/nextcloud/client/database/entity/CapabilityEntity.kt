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

@Entity(tableName = ProviderTableMeta.CAPABILITIES_TABLE_NAME)
data class CapabilityEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ProviderTableMeta._ID)
    val id: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_ACCOUNT_NAME)
    val accountName: String?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_VERSION_MAYOR)
    val versionMajor: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_VERSION_MINOR)
    val versionMinor: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_VERSION_MICRO)
    val versionMicro: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_VERSION_STRING)
    val versionString: String?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_VERSION_EDITION)
    val versionEditor: String?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_EXTENDED_SUPPORT)
    val extendedSupport: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_CORE_POLLINTERVAL)
    val corePollinterval: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_API_ENABLED)
    val sharingApiEnabled: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ENABLED)
    val sharingPublicEnabled: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED)
    val sharingPublicPasswordEnforced: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED)
    val sharingPublicExpireDateEnabled: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS)
    val sharingPublicExpireDateDays: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED)
    val sharingPublicExpireDateEnforced: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_SEND_MAIL)
    val sharingPublicSendMail: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_UPLOAD)
    val sharingPublicUpload: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_USER_SEND_MAIL)
    val sharingUserSendMail: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_RESHARING)
    val sharingResharing: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_OUTGOING)
    val sharingFederationOutgoing: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_FEDERATION_INCOMING)
    val sharingFederationIncoming: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_FILES_BIGFILECHUNKING)
    val filesBigfilechunking: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_FILES_UNDELETE)
    val filesUndelete: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_FILES_VERSIONING)
    val filesVersioning: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_EXTERNAL_LINKS)
    val externalLinks: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SERVER_NAME)
    val serverName: String?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SERVER_COLOR)
    val serverColor: String?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SERVER_TEXT_COLOR)
    val serverTextColor: String?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SERVER_ELEMENT_COLOR)
    val serverElementColor: String?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SERVER_SLOGAN)
    val serverSlogan: String?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SERVER_LOGO)
    val serverLogo: String?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SERVER_BACKGROUND_URL)
    val serverBackgroundUrl: String?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_END_TO_END_ENCRYPTION)
    val endToEndEncryption: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_END_TO_END_ENCRYPTION_KEYS_EXIST)
    val endToEndEncryptionKeysExist: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_ACTIVITY)
    val activity: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SERVER_BACKGROUND_DEFAULT)
    val serverBackgroundDefault: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SERVER_BACKGROUND_PLAIN)
    val serverBackgroundPlain: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_RICHDOCUMENT)
    val richdocument: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_MIMETYPE_LIST)
    val richdocumentMimetypeList: String?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_DIRECT_EDITING)
    val richdocumentDirectEditing: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_TEMPLATES)
    val richdocumentTemplates: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_OPTIONAL_MIMETYPE_LIST)
    val richdocumentOptionalMimetypeList: String?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ASK_FOR_OPTIONAL_PASSWORD)
    val sharingPublicAskForOptionalPassword: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_PRODUCT_NAME)
    val richdocumentProductName: String?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_DIRECT_EDITING_ETAG)
    val directEditingEtag: String?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_USER_STATUS)
    val userStatus: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_USER_STATUS_SUPPORTS_EMOJI)
    val userStatusSupportsEmoji: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_ETAG)
    val etag: String?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_FILES_LOCKING_VERSION)
    val filesLockingVersion: String?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_GROUPFOLDERS)
    val groupfolders: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_DROP_ACCOUNT)
    val dropAccount: Int?
)
