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
    private val clock: Clock
) : Migration(from, to) {

    override fun migrate(database: SupportSQLiteDatabase) {
        LegacyMigrationHelper(clock)
            .onUpgrade(database, from, to)
    }
}

/**
 * Adds a legacy migration for all versions before Room was introduced
 *
 * This is needed because the [Migration] does not know which versions it's dealing with
 */
fun RoomDatabase.Builder<NextcloudDatabase>.addLegacyMigrations(
    clock: Clock
): RoomDatabase.Builder<NextcloudDatabase> {
    (MIN_SUPPORTED_DB_VERSION until NextcloudDatabase.FIRST_ROOM_DB_VERSION - 1)
        .map { from -> LegacyMigration(from, from + 1, clock) }
        .forEach { migration -> this.addMigrations(migration) }
    return this
}
