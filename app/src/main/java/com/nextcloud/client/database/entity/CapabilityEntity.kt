/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
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
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_GOVERNANCE)
    val governance: Int?,
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
    val clientIntegrationJson: String?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_MOD_REWRITE_WORKING)
    val modRewriteWorking: Int?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_CHUNKED_UPLOAD_MAX_SIZE)
    val chunkedUploadMaxSize: Long?,
    @ColumnInfo(name = ProviderTableMeta.CAPABILITIES_SHARING_JSON)
    val sharingJson: String?
)

@Suppress("LongMethod", "CyclomaticComplexMethod")
fun CapabilityEntity?.toOCCapability(): OCCapability {
    val capability = OCCapability()
    if (this == null) return capability
    val id = this.id ?: return capability

    fun intToBoolean(value: Int?): CapabilityBooleanType =
        value?.let { CapabilityBooleanType.fromValue(it) } ?: CapabilityBooleanType.FALSE

    capability.id = id.toLong()
    capability.accountName = accountName
    capability.versionMayor = versionMajor ?: 0
    capability.versionMinor = versionMinor ?: 0
    capability.versionMicro = versionMicro ?: 0
    capability.versionString = versionString
    capability.versionEdition = versionEditor
    capability.extendedSupport = intToBoolean(extendedSupport)
    capability.corePollInterval = corePollinterval ?: 0
    capability.filesSharingApiEnabled = intToBoolean(sharingApiEnabled)
    capability.filesSharingPublicEnabled = intToBoolean(sharingPublicEnabled)
    capability.filesSharingPublicPasswordEnforced = intToBoolean(sharingPublicPasswordEnforced)
    capability.filesSharingPublicAskForOptionalPassword = intToBoolean(sharingPublicAskForOptionalPassword)
    capability.filesSharingPublicExpireDateEnabled = intToBoolean(sharingPublicExpireDateEnabled)
    capability.filesSharingPublicExpireDateDays = sharingPublicExpireDateDays ?: 0
    capability.filesSharingPublicExpireDateEnforced = intToBoolean(sharingPublicExpireDateEnforced)
    capability.filesSharingPublicSendMail = intToBoolean(sharingPublicSendMail)
    capability.filesSharingPublicUpload = intToBoolean(sharingPublicUpload)
    capability.filesSharingUserSendMail = intToBoolean(sharingUserSendMail)
    capability.filesSharingResharing = intToBoolean(sharingResharing)
    capability.filesSharingFederationOutgoing = intToBoolean(sharingFederationOutgoing)
    capability.filesSharingFederationIncoming = intToBoolean(sharingFederationIncoming)
    capability.filesBigFileChunking = intToBoolean(filesBigfilechunking)
    capability.filesUndelete = intToBoolean(filesUndelete)
    capability.filesVersioning = intToBoolean(filesVersioning)
    capability.externalLinks = intToBoolean(externalLinks)
    capability.serverName = serverName
    capability.serverColor = serverColor
    capability.serverTextColor = serverTextColor
    capability.serverElementColor = serverElementColor
    capability.serverSlogan = serverSlogan
    capability.serverLogo = serverLogo
    capability.serverBackground = serverBackgroundUrl
    capability.endToEndEncryption = intToBoolean(endToEndEncryption)
    capability.endToEndEncryptionKeysExist = intToBoolean(endToEndEncryptionKeysExist)
    capability.endToEndEncryptionApiVersion = endToEndEncryptionApiVersion?.let {
        E2EVersion.fromValue(it)
    } ?: E2EVersion.UNKNOWN
    capability.serverBackgroundDefault = intToBoolean(serverBackgroundDefault)
    capability.serverBackgroundPlain = intToBoolean(serverBackgroundPlain)
    capability.activity = intToBoolean(activity)
    capability.richDocuments = intToBoolean(richdocument)
    capability.richDocumentsDirectEditing = intToBoolean(richdocumentDirectEditing)
    capability.richDocumentsTemplatesAvailable = intToBoolean(richdocumentTemplates)
    capability.richDocumentsMimeTypeList = richdocumentMimetypeList.orEmpty().split(MIME_TYPE_SEPARATOR)
    capability.richDocumentsOptionalMimeTypeList =
        richdocumentOptionalMimetypeList.orEmpty().split(MIME_TYPE_SEPARATOR)
    capability.richDocumentsProductName = richdocumentProductName
    capability.directEditingEtag = directEditingEtag
    capability.etag = etag
    capability.userStatus = intToBoolean(userStatus)
    capability.userStatusSupportsEmoji = intToBoolean(userStatusSupportsEmoji)
    capability.userStatusSupportsBusy = intToBoolean(userStatusSupportsBusy)
    capability.filesLockingVersion = filesLockingVersion
    capability.assistant = intToBoolean(assistant)
    capability.groupfolders = intToBoolean(groupfolders)
    capability.dropAccount = intToBoolean(dropAccount)
    capability.securityGuard = intToBoolean(securityGuard)
    capability.governance = intToBoolean(governance)
    capability.forbiddenFilenameCharactersJson = forbiddenFileNameCharacters
    capability.forbiddenFilenamesJson = forbiddenFileNames
    capability.forbiddenFilenameExtensionJson = forbiddenFileNameExtensions
    capability.forbiddenFilenameBaseNamesJson = forbiddenFilenameBaseNames
    capability.isWCFEnabled = intToBoolean(isWCFEnabled)
    capability.filesDownloadLimit = intToBoolean(filesDownloadLimit)
    capability.filesDownloadLimitDefault = filesDownloadLimitDefault ?: 0
    capability.recommendations = intToBoolean(recommendation)
    capability.notesFolderPath = notesFolderPath
    capability.defaultPermissions = defaultPermissions ?: 0
    capability.hasValidSubscription = intToBoolean(hasValidSubscription)
    capability.clientIntegrationJson = clientIntegrationJson
    capability.modRewriteWorking = intToBoolean(modRewriteWorking)
    capability.chunkedUploadMaxSize = chunkedUploadMaxSize ?: 0L
    capability.sharingJson = sharingJson

    return capability
}

