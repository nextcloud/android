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
package com.owncloud.android.db;

import android.net.Uri;
import android.provider.BaseColumns;

import com.owncloud.android.MainApp;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Meta-Class that holds various static field information
 */
public class ProviderMeta {
    public static final String DB_NAME = "filelist";
    public static final int DB_VERSION = 74;

    private ProviderMeta() {
        // No instance
    }

    static public class ProviderTableMeta implements BaseColumns {
        public static final String FILE_TABLE_NAME = "filelist";
        public static final String OCSHARES_TABLE_NAME = "ocshares";
        public static final String CAPABILITIES_TABLE_NAME = "capabilities";
        public static final String UPLOADS_TABLE_NAME = "list_of_uploads";
        public static final String SYNCED_FOLDERS_TABLE_NAME = "synced_folders";
        public static final String EXTERNAL_LINKS_TABLE_NAME = "external_links";
        public static final String ARBITRARY_DATA_TABLE_NAME = "arbitrary_data";
        public static final String VIRTUAL_TABLE_NAME = "virtual";
        public static final String FILESYSTEM_TABLE_NAME = "filesystem";
        public static final String EDITORS_TABLE_NAME = "editors";
        public static final String CREATORS_TABLE_NAME = "creators";

        private static final String CONTENT_PREFIX = "content://";

        public static final Uri CONTENT_URI = Uri.parse(CONTENT_PREFIX
                + MainApp.getAuthority() + "/");
        public static final Uri CONTENT_URI_FILE = Uri.parse(CONTENT_PREFIX
                + MainApp.getAuthority() + "/file");
        public static final Uri CONTENT_URI_DIR = Uri.parse(CONTENT_PREFIX
                + MainApp.getAuthority() + "/dir");
        public static final Uri CONTENT_URI_SHARE = Uri.parse(CONTENT_PREFIX
                + MainApp.getAuthority() + "/shares");
        public static final Uri CONTENT_URI_CAPABILITIES = Uri.parse(CONTENT_PREFIX
                + MainApp.getAuthority() + "/capabilities");
        public static final Uri CONTENT_URI_UPLOADS = Uri.parse(CONTENT_PREFIX
                + MainApp.getAuthority() + "/uploads");
        public static final Uri CONTENT_URI_SYNCED_FOLDERS = Uri.parse(CONTENT_PREFIX
                + MainApp.getAuthority() + "/synced_folders");
        public static final Uri CONTENT_URI_EXTERNAL_LINKS = Uri.parse(CONTENT_PREFIX
                + MainApp.getAuthority() + "/external_links");
        public static final Uri CONTENT_URI_VIRTUAL = Uri.parse(CONTENT_PREFIX + MainApp.getAuthority() + "/virtual");
        public static final Uri CONTENT_URI_FILESYSTEM = Uri.parse(CONTENT_PREFIX
                + MainApp.getAuthority() + "/filesystem");


        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.owncloud.file";
        public static final String CONTENT_TYPE_ITEM = "vnd.android.cursor.item/vnd.owncloud.file";

