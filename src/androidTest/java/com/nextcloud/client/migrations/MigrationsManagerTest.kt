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

import androidx.test.annotation.UiThreadTest
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.appinfo.AppInfo
import com.nextcloud.client.core.ManualAsyncRunner
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.lang.RuntimeException
import java.util.LinkedHashSet

class MigrationsManagerTest {

    companion object {
        const val OLD_APP_VERSION = 41
        const val NEW_APP_VERSION = 42
    }

    lateinit var migrations: List<Migrations.Step>

    @Mock
    lateinit var appInfo: AppInfo

    lateinit var migrationsDb: MockSharedPreferences

    @Mock
    lateinit var userAccountManager: UserAccountManager

    lateinit var asyncRunner: ManualAsyncRunner

    internal lateinit var migrationsManager: MigrationsManagerImpl

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        val migrationStep1: Runnable = mock()
        val migrationStep2: Runnable = mock()
        val migrationStep3: Runnable = mock()
        migrations = listOf(
            Migrations.Step(0, "first migration", migrationStep1, true),
            Migrations.Step(1, "second migration", migrationStep2, true),
            Migrations.Step(2, "third optional migration", migrationStep3, false)
        )
        asyncRunner = ManualAsyncRunner()
        migrationsDb = MockSharedPreferences()
        whenever(appInfo.versionCode).thenReturn(NEW_APP_VERSION)
        migrationsManager = MigrationsManagerImpl(
            appInfo = appInfo,
            migrationsDb = migrationsDb,
            asyncRunner = asyncRunner,
            migrations = migrations
        )
    }

    @Test
    fun inital_status_is_unknown() {
        // GIVEN
        //      migration manager has not been used yets

        // THEN
        //      status is not set
        assertEquals(MigrationsManager.Status.UNKNOWN, migrationsManager.status.value)
    }

    @Test
    fun applied_migrations_are_returned_in_order() {
        // GIVEN
        //      some migrations are marked as applied
        //      migration ids are stored in random order
        val storedMigrationIds = LinkedHashSet<String>()
        storedMigrationIds.apply {
            add("3")
            add("0")
            add("2")
            add("1")
        }
        migrationsDb.store.put(MigrationsManagerImpl.DB_KEY_APPLIED_MIGRATIONS, storedMigrationIds)

        // WHEN
        //      applied migration ids are retrieved
        val ids = migrationsManager.getAppliedMigrations()

        // THEN
        //      returned list is sorted
        assertEquals(ids, ids.sorted())
    }

    @Test
    @Suppress("MagicNumber")
    fun registering_new_applied_migration_preserves_old_ids() {
        // WHEN
        //     some applied migrations are registered
        val appliedMigrationIds = setOf("0", "1", "2")
        migrationsDb.store.put(MigrationsManagerImpl.DB_KEY_APPLIED_MIGRATIONS, appliedMigrationIds)

        // WHEN
        //     new set of migration ids are registered
        //      some ids are added again
        migrationsManager.addAppliedMigration(2, 3, 4)

        // THEN
        //      new ids are appended to set of existing ids
        val expectedIds = setOf("0", "1", "2", "3", "4")
        assertEquals(expectedIds, migrationsDb.store.get(MigrationsManagerImpl.DB_KEY_APPLIED_MIGRATIONS))
    }

    @Test
    @UiThreadTest
    fun migrations_are_scheduled_on_background_thread() {
        // GIVEN
        //      migrations can be applied
        assertEquals(0, migrationsManager.getAppliedMigrations().size)

        // WHEN
        //      migration is started
        val count = migrationsManager.startMigration()

        // THEN
        //      all migrations are scheduled on background thread
        //      single task is scheduled
        assertEquals(migrations.size, count)
        assertEquals(1, asyncRunner.size)
        assertEquals(MigrationsManager.Status.RUNNING, migrationsManager.status.value)
    }

    @Test
    @UiThreadTest
    fun applied_migrations_are_recorded() {
        // GIVEN
        //      no migrations are applied yet
        //      current app version is newer then last recorded migrated version
        whenever(appInfo.versionCode).thenReturn(NEW_APP_VERSION)
        migrationsDb.store.put(MigrationsManagerImpl.DB_KEY_LAST_MIGRATED_VERSION, OLD_APP_VERSION)

        // WHEN
        //      migration is run
        whenever(userAccountManager.migrateUserId()).thenReturn(true)
        val count = migrationsManager.startMigration()
        assertTrue(asyncRunner.runOne())

        // THEN
        //      total migrations count is returned
        //      migration functions are called
        //      applied migrations are recorded
        //      new app version code is recorded
        assertEquals(migrations.size, count)
        val allAppliedIds = migrations.map { it.id.toString() }.toSet()
        assertEquals(allAppliedIds, migrationsDb.store.get(MigrationsManagerImpl.DB_KEY_APPLIED_MIGRATIONS))
        assertEquals(NEW_APP_VERSION, migrationsDb.store.get(MigrationsManagerImpl.DB_KEY_LAST_MIGRATED_VERSION))
    }

    @Test
    @UiThreadTest
    fun migration_error_is_recorded() {
        // GIVEN
        //      no migrations applied yet

        // WHEN
        //      migrations are applied
        //      one migration throws
        val lastMigration = migrations.findLast { it.mandatory } ?: throw IllegalStateException("Test fixture error")
        val errorMessage = "error message"
        whenever(lastMigration.function.run()).thenThrow(RuntimeException(errorMessage))
        migrationsManager.startMigration()
        assertTrue(asyncRunner.runOne())

        // THEN
        //      failure is marked in the migration db
        //      failure message is recorded
        //      failed migration id is recorded
        assertEquals(MigrationsManager.Status.FAILED, migrationsManager.status.value)
        assertTrue(migrationsDb.getBoolean(MigrationsManagerImpl.DB_KEY_FAILED, false))
        assertEquals(
            errorMessage,
            migrationsDb.getString(MigrationsManagerImpl.DB_KEY_FAILED_MIGRATION_ERROR_MESSAGE, "")
        )
        assertEquals(
            lastMigration.id,
            migrationsDb.getInt(MigrationsManagerImpl.DB_KEY_FAILED_MIGRATION_ID, -1)
        )
    }

    @Test
    @UiThreadTest
    fun migrations_are_not_run_if_already_run_for_an_app_version() {
        // GIVEN
        //      migrations were already run for the current app version
        whenever(appInfo.versionCode).thenReturn(NEW_APP_VERSION)
        migrationsDb.store.put(MigrationsManagerImpl.DB_KEY_LAST_MIGRATED_VERSION, NEW_APP_VERSION)

        // WHEN
        //      app is migrated again
        val migrationCount = migrationsManager.startMigration()

        // THEN
        //      migration processing is skipped entirely
        //      status is set to applied
        assertEquals(0, migrationCount)
        migrations.forEach {
            verify(it.function, never()).run()
        }
        assertEquals(MigrationsManager.Status.APPLIED, migrationsManager.status.value)
    }

    @Test
    @UiThreadTest
    fun new_app_version_is_marked_as_migrated_if_no_new_migrations_are_available() {
        //  GIVEN
        //      migrations were applied in previous version
        //      new version has no new migrations
        whenever(appInfo.versionCode).thenReturn(NEW_APP_VERSION)
        migrationsDb.store.put(MigrationsManagerImpl.DB_KEY_LAST_MIGRATED_VERSION, OLD_APP_VERSION)
        val applied = migrations.map { it.id.toString() }.toSet()
        migrationsDb.store.put(MigrationsManagerImpl.DB_KEY_APPLIED_MIGRATIONS, applied)

        // WHEN
        //      migration is started
        val startedCount = migrationsManager.startMigration()

        // THEN
        //      no new migrations are run
        //      new version is marked as migrated
        assertEquals(0, startedCount)
        assertEquals(
            NEW_APP_VERSION,
            migrationsDb.getInt(MigrationsManagerImpl.DB_KEY_LAST_MIGRATED_VERSION, -1)
        )
    }

    @Test
    @UiThreadTest
    fun optional_migration_failure_does_not_trigger_a_migration_failure() {
        // GIVEN
        //      pending migrations
        //      mandatory migrations are passing
        //      one migration is optional and fails
        val optionalFailingMigration = migrations.first { !it.mandatory }
        whenever(optionalFailingMigration.function.run()).thenThrow(RuntimeException())

        // WHEN
        //      migration is started
        val startedCount = migrationsManager.startMigration()
        asyncRunner.runOne()
        assertEquals(migrations.size, startedCount)

        // THEN
        //      mandatory migrations are marked as applied
        //      optional failed migration is not marked
        //      no error
        //      status is applied
        //      failed migration is available during next migration
        val appliedMigrations = migrations.filter { it.mandatory }
            .map { it.id.toString() }
            .toSet()
        assertTrue("Fixture error", appliedMigrations.isNotEmpty())
        assertEquals(appliedMigrations, migrationsDb.store.get(MigrationsManagerImpl.DB_KEY_APPLIED_MIGRATIONS))
        assertFalse(migrationsDb.getBoolean(MigrationsManagerImpl.DB_KEY_FAILED, false))
        assertEquals(MigrationsManager.Status.APPLIED, migrationsManager.status.value)
    }
}
