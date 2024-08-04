/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.database.migrations

import android.content.Context
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nextcloud.client.core.Clock
import com.nextcloud.client.database.NextcloudDatabase

private const val MIN_SUPPORTED_DB_VERSION = 24

/**
 * Migrations for DB versions before Room was introduced
 */
class LegacyMigration(
    private val from: Int,
    private val to: Int,
    private val clock: Clock,
    private val context: Context
) : Migration(from, to) {

    override fun migrate(database: SupportSQLiteDatabase) {
        LegacyMigrationHelper(clock, context)
            .tryUpgrade(database, from, to)
    }
}

/**
 * Adds a legacy migration for all versions before Room was introduced
 *
 * This is needed because the [Migration] does not know which versions it's dealing with
 */
@Suppress("ForEachOnRange")
fun RoomDatabase.Builder<NextcloudDatabase>.addLegacyMigrations(
    clock: Clock,
    context: Context
): RoomDatabase.Builder<NextcloudDatabase> {
    (MIN_SUPPORTED_DB_VERSION until NextcloudDatabase.FIRST_ROOM_DB_VERSION - 1)
        .map { from -> LegacyMigration(from, from + 1, clock, context) }
        .forEach { migration -> this.addMigrations(migration) }
    return this
}