        // Columns of filelist table
        public static final String FILE_PARENT = "parent";
        public static final String FILE_NAME = "filename";
        public static final String FILE_ENCRYPTED_NAME = "encrypted_filename";
        public static final String FILE_CREATION = "created";
        public static final String FILE_MODIFIED = "modified";
        public static final String FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA = "modified_at_last_sync_for_data";
        public static final String FILE_CONTENT_LENGTH = "content_length";
        public static final String FILE_CONTENT_TYPE = "content_type";
        public static final String FILE_STORAGE_PATH = "media_path";
        public static final String FILE_PATH = "path";
        public static final String FILE_PATH_DECRYPTED = "path_decrypted";
        public static final String FILE_ACCOUNT_OWNER = "file_owner";
        public static final String FILE_LAST_SYNC_DATE = "last_sync_date";// _for_properties, but let's keep it as it is
        public static final String FILE_LAST_SYNC_DATE_FOR_DATA = "last_sync_date_for_data";
        public static final String FILE_KEEP_IN_SYNC = "keep_in_sync";
        public static final String FILE_ETAG = "etag";
        public static final String FILE_ETAG_ON_SERVER = "etag_on_server";
        public static final String FILE_SHARED_VIA_LINK = "share_by_link";
        public static final String FILE_SHARED_WITH_SHAREE = "shared_via_users";
        public static final String FILE_PERMISSIONS = "permissions";
        public static final String FILE_LOCAL_ID = "local_id";
        public static final String FILE_REMOTE_ID = "remote_id";
        public static final String FILE_UPDATE_THUMBNAIL = "update_thumbnail";
        public static final String FILE_IS_DOWNLOADING = "is_downloading";
        public static final String FILE_ETAG_IN_CONFLICT = "etag_in_conflict";
        public static final String FILE_FAVORITE = "favorite";
        public static final String FILE_IS_ENCRYPTED = "is_encrypted";
        public static final String FILE_MOUNT_TYPE = "mount_type";
        public static final String FILE_HAS_PREVIEW = "has_preview";
        public static final String FILE_UNREAD_COMMENTS_COUNT = "unread_comments_count";
        public static final String FILE_OWNER_ID = "owner_id";
        public static final String FILE_OWNER_DISPLAY_NAME = "owner_display_name";
        public static final String FILE_NOTE = "note";
        public static final String FILE_SHAREES = "sharees";
        public static final String FILE_RICH_WORKSPACE = "rich_workspace";
        public static final String FILE_METADATA_SIZE = "metadata_size";
        public static final String FILE_METADATA_GPS = "metadata_gps";
        public static final String FILE_LOCKED = "locked";
        public static final String FILE_LOCK_TYPE = "lock_type";
        public static final String FILE_LOCK_OWNER = "lock_owner";
        public static final String FILE_LOCK_OWNER_DISPLAY_NAME = "lock_owner_display_name";
        public static final String FILE_LOCK_OWNER_EDITOR = "lock_owner_editor";
        public static final String FILE_LOCK_TIMESTAMP = "lock_timestamp";
        public static final String FILE_LOCK_TIMEOUT = "lock_timeout";
        public static final String FILE_LOCK_TOKEN = "lock_token";
        public static final String FILE_TAGS = "tags";

        public static final List<String> FILE_ALL_COLUMNS = Collections.unmodifiableList(Arrays.asList(
                _ID,
                FILE_PARENT,
                FILE_NAME,
                FILE_ENCRYPTED_NAME,
                FILE_CREATION,
                FILE_MODIFIED,
                FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA,
                FILE_CONTENT_LENGTH,
                FILE_CONTENT_TYPE,
                FILE_STORAGE_PATH,
                FILE_PATH,
                FILE_PATH_DECRYPTED,
                FILE_ACCOUNT_OWNER,
                FILE_LAST_SYNC_DATE,
                FILE_LAST_SYNC_DATE_FOR_DATA,
                FILE_KEEP_IN_SYNC,
                FILE_ETAG,
                FILE_ETAG_ON_SERVER,
                FILE_SHARED_VIA_LINK,
                FILE_SHARED_WITH_SHAREE,
                FILE_PERMISSIONS,
                FILE_REMOTE_ID,
                FILE_LOCAL_ID,
                FILE_UPDATE_THUMBNAIL,
                FILE_IS_DOWNLOADING,
                FILE_ETAG_IN_CONFLICT,
                FILE_FAVORITE,
                FILE_IS_ENCRYPTED,
                FILE_MOUNT_TYPE,
                FILE_HAS_PREVIEW,
                FILE_UNREAD_COMMENTS_COUNT,
                FILE_OWNER_ID,
                FILE_OWNER_DISPLAY_NAME,
                FILE_NOTE,
                FILE_SHAREES,
                FILE_RICH_WORKSPACE,
                FILE_LOCKED,
                FILE_LOCK_TYPE,
                FILE_LOCK_OWNER,
                FILE_LOCK_OWNER_DISPLAY_NAME,
                FILE_LOCK_OWNER_EDITOR,
                FILE_LOCK_TIMESTAMP,
                FILE_LOCK_TIMEOUT,
                FILE_LOCK_TOKEN,
                FILE_METADATA_SIZE,
                FILE_TAGS,
                FILE_METADATA_GPS));
        public static final String FILE_DEFAULT_SORT_ORDER = FILE_NAME + " collate nocase asc";

