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

class RoomMigration : Migration(NextcloudDatabase.FIRST_ROOM_DB_VERSION - 1, NextcloudDatabase.FIRST_ROOM_DB_VERSION) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // migrate LONG, STRING to INTEGER, TEXT
        migrateFilesystemTable(database)
        migrateUploadsTable(database)
    }

    private fun migrateFilesystemTable(database: SupportSQLiteDatabase) {
        val table = "filesystem"
        val newTable = "${table}_new"
        // create table with fixed types
        database.execSQL(
            "CREATE TABLE $newTable (" +
                "_id INTEGER PRIMARY KEY," +
                "local_path TEXT," +
                "is_folder INTEGER," +
                "found_at INTEGER," +
                "upload_triggered INTEGER," +
                "syncedfolder_id TEXT," +
                "crc32 TEXT," +
                "modified_at INTEGER " +
                ")"
        )
        // copy data
        val columns =
            "_id, local_path, is_folder, found_at, upload_triggered, syncedfolder_id, crc32, modified_at"
        database.execSQL(
            "INSERT INTO $newTable ($columns) " +
                "SELECT $columns FROM $table"
        )
        // replace table
        database.execSQL("DROP TABLE $table")
        database.execSQL("ALTER TABLE $newTable RENAME TO $table")
    }

    private fun migrateUploadsTable(database: SupportSQLiteDatabase) {
        val table = "list_of_uploads"
        val newTable = "${table}_new"
        // create table with fixed types
        database.execSQL(
            "CREATE TABLE $newTable (" +
                "_id INTEGER PRIMARY KEY," +
                "local_path TEXT, " +
                "remote_path TEXT, " +
                "account_name TEXT, " +
                "file_size INTEGER, " +
                "status INTEGER, " +
                "local_behaviour INTEGER, " +
                "upload_time INTEGER, " +
                "name_collision_policy INTEGER, " +
                "is_create_remote_folder INTEGER, " +
                "upload_end_timestamp INTEGER, " +
                "last_result INTEGER, " +
                "is_while_charging_only INTEGER, " +
                "is_wifi_only INTEGER, " +
                "created_by INTEGER, " +
                "folder_unlock_token TEXT " +
                ")"
        )

        // copy data
        val columns =
            "_id, local_path, remote_path, account_name, file_size, status, local_behaviour, upload_time," +
                " name_collision_policy, is_create_remote_folder, upload_end_timestamp, last_result," +
                " is_while_charging_only, is_wifi_only, created_by, folder_unlock_token"
        database.execSQL(
            "INSERT INTO $newTable ($columns) " +
                "SELECT $columns FROM $table"
        )
        // replace table
        database.execSQL("DROP TABLE $table")
        database.execSQL("ALTER TABLE $newTable RENAME TO $table")
    }
}
