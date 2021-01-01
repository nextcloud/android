/*
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author David A. Velasco
 * @author masensio
 * Copyright (C) 2011  Bartek Przybylski
 * Copyright (C) 2016 ownCloud Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.db

import android.net.Uri
import android.provider.BaseColumns
import com.owncloud.android.MainApp
import java.util.Arrays
import java.util.Collections

/**
 * Meta-Class that holds various static field information
 */
object ProviderMeta {
    const val DB_NAME = "filelist"
    const val DB_VERSION = 61

    object ProviderTableMeta : BaseColumns {
        const val FILE_TABLE_NAME = "filelist"
        const val OCSHARES_TABLE_NAME = "ocshares"
        const val CAPABILITIES_TABLE_NAME = "capabilities"
        const val UPLOADS_TABLE_NAME = "list_of_uploads"
        const val SYNCED_FOLDERS_TABLE_NAME = "synced_folders"
        const val EXTERNAL_LINKS_TABLE_NAME = "external_links"
        const val ARBITRARY_DATA_TABLE_NAME = "arbitrary_data"
        const val VIRTUAL_TABLE_NAME = "virtual"
        const val FILESYSTEM_TABLE_NAME = "filesystem"
        const val EDITORS_TABLE_NAME = "editors"
        const val CREATORS_TABLE_NAME = "creators"
        private const val CONTENT_PREFIX = "content://"
        @JvmField
        val CONTENT_URI = Uri.parse(
            CONTENT_PREFIX +
                MainApp.getAuthority() + "/"
        )
        @JvmField
        val CONTENT_URI_FILE = Uri.parse(
            CONTENT_PREFIX +
                MainApp.getAuthority() + "/file"
        )
        @JvmField
        val CONTENT_URI_DIR = Uri.parse(
            CONTENT_PREFIX +
                MainApp.getAuthority() + "/dir"
        )
        @JvmField
        val CONTENT_URI_SHARE = Uri.parse(
            CONTENT_PREFIX +
                MainApp.getAuthority() + "/shares"
        )
        @JvmField
        val CONTENT_URI_CAPABILITIES = Uri.parse(
            CONTENT_PREFIX +
                MainApp.getAuthority() + "/capabilities"
        )
        @JvmField
        val CONTENT_URI_UPLOADS = Uri.parse(
            CONTENT_PREFIX +
                MainApp.getAuthority() + "/uploads"
        )
        @JvmField
        val CONTENT_URI_SYNCED_FOLDERS = Uri.parse(
            CONTENT_PREFIX +
                MainApp.getAuthority() + "/synced_folders"
        )
        @JvmField
        val CONTENT_URI_EXTERNAL_LINKS = Uri.parse(
            CONTENT_PREFIX +
                MainApp.getAuthority() + "/external_links"
        )
        @JvmField
        val CONTENT_URI_ARBITRARY_DATA = Uri.parse(
            CONTENT_PREFIX +
                MainApp.getAuthority() + "/arbitrary_data"
        )
        @JvmField
        val CONTENT_URI_VIRTUAL = Uri.parse(CONTENT_PREFIX + MainApp.getAuthority() + "/virtual")
        @JvmField
        val CONTENT_URI_FILESYSTEM = Uri.parse(
            CONTENT_PREFIX +
                MainApp.getAuthority() + "/filesystem"
        )
        const val CONTENT_TYPE = "vnd.android.cursor.dir/vnd.owncloud.file"
        const val CONTENT_TYPE_ITEM = "vnd.android.cursor.item/vnd.owncloud.file"