        // Columns of ocshares table
        public static final String OCSHARES_FILE_SOURCE = "file_source";
        public static final String OCSHARES_ITEM_SOURCE = "item_source";
        public static final String OCSHARES_SHARE_TYPE = "share_type";
        public static final String OCSHARES_SHARE_WITH = "shate_with";
        public static final String OCSHARES_PATH = "path";
        public static final String OCSHARES_PERMISSIONS = "permissions";
        public static final String OCSHARES_SHARED_DATE = "shared_date";
        public static final String OCSHARES_EXPIRATION_DATE = "expiration_date";
        public static final String OCSHARES_TOKEN = "token";
        public static final String OCSHARES_SHARE_WITH_DISPLAY_NAME = "shared_with_display_name";
        public static final String OCSHARES_IS_DIRECTORY = "is_directory";
        public static final String OCSHARES_USER_ID = "user_id";
        public static final String OCSHARES_ID_REMOTE_SHARED = "id_remote_shared";
        public static final String OCSHARES_ACCOUNT_OWNER = "owner_share";
        public static final String OCSHARES_IS_PASSWORD_PROTECTED = "is_password_protected";
        public static final String OCSHARES_NOTE = "note";
        public static final String OCSHARES_HIDE_DOWNLOAD = "hide_download";
        public static final String OCSHARES_SHARE_LINK = "share_link";
        public static final String OCSHARES_SHARE_LABEL = "share_label";

        public static final String OCSHARES_DEFAULT_SORT_ORDER = OCSHARES_FILE_SOURCE
                + " collate nocase asc";

