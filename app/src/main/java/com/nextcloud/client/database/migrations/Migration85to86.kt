/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Suppress("MagicNumber", "NestedBlockDepth")
val Migration85to86 = object : Migration(85, 86) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE offline_operations ADD COLUMN offline_operations_modified_at INTEGER")
    }
}
