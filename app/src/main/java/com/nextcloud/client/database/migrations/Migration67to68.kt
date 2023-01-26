/*
 * Nextcloud Android client application
 *
 *  @author Álvaro Brey
 *  Copyright (C) 2023 Álvaro Brey
 *  Copyright (C) 2023 Nextcloud GmbH
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
import com.nextcloud.client.database.migrations.DatabaseMigrationUtil.KEYWORD_NOT_NULL
import com.nextcloud.client.database.migrations.DatabaseMigrationUtil.TYPE_INTEGER
import com.nextcloud.client.database.migrations.DatabaseMigrationUtil.TYPE_TEXT

/**
 * Migration from version 67 to 68.
 *
 * This migration makes the local_id column NOT NULL, with -1 as a default value.
 */
@Suppress("MagicNumber")
class Migration67to68 : Migration(67, 68) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val tableName = "filelist"
        val newTableTempName = "${tableName}_new"
        val newColumns = mapOf(
            "_id" to DatabaseMigrationUtil.TYPE_INTEGER_PRIMARY_KEY,
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
            "local_id" to "$TYPE_INTEGER $KEYWORD_NOT_NULL DEFAULT -1",
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

        DatabaseMigrationUtil.migrateTable(database, "filelist", newColumns) { columnName ->
            when (columnName) {
                "local_id" -> "IFNULL(local_id, -1)"
                else -> columnName
            }
        }
    }
}
