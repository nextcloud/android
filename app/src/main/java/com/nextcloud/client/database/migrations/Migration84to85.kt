/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.owncloud.android.db.ProviderMeta

@Suppress("MagicNumber", "NestedBlockDepth")
val Migration84to85 = object : Migration(84, 85) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.run {
            execSQL(
                """
            CREATE TABLE IF NOT EXISTS `offline_operations_new` (
                `_id` INTEGER PRIMARY KEY AUTOINCREMENT,
                `offline_operations_parent_oc_file_id` INTEGER,
                `offline_operations_path` TEXT,
                `offline_operations_type` TEXT,
                `offline_operations_file_name` TEXT,
                `offline_operations_created_at` INTEGER
            )
        """
            )

            execSQL(
                """
            INSERT INTO offline_operations_new 
            SELECT _id, offline_operations_parent_oc_file_id, offline_operations_path, 
                   offline_operations_type, offline_operations_file_name, offline_operations_created_at 
            FROM ${ProviderMeta.ProviderTableMeta.OFFLINE_OPERATION_TABLE_NAME}
        """
            )

            execSQL("DROP TABLE ${ProviderMeta.ProviderTableMeta.OFFLINE_OPERATION_TABLE_NAME}")
            execSQL(
                "ALTER TABLE offline_operations_new RENAME TO " +
                    ProviderMeta.ProviderTableMeta.OFFLINE_OPERATION_TABLE_NAME
            )
        }
    }
}