        // Columns of filelist table
        const val FILE_PARENT = "parent"
        const val FILE_NAME = "filename"
        const val FILE_ENCRYPTED_NAME = "encrypted_filename"
        const val FILE_CREATION = "created"
        const val FILE_MODIFIED = "modified"
        const val FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA = "modified_at_last_sync_for_data"
        const val FILE_CONTENT_LENGTH = "content_length"
        const val FILE_CONTENT_TYPE = "content_type"
        const val FILE_STORAGE_PATH = "media_path"
        const val FILE_PATH = "path"
        const val FILE_PATH_DECRYPTED = "path_decrypted"
        const val FILE_ACCOUNT_OWNER = "file_owner"
        const val FILE_LAST_SYNC_DATE = "last_sync_date" // _for_properties, but let's keep it as it is
        const val FILE_LAST_SYNC_DATE_FOR_DATA = "last_sync_date_for_data"
        const val FILE_KEEP_IN_SYNC = "keep_in_sync"
        const val FILE_ETAG = "etag"
        const val FILE_ETAG_ON_SERVER = "etag_on_server"
        const val FILE_SHARED_VIA_LINK = "share_by_link"
        const val FILE_SHARED_WITH_SHAREE = "shared_via_users"
        const val FILE_PERMISSIONS = "permissions"
        const val FILE_REMOTE_ID = "remote_id"
        const val FILE_UPDATE_THUMBNAIL = "update_thumbnail"
        const val FILE_IS_DOWNLOADING = "is_downloading"
        const val FILE_ETAG_IN_CONFLICT = "etag_in_conflict"
        const val FILE_FAVORITE = "favorite"
        const val FILE_IS_ENCRYPTED = "is_encrypted"
        const val FILE_MOUNT_TYPE = "mount_type"
        const val FILE_HAS_PREVIEW = "has_preview"
        const val FILE_UNREAD_COMMENTS_COUNT = "unread_comments_count"
        const val FILE_OWNER_ID = "owner_id"
        const val FILE_OWNER_DISPLAY_NAME = "owner_display_name"
        const val FILE_NOTE = "note"
        const val FILE_SHAREES = "sharees"
        const val FILE_RICH_WORKSPACE = "rich_workspace"
        @JvmField
        val FILE_ALL_COLUMNS = Collections.unmodifiableList(
            Arrays.asList(
                BaseColumns._ID,
                FILE_PARENT,
                FILE_NAME,
                FILE_CREATION,
                FILE_MODIFIED,
                FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA,
                FILE_CONTENT_LENGTH,
                FILE_CONTENT_TYPE,
                FILE_STORAGE_PATH,
                FILE_PATH,
                FILE_ACCOUNT_OWNER,
                FILE_LAST_SYNC_DATE,
                FILE_LAST_SYNC_DATE_FOR_DATA,
                FILE_ETAG,
                FILE_ETAG_ON_SERVER,
                FILE_SHARED_VIA_LINK,
                FILE_SHARED_WITH_SHAREE,
                FILE_PERMISSIONS,
                FILE_REMOTE_ID,
                FILE_UPDATE_THUMBNAIL,
                FILE_IS_DOWNLOADING,
                FILE_ETAG_IN_CONFLICT,
                FILE_FAVORITE,
                FILE_IS_ENCRYPTED,
                FILE_MOUNT_TYPE,
                FILE_HAS_PREVIEW,
                FILE_UNREAD_COMMENTS_COUNT,
                FILE_SHAREES,
                FILE_RICH_WORKSPACE
            )
        )
        const val FILE_DEFAULT_SORT_ORDER = FILE_NAME + " collate nocase asc"

        // Columns of ocshares table
        const val OCSHARES_FILE_SOURCE = "file_source"
        const val OCSHARES_ITEM_SOURCE = "item_source"
        const val OCSHARES_SHARE_TYPE = "share_type"
        const val OCSHARES_SHARE_WITH = "shate_with"
        const val OCSHARES_PATH = "path"
        const val OCSHARES_PERMISSIONS = "permissions"
        const val OCSHARES_SHARED_DATE = "shared_date"
        const val OCSHARES_EXPIRATION_DATE = "expiration_date"
        const val OCSHARES_TOKEN = "token"
        const val OCSHARES_SHARE_WITH_DISPLAY_NAME = "shared_with_display_name"
        const val OCSHARES_IS_DIRECTORY = "is_directory"
        const val OCSHARES_USER_ID = "user_id"
        const val OCSHARES_ID_REMOTE_SHARED = "id_remote_shared"
        const val OCSHARES_ACCOUNT_OWNER = "owner_share"
        const val OCSHARES_IS_PASSWORD_PROTECTED = "is_password_protected"
        const val OCSHARES_NOTE = "note"
        const val OCSHARES_HIDE_DOWNLOAD = "hide_download"
        const val OCSHARES_SHARE_LINK = "share_link"
        const val OCSHARES_SHARE_LABEL = "share_label"
        const val OCSHARES_DEFAULT_SORT_ORDER = (
            OCSHARES_FILE_SOURCE +
                " collate nocase asc"
            )

