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
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.nextcloud.client.appinfo.AppInfo
import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.migrations.MigrationsManager.Status
import java.util.TreeSet

internal class MigrationsManagerImpl(
    private val appInfo: AppInfo,
    private val migrationsDb: SharedPreferences,
    private val asyncRunner: AsyncRunner,
    private val migrations: Collection<Migrations.Step>
) : MigrationsManager {

    companion object {
        const val DB_KEY_LAST_MIGRATED_VERSION = "last_migrated_version"
        const val DB_KEY_APPLIED_MIGRATIONS = "applied_migrations"
        const val DB_KEY_FAILED = "failed"
        const val DB_KEY_FAILED_MIGRATION_ID = "failed_migration_id"
        const val DB_KEY_FAILED_MIGRATION_ERROR_MESSAGE = "failed_migration_error"
    }

    override val status: LiveData<Status>

    init {
        this.status = MutableLiveData<Status>(Status.UNKNOWN)
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

    @Throws(MigrationError::class)
    @Suppress("ReturnCount")
    override fun startMigration(): Int {

        if (migrationsDb.getBoolean(DB_KEY_FAILED, false)) {
            (status as MutableLiveData<Status>).value = Status.FAILED
            return 0
        }
        val lastMigratedVersion = migrationsDb.getInt(DB_KEY_LAST_MIGRATED_VERSION, -1)
        if (lastMigratedVersion >= appInfo.versionCode) {
            (status as MutableLiveData<Status>).value = Status.APPLIED
            return 0
        }
        val applied = getAppliedMigrations()
        val toApply = migrations.filter { !applied.contains(it.id) }
        if (toApply.isEmpty()) {
            onMigrationSuccess()
            return 0
        }
        (status as MutableLiveData<Status>).value = Status.RUNNING
        asyncRunner.post(
            task = { asyncApplyMigrations(toApply) },
            onResult = { onMigrationSuccess() },
            onError = { onMigrationFailed(it) }
        )
        return toApply.size
    }

    /**
     * This method calls all pending migrations which can execute long-blocking code.
     * It should be run in a background thread.
     */
    private fun asyncApplyMigrations(migrations: Collection<Migrations.Step>) {
        migrations.forEach {
            @Suppress("TooGenericExceptionCaught") // migration code is free to throw anything
            try {
                it.function.run()
                addAppliedMigration(it.id)
            } catch (t: Throwable) {
                if (it.mandatory) {
                    throw MigrationError(id = it.id, message = t.message ?: t.javaClass.simpleName)
                }
            }
        }
    }

    @MainThread
    private fun onMigrationFailed(error: Throwable) {
        val id = when (error) {
            is MigrationError -> error.id
            else -> -1
        }
        migrationsDb
            .edit()
            .putBoolean(DB_KEY_FAILED, true)
            .putString(DB_KEY_FAILED_MIGRATION_ERROR_MESSAGE, error.message)
            .putInt(DB_KEY_FAILED_MIGRATION_ID, id)
            .apply()
        (status as MutableLiveData<Status>).value = Status.FAILED
    }

    @MainThread
    private fun onMigrationSuccess() {
        migrationsDb.edit().putInt(DB_KEY_LAST_MIGRATED_VERSION, appInfo.versionCode).apply()
        (status as MutableLiveData<Status>).value = Status.APPLIED
    }
}
