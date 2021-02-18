/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.migrations

import android.content.SharedPreferences
import java.util.TreeSet

class MigrationsDb(private val migrationsDb: SharedPreferences) {

    companion object {
        const val DB_KEY_LAST_MIGRATED_VERSION = "last_migrated_version"
        const val DB_KEY_APPLIED_MIGRATIONS = "applied_migrations"
        const val DB_KEY_FAILED = "failed"
        const val DB_KEY_FAILED_MIGRATION_ID = "failed_migration_id"
        const val DB_KEY_FAILED_MIGRATION_ERROR_MESSAGE = "failed_migration_error"

        const val NO_LAST_MIGRATED_VERSION = -1
        const val NO_FAILED_MIGRATION_ID = -1
    }

    fun getAppliedMigrations(): List<Int> {
        val appliedIdsStr: Set<String> = migrationsDb.getStringSet(DB_KEY_APPLIED_MIGRATIONS, null) ?: TreeSet()
        return appliedIdsStr.mapNotNull {
            try {
                it.toInt()
            } catch (_: NumberFormatException) {
                null
            }
        }.sorted()
    }

    fun addAppliedMigration(vararg migrations: Int) {
        val oldApplied = migrationsDb.getStringSet(DB_KEY_APPLIED_MIGRATIONS, null) ?: TreeSet()
        val newApplied = TreeSet<String>().apply {
            addAll(oldApplied)
            addAll(migrations.map { it.toString() })
        }
        migrationsDb.edit().putStringSet(DB_KEY_APPLIED_MIGRATIONS, newApplied).apply()
    }

    var lastMigratedVersion: Int
        set(value) {
            migrationsDb.edit().putInt(DB_KEY_LAST_MIGRATED_VERSION, value).apply()
        }
        get() {
            return migrationsDb.getInt(DB_KEY_LAST_MIGRATED_VERSION, NO_LAST_MIGRATED_VERSION)
        }

    val isFailed: Boolean get() = migrationsDb.getBoolean(DB_KEY_FAILED, false)
    val failureReason: String get() = migrationsDb.getString(DB_KEY_FAILED_MIGRATION_ERROR_MESSAGE, "") ?: ""
    val failedMigrationId: Int get() = migrationsDb.getInt(DB_KEY_FAILED_MIGRATION_ID, NO_FAILED_MIGRATION_ID)

    fun setFailed(id: Int, error: String) {
        migrationsDb
            .edit()
            .putBoolean(DB_KEY_FAILED, true)
            .putString(DB_KEY_FAILED_MIGRATION_ERROR_MESSAGE, error)
            .putInt(DB_KEY_FAILED_MIGRATION_ID, id)
            .apply()
    }

    fun clearMigrations() {
        migrationsDb.edit()
            .putStringSet(DB_KEY_APPLIED_MIGRATIONS, emptySet())
            .putInt(DB_KEY_LAST_MIGRATED_VERSION, 0)
            .apply()
    }
}
