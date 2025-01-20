/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_85_to_86 = object : Migration(85, 86) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE capabilities ADD COLUMN recommendation INTEGER DEFAULT NULL")
    }
}
