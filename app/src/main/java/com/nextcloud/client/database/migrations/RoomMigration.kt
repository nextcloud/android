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

package com.nextcloud.client.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nextcloud.client.database.NextcloudDatabase
import com.nextcloud.client.database.migrations.DatabaseMigrationUtil.TYPE_INTEGER
import com.nextcloud.client.database.migrations.DatabaseMigrationUtil.TYPE_INTEGER_PRIMARY_KEY
import com.nextcloud.client.database.migrations.DatabaseMigrationUtil.TYPE_TEXT

class RoomMigration : Migration(NextcloudDatabase.FIRST_ROOM_DB_VERSION - 1, NextcloudDatabase.FIRST_ROOM_DB_VERSION) {

    override fun migrate(database: SupportSQLiteDatabase) {
        migrateFilesystemTable(database)
        migrateUploadsTable(database)
        migrateCapabilitiesTable(database)
        migrateFilesTable(database)
    }

    /**
     * filesystem table: STRING converted to TEXT
     */
    private fun migrateFilesystemTable(database: SupportSQLiteDatabase) {
        val newColumns = mapOf(
            "_id" to TYPE_INTEGER_PRIMARY_KEY,
            "local_path" to TYPE_TEXT,
            "is_folder" to TYPE_INTEGER,
            "found_at" to TYPE_INTEGER,
            "upload_triggered" to TYPE_INTEGER,
            "syncedfolder_id" to TYPE_TEXT,
            "crc32" to TYPE_TEXT,
            "modified_at" to TYPE_INTEGER
        )

        DatabaseMigrationUtil.migrateTable(database, "filesystem", newColumns)
    }

    /**
     * uploads table: LONG converted to INTEGER
     */
    private fun migrateUploadsTable(database: SupportSQLiteDatabase) {
        val newColumns = mapOf(
            "_id" to TYPE_INTEGER_PRIMARY_KEY,
            "local_path" to TYPE_TEXT,
            "remote_path" to TYPE_TEXT,
            "account_name" to TYPE_TEXT,
            "file_size" to TYPE_INTEGER,
            "status" to TYPE_INTEGER,
            "local_behaviour" to TYPE_INTEGER,
            "upload_time" to TYPE_INTEGER,
            "name_collision_policy" to TYPE_INTEGER,
            "is_create_remote_folder" to TYPE_INTEGER,
            "upload_end_timestamp" to TYPE_INTEGER,
            "last_result" to TYPE_INTEGER,
            "is_while_charging_only" to TYPE_INTEGER,
            "is_wifi_only" to TYPE_INTEGER,
            "created_by" to TYPE_INTEGER,
            "folder_unlock_token" to TYPE_TEXT
        )

        DatabaseMigrationUtil.migrateTable(database, "list_of_uploads", newColumns)
    }

