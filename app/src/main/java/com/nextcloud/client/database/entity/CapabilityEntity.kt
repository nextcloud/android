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

@Entity(tableName = ProviderTableMeta.CAPABILITIES_TABLE_NAME)
data class CapabilityEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = ProviderTableMeta._ID)
    val id: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_ASSISTANT)
    val assistant: Int?,
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
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_END_TO_END_ENCRYPTION_API_VERSION)
    val endToEndEncryptionApiVersion: String?,
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
    val dropAccount: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SECURITY_GUARD)
    val securityGuard: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_FORBIDDEN_FILENAME_CHARACTERS)
    val forbiddenFileNameCharacters: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_FORBIDDEN_FILENAMES)
    val forbiddenFileNames: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_FORBIDDEN_FORBIDDEN_FILENAME_EXTENSIONS)
    val forbiddenFileNameExtensions: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_FORBIDDEN_FORBIDDEN_FILENAME_BASE_NAMES)
    val forbiddenFilenameBaseNames: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_FILES_DOWNLOAD_LIMIT)
    val filesDownloadLimit: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_FILES_DOWNLOAD_LIMIT_DEFAULT)
    val filesDownloadLimitDefault: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_RECOMMENDATION)
    val recommendation: Int?
)