        // Columns of capabilities table
        const val CAPABILITIES_ACCOUNT_NAME = "account"
        const val CAPABILITIES_VERSION_MAYOR = "version_mayor"
        const val CAPABILITIES_VERSION_MINOR = "version_minor"
        const val CAPABILITIES_VERSION_MICRO = "version_micro"
        const val CAPABILITIES_VERSION_STRING = "version_string"
        const val CAPABILITIES_VERSION_EDITION = "version_edition"
        const val CAPABILITIES_EXTENDED_SUPPORT = "extended_support"
        const val CAPABILITIES_CORE_POLLINTERVAL = "core_pollinterval"
        const val CAPABILITIES_SHARING_API_ENABLED = "sharing_api_enabled"
        const val CAPABILITIES_SHARING_PUBLIC_ENABLED = "sharing_public_enabled"
        const val CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED = "sharing_public_password_enforced"
        const val CAPABILITIES_SHARING_PUBLIC_ASK_FOR_OPTIONAL_PASSWORD = "sharing_public_ask_for_optional_password"
        const val CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED = "sharing_public_expire_date_enabled"
        const val CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS = "sharing_public_expire_date_days"
        const val CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED = "sharing_public_expire_date_enforced"
        const val CAPABILITIES_SHARING_PUBLIC_SEND_MAIL = "sharing_public_send_mail"
        const val CAPABILITIES_SHARING_PUBLIC_UPLOAD = "sharing_public_upload"
        const val CAPABILITIES_SHARING_USER_SEND_MAIL = "sharing_user_send_mail"
        const val CAPABILITIES_SHARING_RESHARING = "sharing_resharing"
        const val CAPABILITIES_SHARING_FEDERATION_OUTGOING = "sharing_federation_outgoing"
        const val CAPABILITIES_SHARING_FEDERATION_INCOMING = "sharing_federation_incoming"
        const val CAPABILITIES_FILES_BIGFILECHUNKING = "files_bigfilechunking"
        const val CAPABILITIES_FILES_UNDELETE = "files_undelete"
        const val CAPABILITIES_FILES_VERSIONING = "files_versioning"
        const val CAPABILITIES_EXTERNAL_LINKS = "external_links"
        const val CAPABILITIES_SERVER_NAME = "server_name"
        const val CAPABILITIES_SERVER_COLOR = "server_color"
        const val CAPABILITIES_SERVER_TEXT_COLOR = "server_text_color"
        const val CAPABILITIES_SERVER_ELEMENT_COLOR = "server_element_color"
        const val CAPABILITIES_SERVER_BACKGROUND_URL = "background_url"
        const val CAPABILITIES_SERVER_SLOGAN = "server_slogan"
        const val CAPABILITIES_SERVER_BACKGROUND_DEFAULT = "background_default"
        const val CAPABILITIES_SERVER_BACKGROUND_PLAIN = "background_plain"
        const val CAPABILITIES_END_TO_END_ENCRYPTION = "end_to_end_encryption"
        const val CAPABILITIES_ACTIVITY = "activity"
        const val CAPABILITIES_RICHDOCUMENT = "richdocument"
        const val CAPABILITIES_RICHDOCUMENT_MIMETYPE_LIST = "richdocument_mimetype_list"
        const val CAPABILITIES_RICHDOCUMENT_OPTIONAL_MIMETYPE_LIST = "richdocument_optional_mimetype_list"
        const val CAPABILITIES_RICHDOCUMENT_DIRECT_EDITING = "richdocument_direct_editing"
        const val CAPABILITIES_RICHDOCUMENT_TEMPLATES = "richdocument_direct_templates"
        const val CAPABILITIES_RICHDOCUMENT_PRODUCT_NAME = "richdocument_product_name"
        const val CAPABILITIES_DEFAULT_SORT_ORDER = (
            CAPABILITIES_ACCOUNT_NAME +
                " collate nocase asc"
            )
        const val CAPABILITIES_DIRECT_EDITING_ETAG = "direct_editing_etag"
        const val CAPABILITIES_ETAG = "etag"
        const val CAPABILITIES_USER_STATUS = "user_status"
        const val CAPABILITIES_USER_STATUS_SUPPORTS_EMOJI = "user_status_supports_emoji"