    /**
     * capabilities table: "files_drop" column removed
     */
    private fun migrateCapabilitiesTable(database: SupportSQLiteDatabase) {
        val newColumns = mapOf(
            "_id" to TYPE_INTEGER_PRIMARY_KEY,
            "account" to TYPE_TEXT,
            "version_mayor" to TYPE_INTEGER,
            "version_minor" to TYPE_INTEGER,
            "version_micro" to TYPE_INTEGER,
            "version_string" to TYPE_TEXT,
            "version_edition" to TYPE_TEXT,
            "extended_support" to TYPE_INTEGER,
            "core_pollinterval" to TYPE_INTEGER,
            "sharing_api_enabled" to TYPE_INTEGER,
            "sharing_public_enabled" to TYPE_INTEGER,
            "sharing_public_password_enforced" to TYPE_INTEGER,
            "sharing_public_expire_date_enabled" to TYPE_INTEGER,
            "sharing_public_expire_date_days" to TYPE_INTEGER,
            "sharing_public_expire_date_enforced" to TYPE_INTEGER,
            "sharing_public_send_mail" to TYPE_INTEGER,
            "sharing_public_upload" to TYPE_INTEGER,
            "sharing_user_send_mail" to TYPE_INTEGER,
            "sharing_resharing" to TYPE_INTEGER,
            "sharing_federation_outgoing" to TYPE_INTEGER,
            "sharing_federation_incoming" to TYPE_INTEGER,
            "files_bigfilechunking" to TYPE_INTEGER,
            "files_undelete" to TYPE_INTEGER,
            "files_versioning" to TYPE_INTEGER,
            "external_links" to TYPE_INTEGER,
            "server_name" to TYPE_TEXT,
            "server_color" to TYPE_TEXT,
            "server_text_color" to TYPE_TEXT,
            "server_element_color" to TYPE_TEXT,
            "server_slogan" to TYPE_TEXT,
            "server_logo" to TYPE_TEXT,
            "background_url" to TYPE_TEXT,
            "end_to_end_encryption" to TYPE_INTEGER,
            "activity" to TYPE_INTEGER,
            "background_default" to TYPE_INTEGER,
            "background_plain" to TYPE_INTEGER,
            "richdocument" to TYPE_INTEGER,
            "richdocument_mimetype_list" to TYPE_TEXT,
            "richdocument_direct_editing" to TYPE_INTEGER,
            "richdocument_direct_templates" to TYPE_INTEGER,
            "richdocument_optional_mimetype_list" to TYPE_TEXT,
            "sharing_public_ask_for_optional_password" to TYPE_INTEGER,
            "richdocument_product_name" to TYPE_TEXT,
            "direct_editing_etag" to TYPE_TEXT,
            "user_status" to TYPE_INTEGER,
            "user_status_supports_emoji" to TYPE_INTEGER,
            "etag" to TYPE_TEXT,
            "files_locking_version" to TYPE_TEXT
        )

        DatabaseMigrationUtil.migrateTable(database, "capabilities", newColumns)
    }

    /**
     * files table: "public_link" column removed
     */
    private fun migrateFilesTable(database: SupportSQLiteDatabase) {
        val newColumns = mapOf(
            "_id" to TYPE_INTEGER_PRIMARY_KEY,
            "filename" to TYPE_TEXT,
            "encrypted_filename" to TYPE_TEXT,
            "path" to TYPE_TEXT,
            "path_decrypted" to TYPE_TEXT,
            "parent" to TYPE_INTEGER,
            "created" to TYPE_INTEGER,
            "modified" to TYPE_INTEGER,
            "content_type" to TYPE_TEXT,
            "content_length" to TYPE_INTEGER,
            "media_path" to TYPE_TEXT,
            "file_owner" to TYPE_TEXT,
            "last_sync_date" to TYPE_INTEGER,
            "last_sync_date_for_data" to TYPE_INTEGER,
            "modified_at_last_sync_for_data" to TYPE_INTEGER,
            "etag" to TYPE_TEXT,
            "etag_on_server" to TYPE_TEXT,
            "share_by_link" to TYPE_INTEGER,
            "permissions" to TYPE_TEXT,
            "remote_id" to TYPE_TEXT,
            "update_thumbnail" to TYPE_INTEGER,
            "is_downloading" to TYPE_INTEGER,
            "favorite" to TYPE_INTEGER,
            "is_encrypted" to TYPE_INTEGER,
            "etag_in_conflict" to TYPE_TEXT,
            "shared_via_users" to TYPE_INTEGER,
            "mount_type" to TYPE_INTEGER,
            "has_preview" to TYPE_INTEGER,
            "unread_comments_count" to TYPE_INTEGER,
            "owner_id" to TYPE_TEXT,
            "owner_display_name" to TYPE_TEXT,
            "note" to TYPE_TEXT,
            "sharees" to TYPE_TEXT,
            "rich_workspace" to TYPE_TEXT,
            "metadata_size" to TYPE_TEXT,
            "locked" to TYPE_INTEGER,
            "lock_type" to TYPE_INTEGER,
            "lock_owner" to TYPE_TEXT,
            "lock_owner_display_name" to TYPE_TEXT,
            "lock_owner_editor" to TYPE_TEXT,
            "lock_timestamp" to TYPE_INTEGER,
            "lock_timeout" to TYPE_INTEGER,
            "lock_token" to TYPE_TEXT
        )
        DatabaseMigrationUtil.migrateTable(database, "filelist", newColumns)
    }
}
