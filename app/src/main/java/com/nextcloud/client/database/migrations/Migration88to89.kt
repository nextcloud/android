/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nextcloud.client.database.migrations.model.SQLiteColumnType
import com.nextcloud.utils.extensions.addColumnIfNotExists
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta

val MIGRATION_88_89 = object : Migration(88, 89) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.addColumnIfNotExists(
            ProviderTableMeta.FILE_TABLE_NAME,
            ProviderTableMeta.FILE_UPLOADED,
            SQLiteColumnType.INTEGER_DEFAULT_NULL
        )
        database.addColumnIfNotExists(
            ProviderTableMeta.CAPABILITIES_TABLE_NAME,
            ProviderTableMeta.CAPABILITIES_NOTES_FOLDER_PATH,
            SQLiteColumnType.TEXT_DEFAULT_NULL
        )
    }
}
