/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nextcloud.client.database.migrations.model.SQLiteColumnType
import com.owncloud.android.db.ProviderMeta

@Suppress("MagicNumber")
val MIGRATION_97_98 = object : Migration(97, 98) {
    override fun migrate(db: SupportSQLiteDatabase) {
        DatabaseMigrationUtil.addColumnIfNotExists(
            db,
            ProviderMeta.ProviderTableMeta.UPLOADS_TABLE_NAME,
            ProviderMeta.ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP_LONG,
            SQLiteColumnType.INTEGER_DEFAULT_NULL
        )

        DatabaseMigrationUtil.addColumnIfNotExists(
            db,
            ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME,
            ProviderMeta.ProviderTableMeta.CAPABILITIES_CLIENT_INTEGRATION_JSON,
            SQLiteColumnType.TEXT_DEFAULT_NULL
        )

        DatabaseMigrationUtil.resetCapabilities(db)
    }
}
