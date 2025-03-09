/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.migrations

import android.content.SharedPreferences
import androidx.core.content.edit
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
        migrationsDb.edit { putStringSet(DB_KEY_APPLIED_MIGRATIONS, newApplied) }
    }

    var lastMigratedVersion: Int
        set(value) {
            migrationsDb.edit { putInt(DB_KEY_LAST_MIGRATED_VERSION, value) }
        }
        get() {
            return migrationsDb.getInt(DB_KEY_LAST_MIGRATED_VERSION, NO_LAST_MIGRATED_VERSION)
        }

    val isFailed: Boolean get() = migrationsDb.getBoolean(DB_KEY_FAILED, false)
    val failureReason: String get() = migrationsDb.getString(DB_KEY_FAILED_MIGRATION_ERROR_MESSAGE, "") ?: ""
    val failedMigrationId: Int get() = migrationsDb.getInt(DB_KEY_FAILED_MIGRATION_ID, NO_FAILED_MIGRATION_ID)

    fun setFailed(id: Int, error: String) {
        migrationsDb
            .edit {
                putBoolean(DB_KEY_FAILED, true)
                    .putString(DB_KEY_FAILED_MIGRATION_ERROR_MESSAGE, error)
                    .putInt(DB_KEY_FAILED_MIGRATION_ID, id)
            }
    }

    fun clearMigrations() {
        migrationsDb.edit {
            putStringSet(DB_KEY_APPLIED_MIGRATIONS, emptySet())
                .putInt(DB_KEY_LAST_MIGRATED_VERSION, 0)
        }
    }
}
