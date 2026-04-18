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
import com.owncloud.android.lib.resources.status.CapabilityBooleanType
import com.owncloud.android.lib.resources.status.E2EVersion
import com.owncloud.android.lib.resources.status.OCCapability

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
    val forbiddenFileNameCharacters: String?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_FORBIDDEN_FILENAMES)
    val forbiddenFileNames: String?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_FORBIDDEN_FORBIDDEN_FILENAME_EXTENSIONS)
    val forbiddenFileNameExtensions: String?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_FORBIDDEN_FORBIDDEN_FILENAME_BASE_NAMES)
    val forbiddenFilenameBaseNames: String?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_FILES_DOWNLOAD_LIMIT)
    val filesDownloadLimit: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_FILES_DOWNLOAD_LIMIT_DEFAULT)
    val filesDownloadLimitDefault: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_RECOMMENDATION)
    val recommendation: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_NOTES_FOLDER_PATH)
    val notesFolderPath: String?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_DEFAULT_PERMISSIONS)
    val defaultPermissions: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_USER_STATUS_SUPPORTS_BUSY)
    val userStatusSupportsBusy: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_WINDOWS_COMPATIBLE_FILENAMES)
    val isWCFEnabled: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_HAS_VALID_SUBSCRIPTION)
    val hasValidSubscription: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_CLIENT_INTEGRATION_JSON)
    val clientIntegrationJson: String?
)

@Suppress("LongMethod", "ReturnCount")
fun CapabilityEntity?.toOCCapability(): OCCapability {
    val capability = OCCapability()
    if (this == null) return capability
    val id = this.id ?: return capability

    fun intToBoolean(value: Int?): CapabilityBooleanType =
        value?.let { CapabilityBooleanType.fromValue(it) } ?: CapabilityBooleanType.UNKNOWN

    capability.id = id.toLong()
    capability.accountName = this.accountName
    capability.versionMayor = this.versionMajor ?: 0
    capability.versionMinor = this.versionMinor ?: 0
    capability.versionMicro = this.versionMicro ?: 0
    capability.versionString = this.versionString
    capability.versionEdition = this.versionEditor
    capability.extendedSupport = intToBoolean(this.extendedSupport)
    capability.corePollInterval = this.corePollinterval ?: 0
    capability.filesSharingApiEnabled = intToBoolean(this.sharingApiEnabled)
    capability.filesSharingPublicEnabled = intToBoolean(this.sharingPublicEnabled)
    capability.filesSharingPublicPasswordEnforced = intToBoolean(this.sharingPublicPasswordEnforced)
    capability.filesSharingPublicAskForOptionalPassword = intToBoolean(this.sharingPublicAskForOptionalPassword)
    capability.filesSharingPublicExpireDateEnabled = intToBoolean(this.sharingPublicExpireDateEnabled)
    capability.filesSharingPublicExpireDateDays = this.sharingPublicExpireDateDays ?: 0
    capability.filesSharingPublicExpireDateEnforced = intToBoolean(this.sharingPublicExpireDateEnforced)
    capability.filesSharingPublicSendMail = intToBoolean(this.sharingPublicSendMail)
    capability.filesSharingPublicUpload = intToBoolean(this.sharingPublicUpload)
    capability.filesSharingUserSendMail = intToBoolean(this.sharingUserSendMail)
    capability.filesSharingResharing = intToBoolean(this.sharingResharing)
    capability.filesSharingFederationOutgoing = intToBoolean(this.sharingFederationOutgoing)
    capability.filesSharingFederationIncoming = intToBoolean(this.sharingFederationIncoming)
    capability.filesBigFileChunking = intToBoolean(this.filesBigfilechunking)
    capability.filesUndelete = intToBoolean(this.filesUndelete)
    capability.filesVersioning = intToBoolean(this.filesVersioning)
    capability.externalLinks = intToBoolean(this.externalLinks)
    capability.serverName = this.serverName
    capability.serverColor = this.serverColor
    capability.serverTextColor = this.serverTextColor
    capability.serverElementColor = this.serverElementColor
    capability.serverSlogan = this.serverSlogan
    capability.serverLogo = this.serverLogo
    capability.serverBackground = this.serverBackgroundUrl
    capability.endToEndEncryption = intToBoolean(this.endToEndEncryption)
    capability.endToEndEncryptionKeysExist = intToBoolean(this.endToEndEncryptionKeysExist)
    capability.endToEndEncryptionApiVersion = this.endToEndEncryptionApiVersion?.let {
        E2EVersion.fromValue(it)
    } ?: E2EVersion.UNKNOWN
    capability.serverBackgroundDefault = intToBoolean(this.serverBackgroundDefault)
    capability.serverBackgroundPlain = intToBoolean(this.serverBackgroundPlain)
    capability.activity = intToBoolean(this.activity)
    capability.richDocuments = intToBoolean(this.richdocument)
    capability.richDocumentsDirectEditing = intToBoolean(this.richdocumentDirectEditing)
    capability.richDocumentsTemplatesAvailable = intToBoolean(this.richdocumentTemplates)
    capability.richDocumentsMimeTypeList = this.richdocumentMimetypeList?.split(",") ?: emptyList()
    capability.richDocumentsOptionalMimeTypeList = this.richdocumentOptionalMimetypeList?.split(",") ?: emptyList()
    capability.richDocumentsProductName = this.richdocumentProductName
    capability.directEditingEtag = this.directEditingEtag
    capability.etag = this.etag
    capability.userStatus = intToBoolean(this.userStatus)
    capability.userStatusSupportsEmoji = intToBoolean(this.userStatusSupportsEmoji)
    capability.userStatusSupportsBusy = intToBoolean(this.userStatusSupportsBusy)
    capability.filesLockingVersion = this.filesLockingVersion
    capability.assistant = intToBoolean(this.assistant)
    capability.groupfolders = intToBoolean(this.groupfolders)
    capability.dropAccount = intToBoolean(this.dropAccount)
    capability.securityGuard = intToBoolean(this.securityGuard)
    capability.forbiddenFilenameCharactersJson = this.forbiddenFileNameCharacters
    capability.forbiddenFilenamesJson = this.forbiddenFileNames
    capability.forbiddenFilenameExtensionJson = this.forbiddenFileNameExtensions
    capability.forbiddenFilenameBaseNamesJson = this.forbiddenFilenameBaseNames
    capability.isWCFEnabled = intToBoolean(this.isWCFEnabled)
    capability.filesDownloadLimit = intToBoolean(this.filesDownloadLimit)
    capability.filesDownloadLimitDefault = this.filesDownloadLimitDefault ?: 0
    capability.recommendations = intToBoolean(this.recommendation)
    capability.notesFolderPath = this.notesFolderPath
    capability.defaultPermissions = this.defaultPermissions ?: 0
    capability.hasValidSubscription = intToBoolean(this.hasValidSubscription)
    capability.clientIntegrationJson = this.clientIntegrationJson

    return capability
}