        // Columns of capabilities table
        public static final String CAPABILITIES_ACCOUNT_NAME = "account";
        public static final String CAPABILITIES_VERSION_MAYOR = "version_mayor";
        public static final String CAPABILITIES_VERSION_MINOR = "version_minor";
        public static final String CAPABILITIES_VERSION_MICRO = "version_micro";
        public static final String CAPABILITIES_VERSION_STRING = "version_string";
        public static final String CAPABILITIES_VERSION_EDITION = "version_edition";
        public static final String CAPABILITIES_EXTENDED_SUPPORT = "extended_support";
        public static final String CAPABILITIES_CORE_POLLINTERVAL = "core_pollinterval";
        public static final String CAPABILITIES_SHARING_API_ENABLED = "sharing_api_enabled";
        public static final String CAPABILITIES_SHARING_PUBLIC_ENABLED = "sharing_public_enabled";
        public static final String CAPABILITIES_SHARING_PUBLIC_PASSWORD_ENFORCED = "sharing_public_password_enforced";
        public static final String CAPABILITIES_SHARING_PUBLIC_ASK_FOR_OPTIONAL_PASSWORD =
            "sharing_public_ask_for_optional_password";
        public static final String CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENABLED =
                "sharing_public_expire_date_enabled";
        public static final String CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_DAYS =
                "sharing_public_expire_date_days";
        public static final String CAPABILITIES_SHARING_PUBLIC_EXPIRE_DATE_ENFORCED =
                "sharing_public_expire_date_enforced";
        public static final String CAPABILITIES_SHARING_PUBLIC_SEND_MAIL = "sharing_public_send_mail";
        public static final String CAPABILITIES_SHARING_PUBLIC_UPLOAD = "sharing_public_upload";
        public static final String CAPABILITIES_SHARING_USER_SEND_MAIL = "sharing_user_send_mail";
        public static final String CAPABILITIES_SHARING_RESHARING = "sharing_resharing";
        public static final String CAPABILITIES_SHARING_FEDERATION_OUTGOING = "sharing_federation_outgoing";
        public static final String CAPABILITIES_SHARING_FEDERATION_INCOMING = "sharing_federation_incoming";
        public static final String CAPABILITIES_FILES_BIGFILECHUNKING = "files_bigfilechunking";
        public static final String CAPABILITIES_FILES_UNDELETE = "files_undelete";
        public static final String CAPABILITIES_FILES_VERSIONING = "files_versioning";
        public static final String CAPABILITIES_FILES_LOCKING_VERSION = "files_locking_version";
        public static final String CAPABILITIES_EXTERNAL_LINKS = "external_links";
        public static final String CAPABILITIES_SERVER_NAME = "server_name";
        public static final String CAPABILITIES_SERVER_COLOR = "server_color";
        public static final String CAPABILITIES_SERVER_TEXT_COLOR = "server_text_color";
        public static final String CAPABILITIES_SERVER_ELEMENT_COLOR = "server_element_color";
        public static final String CAPABILITIES_SERVER_BACKGROUND_URL = "background_url";
        public static final String CAPABILITIES_SERVER_SLOGAN = "server_slogan";
        public static final String CAPABILITIES_SERVER_LOGO = "server_logo";
        public static final String CAPABILITIES_SERVER_BACKGROUND_DEFAULT = "background_default";
        public static final String CAPABILITIES_SERVER_BACKGROUND_PLAIN = "background_plain";
        public static final String CAPABILITIES_END_TO_END_ENCRYPTION = "end_to_end_encryption";
        public static final String CAPABILITIES_END_TO_END_ENCRYPTION_KEYS_EXIST = "end_to_end_encryption_keys_exist";
        public static final String CAPABILITIES_ACTIVITY = "activity";
        public static final String CAPABILITIES_RICHDOCUMENT = "richdocument";
        public static final String CAPABILITIES_RICHDOCUMENT_MIMETYPE_LIST = "richdocument_mimetype_list";
        public static final String CAPABILITIES_RICHDOCUMENT_OPTIONAL_MIMETYPE_LIST =
            "richdocument_optional_mimetype_list";
        public static final String CAPABILITIES_RICHDOCUMENT_DIRECT_EDITING = "richdocument_direct_editing";
        public static final String CAPABILITIES_RICHDOCUMENT_TEMPLATES = "richdocument_direct_templates";
        public static final String CAPABILITIES_RICHDOCUMENT_PRODUCT_NAME = "richdocument_product_name";
        public static final String CAPABILITIES_DEFAULT_SORT_ORDER = CAPABILITIES_ACCOUNT_NAME
            + " collate nocase asc";
        public static final String CAPABILITIES_DIRECT_EDITING_ETAG = "direct_editing_etag";
        public static final String CAPABILITIES_ETAG = "etag";
        public static final String CAPABILITIES_USER_STATUS = "user_status";
        public static final String CAPABILITIES_USER_STATUS_SUPPORTS_EMOJI = "user_status_supports_emoji";
        public static final String CAPABILITIES_GROUPFOLDERS = "groupfolders";
        public static final String CAPABILITIES_DROP_ACCOUNT = "drop_account";