@Suppress("LongMethod")
fun OCCapability.toEntity(accountName: String, id: Int?): CapabilityEntity = CapabilityEntity(
    id = id,
    assistant = assistant.value,
    accountName = accountName,
    versionMajor = versionMayor,
    versionMinor = versionMinor,
    versionMicro = versionMicro,
    versionString = versionString,
    versionEditor = versionEdition,
    extendedSupport = extendedSupport.value,
    corePollinterval = corePollInterval,
    sharingApiEnabled = filesSharingApiEnabled.value,
    sharingPublicEnabled = filesSharingPublicEnabled.value,
    sharingPublicPasswordEnforced = filesSharingPublicPasswordEnforced.value,
    sharingPublicExpireDateEnabled = filesSharingPublicExpireDateEnabled.value,
    sharingPublicExpireDateDays = filesSharingPublicExpireDateDays,
    sharingPublicExpireDateEnforced = filesSharingPublicExpireDateEnforced.value,
    sharingPublicSendMail = filesSharingPublicSendMail.value,
    sharingPublicUpload = filesSharingPublicUpload.value,
    sharingUserSendMail = filesSharingUserSendMail.value,
    sharingResharing = filesSharingResharing.value,
    sharingFederationOutgoing = filesSharingFederationOutgoing.value,
    sharingFederationIncoming = filesSharingFederationIncoming.value,
    filesBigfilechunking = filesBigFileChunking.value,
    filesUndelete = filesUndelete.value,
    filesVersioning = filesVersioning.value,
    externalLinks = externalLinks.value,
    serverName = serverName,
    serverColor = serverColor,
    serverTextColor = serverTextColor,
    serverElementColor = serverElementColor,
    serverSlogan = serverSlogan,
    serverLogo = serverLogo,
    serverBackgroundUrl = serverBackground,
    endToEndEncryption = endToEndEncryption.value,
    endToEndEncryptionKeysExist = endToEndEncryptionKeysExist.value,
    endToEndEncryptionApiVersion = endToEndEncryptionApiVersion.value,
    activity = activity.value,
    serverBackgroundDefault = serverBackgroundDefault.value,
    serverBackgroundPlain = serverBackgroundPlain.value,
    richdocument = richDocuments.value,
    richdocumentMimetypeList = richDocumentsMimeTypeList?.joinToString(MIME_TYPE_SEPARATOR),
    richdocumentDirectEditing = richDocumentsDirectEditing.value,
    richdocumentTemplates = richDocumentsTemplatesAvailable.value,
    richdocumentOptionalMimetypeList = richDocumentsOptionalMimeTypeList?.joinToString(MIME_TYPE_SEPARATOR),
    sharingPublicAskForOptionalPassword = filesSharingPublicAskForOptionalPassword.value,
    richdocumentProductName = richDocumentsProductName,
    directEditingEtag = directEditingEtag,
    userStatus = userStatus.value,
    userStatusSupportsEmoji = userStatusSupportsEmoji.value,
    etag = etag,
    filesLockingVersion = filesLockingVersion,
    groupfolders = groupfolders.value,
    dropAccount = dropAccount.value,
    securityGuard = securityGuard.value,
    governance = governance.value,
    forbiddenFileNameCharacters = forbiddenFilenameCharactersJson,
    forbiddenFileNames = forbiddenFilenamesJson,
    forbiddenFileNameExtensions = forbiddenFilenameExtensionJson,
    forbiddenFilenameBaseNames = forbiddenFilenameBaseNamesJson,
    filesDownloadLimit = filesDownloadLimit.value,
    filesDownloadLimitDefault = filesDownloadLimitDefault,
    recommendation = recommendations.value,
    notesFolderPath = notesFolderPath,
    defaultPermissions = defaultPermissions,
    userStatusSupportsBusy = userStatusSupportsBusy.value,
    isWCFEnabled = isWCFEnabled.value,
    hasValidSubscription = hasValidSubscription.value,
    clientIntegrationJson = clientIntegrationJson,
    modRewriteWorking = modRewriteWorking.value,
    chunkedUploadMaxSize = chunkedUploadMaxSize,
    sharingJson = sharingJson
)

private const val MIME_TYPE_SEPARATOR = ","
