/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nextcloud.client.database.migrations.model.SQLiteColumnType
import com.owncloud.android.db.ProviderMeta

@Suppress("MagicNumber")
val MIGRATION_100_101 = object : Migration(100, 101) {
    override fun migrate(db: SupportSQLiteDatabase) {
        DatabaseMigrationUtil.addColumnIfNotExists(
            db,
            ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME,
            ProviderMeta.ProviderTableMeta.CAPABILITIES_MOD_REWRITE_WORKING,
            SQLiteColumnType.INTEGER_DEFAULT_NULL
        )

        DatabaseMigrationUtil.resetCapabilities(db)
    }
}