        //Columns of Uploads table
        public static final String UPLOADS_LOCAL_PATH = "local_path";
        public static final String UPLOADS_REMOTE_PATH = "remote_path";
        public static final String UPLOADS_ACCOUNT_NAME = "account_name";
        public static final String UPLOADS_FILE_SIZE = "file_size";
        public static final String UPLOADS_STATUS = "status";
        public static final String UPLOADS_LOCAL_BEHAVIOUR = "local_behaviour";
        public static final String UPLOADS_UPLOAD_TIME = "upload_time";
        public static final String UPLOADS_NAME_COLLISION_POLICY = "name_collision_policy";
        public static final String UPLOADS_IS_CREATE_REMOTE_FOLDER = "is_create_remote_folder";
        public static final String UPLOADS_UPLOAD_END_TIMESTAMP = "upload_end_timestamp";
        public static final String UPLOADS_LAST_RESULT = "last_result";
        public static final String UPLOADS_CREATED_BY = "created_by";
        public static final String UPLOADS_DEFAULT_SORT_ORDER = ProviderTableMeta._ID + " collate nocase desc";
        public static final String UPLOADS_IS_WHILE_CHARGING_ONLY = "is_while_charging_only";
        public static final String UPLOADS_IS_WIFI_ONLY = "is_wifi_only";
        public static final String UPLOADS_FOLDER_UNLOCK_TOKEN = "folder_unlock_token";

        // Columns of synced folder table
        public static final String SYNCED_FOLDER_LOCAL_PATH = "local_path";
        public static final String SYNCED_FOLDER_REMOTE_PATH = "remote_path";
        public static final String SYNCED_FOLDER_WIFI_ONLY = "wifi_only";
        public static final String SYNCED_FOLDER_CHARGING_ONLY = "charging_only";
        public static final String SYNCED_FOLDER_EXISTING = "existing";
        public static final String SYNCED_FOLDER_ENABLED = "enabled";
        public static final String SYNCED_FOLDER_ENABLED_TIMESTAMP_MS = "enabled_timestamp_ms";
        public static final String SYNCED_FOLDER_TYPE = "type";
        public static final String SYNCED_FOLDER_SUBFOLDER_BY_DATE = "subfolder_by_date";
        public static final String SYNCED_FOLDER_ACCOUNT = "account";
        public static final String SYNCED_FOLDER_UPLOAD_ACTION = "upload_option";
        public static final String SYNCED_FOLDER_NAME_COLLISION_POLICY = "name_collision_policy";
        public static final String SYNCED_FOLDER_HIDDEN = "hidden";
        public static final String SYNCED_FOLDER_SUBFOLDER_RULE = "sub_folder_rule";

        // Columns of external links table
        public static final String EXTERNAL_LINKS_ICON_URL = "icon_url";
        public static final String EXTERNAL_LINKS_LANGUAGE = "language";
        public static final String EXTERNAL_LINKS_TYPE = "type";
        public static final String EXTERNAL_LINKS_NAME = "name";
        public static final String EXTERNAL_LINKS_URL = "url";
        public static final String EXTERNAL_LINKS_REDIRECT = "redirect";

        // Columns of arbitrary data table
        public static final String ARBITRARY_DATA_CLOUD_ID = "cloud_id";
        public static final String ARBITRARY_DATA_KEY = "key";
        public static final String ARBITRARY_DATA_VALUE = "value";


        // Columns of virtual
        public static final String VIRTUAL_TYPE = "type";
        public static final String VIRTUAL_OCFILE_ID = "ocfile_id";

        // Columns of filesystem data table
        public static final String FILESYSTEM_FILE_LOCAL_PATH = "local_path";
        public static final String FILESYSTEM_FILE_MODIFIED = "modified_at";
        public static final String FILESYSTEM_FILE_IS_FOLDER = "is_folder";
        public static final String FILESYSTEM_FILE_FOUND_RECENTLY = "found_at";
        public static final String FILESYSTEM_FILE_SENT_FOR_UPLOAD = "upload_triggered";
        public static final String FILESYSTEM_SYNCED_FOLDER_ID = "syncedfolder_id";
        public static final String FILESYSTEM_CRC32 = "crc32";

        private ProviderTableMeta() {
            // No instance
        }
    }
}