        // Columns of Uploads table
        const val UPLOADS_LOCAL_PATH = "local_path"
        const val UPLOADS_REMOTE_PATH = "remote_path"
        const val UPLOADS_ACCOUNT_NAME = "account_name"
        const val UPLOADS_FILE_SIZE = "file_size"
        const val UPLOADS_STATUS = "status"
        const val UPLOADS_LOCAL_BEHAVIOUR = "local_behaviour"
        const val UPLOADS_UPLOAD_TIME = "upload_time"
        const val UPLOADS_NAME_COLLISION_POLICY = "name_collision_policy"
        const val UPLOADS_IS_CREATE_REMOTE_FOLDER = "is_create_remote_folder"
        const val UPLOADS_UPLOAD_END_TIMESTAMP = "upload_end_timestamp"
        const val UPLOADS_LAST_RESULT = "last_result"
        const val UPLOADS_CREATED_BY = "created_by"
        const val UPLOADS_DEFAULT_SORT_ORDER = BaseColumns._ID + " collate nocase desc"
        const val UPLOADS_IS_WHILE_CHARGING_ONLY = "is_while_charging_only"
        const val UPLOADS_IS_WIFI_ONLY = "is_wifi_only"
        const val UPLOADS_FOLDER_UNLOCK_TOKEN = "folder_unlock_token"

        // Columns of synced folder table
        const val SYNCED_FOLDER_LOCAL_PATH = "local_path"
        const val SYNCED_FOLDER_REMOTE_PATH = "remote_path"
        const val SYNCED_FOLDER_WIFI_ONLY = "wifi_only"
        const val SYNCED_FOLDER_CHARGING_ONLY = "charging_only"
        const val SYNCED_FOLDER_EXISTING = "existing"
        const val SYNCED_FOLDER_ENABLED = "enabled"
        const val SYNCED_FOLDER_ENABLED_TIMESTAMP_MS = "enabled_timestamp_ms"
        const val SYNCED_FOLDER_TYPE = "type"
        const val SYNCED_FOLDER_SUBFOLDER_BY_DATE = "subfolder_by_date"
        const val SYNCED_FOLDER_ACCOUNT = "account"
        const val SYNCED_FOLDER_UPLOAD_ACTION = "upload_option"
        const val SYNCED_FOLDER_NAME_COLLISION_POLICY = "name_collision_policy"
        const val SYNCED_FOLDER_HIDDEN = "hidden"

        // Columns of external links table
        const val EXTERNAL_LINKS_ICON_URL = "icon_url"
        const val EXTERNAL_LINKS_LANGUAGE = "language"
        const val EXTERNAL_LINKS_TYPE = "type"
        const val EXTERNAL_LINKS_NAME = "name"
        const val EXTERNAL_LINKS_URL = "url"
        const val EXTERNAL_LINKS_REDIRECT = "redirect"

        // Columns of arbitrary data table
        const val ARBITRARY_DATA_CLOUD_ID = "cloud_id"
        const val ARBITRARY_DATA_KEY = "key"
        const val ARBITRARY_DATA_VALUE = "value"

        // Columns of virtual
        const val VIRTUAL_TYPE = "type"
        const val VIRTUAL_OCFILE_ID = "ocfile_id"

        // Columns of filesystem data table
        const val FILESYSTEM_FILE_LOCAL_PATH = "local_path"
        const val FILESYSTEM_FILE_MODIFIED = "modified_at"
        const val FILESYSTEM_FILE_IS_FOLDER = "is_folder"
        const val FILESYSTEM_FILE_FOUND_RECENTLY = "found_at"
        const val FILESYSTEM_FILE_SENT_FOR_UPLOAD = "upload_triggered"
        const val FILESYSTEM_SYNCED_FOLDER_ID = "syncedfolder_id"
        const val FILESYSTEM_CRC32 = "crc32"
    }
}
