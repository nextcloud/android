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
import com.nextcloud.client.appinfo.AppInfo
import com.nextcloud.client.core.ManualAsyncRunner
import com.nhaarman.mockitokotlin2.inOrder
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

class MigrationsManagerTest {

    companion object {
        const val OLD_APP_VERSION = 41
        const val NEW_APP_VERSION = 42
    }

    lateinit var migrationStep1: Runnable
    lateinit var migrationStep2: Runnable
    lateinit var migrationStep3: Runnable
    lateinit var migrations: List<Migrations.Step>

    @Mock
    lateinit var appInfo: AppInfo

    lateinit var migrationsDbStore: MockSharedPreferences
    lateinit var migrationsDb: MigrationsDb

    lateinit var asyncRunner: ManualAsyncRunner

    internal lateinit var migrationsManager: MigrationsManagerImpl

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        migrationStep1 = mock()
        migrationStep2 = mock()
        migrationStep3 = mock()
        migrations = listOf(
            object : Migrations.Step(0, "first migration", true) {
                override fun run() {
                    migrationStep1.run()
                }
            },
            object : Migrations.Step(1, "second optional migration", false) {
                override fun run() {
                    migrationStep2.run()
                }
            },
            object : Migrations.Step(2, "third migration", true) {
                override fun run() {
                    migrationStep3.run()
                }
            }
        )
        asyncRunner = ManualAsyncRunner()
        migrationsDbStore = MockSharedPreferences()
        migrationsDb = MigrationsDb(migrationsDbStore)

        whenever(appInfo.versionCode).thenReturn(NEW_APP_VERSION)
        migrationsManager = MigrationsManagerImpl(
            appInfo = appInfo,
            migrationsDb = migrationsDb,
            asyncRunner = asyncRunner,
            migrations = migrations
        )
    }

    private fun assertMigrationsRun(vararg migrationSteps: Runnable) {
        inOrder(migrationSteps).apply {
            migrationSteps.forEach {
                verify(it.run())
            }
        }
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
    @UiThreadTest
    fun migrations_are_scheduled_on_background_thread() {
        // GIVEN
        //      migrations can be applied
        assertEquals(0, migrationsDb.getAppliedMigrations().size)

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
        migrationsDb.lastMigratedVersion = OLD_APP_VERSION

        // WHEN
        //      migration is run
        val count = migrationsManager.startMigration()
        assertTrue(asyncRunner.runOne())

        // THEN
        //      total migrations count is returned
        //      migration functions are called
        //      applied migrations are recorded
        //      new app version code is recorded
        assertEquals(migrations.size, count)
        inOrder(migrationStep1, migrationStep2, migrationStep3).apply {
            verify(migrationStep1).run()
            verify(migrationStep2).run()
            verify(migrationStep3).run()
        }
        val allAppliedIds = migrations.map { it.id }
        assertEquals(allAppliedIds, migrationsDb.getAppliedMigrations())
        assertEquals(NEW_APP_VERSION, migrationsDb.lastMigratedVersion)
    }

    @Test
    @UiThreadTest
    fun migration_error_is_recorded() {
        // GIVEN
        //      no migrations applied yet
        //      no prior failed migrations
        assertFalse(migrationsDb.isFailed)
        assertEquals(MigrationsDb.NO_FAILED_MIGRATION_ID, migrationsDb.failedMigrationId)

        // WHEN
        //      migrations are applied
        //      one migration throws
        val lastMigration = migrations.findLast { it.mandatory } ?: throw IllegalStateException("Test fixture error")
        val errorMessage = "error message"
        whenever(lastMigration.run()).thenThrow(RuntimeException(errorMessage))
        migrationsManager.startMigration()
        assertTrue(asyncRunner.runOne())

        // THEN
        //      failure is marked in the migration db
        //      failure message is recorded
        //      failed migration id is recorded
        assertEquals(MigrationsManager.Status.FAILED, migrationsManager.status.value)
        assertTrue(migrationsDb.isFailed)
        assertEquals(errorMessage, migrationsDb.failureReason)
        assertEquals(lastMigration.id, migrationsDb.failedMigrationId)
    }

    @Test
    @UiThreadTest
    fun migrations_are_not_run_if_already_run_for_an_app_version() {
        // GIVEN
        //      migrations were already run for the current app version
        whenever(appInfo.versionCode).thenReturn(NEW_APP_VERSION)
        migrationsDb.lastMigratedVersion = NEW_APP_VERSION

        // WHEN
        //      app is migrated again
        val migrationCount = migrationsManager.startMigration()

        // THEN
        //      migration processing is skipped entirely
        //      status is set to applied
        assertEquals(0, migrationCount)
        listOf(migrationStep1, migrationStep2, migrationStep3).forEach {
            verify(it, never()).run()
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
        migrationsDb.lastMigratedVersion = OLD_APP_VERSION
        migrations.forEach {
            migrationsDb.addAppliedMigration(it.id)
        }

        // WHEN
        //      migration is started
        val startedCount = migrationsManager.startMigration()

        // THEN
        //      no new migrations are run
        //      new version is marked as migrated
        assertEquals(0, startedCount)
        assertEquals(
            NEW_APP_VERSION,
            migrationsDb.lastMigratedVersion
        )
    }

    @Test
    @UiThreadTest
    fun optional_migration_failure_does_not_trigger_a_migration_failure() {
        // GIVEN
        //      pending migrations
        //      mandatory migrations are passing
        //      one migration is optional and fails
        assertEquals("Fixture should provide 1 optional, failing migration", 1, migrations.count { !it.mandatory })
        val optionalFailingMigration = migrations.first { !it.mandatory }
        whenever(optionalFailingMigration.run()).thenThrow(RuntimeException())

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
        val appliedMigrations = migrations.filter { it.mandatory }.map { it.id }
        assertTrue("Fixture error", appliedMigrations.isNotEmpty())
        assertEquals(appliedMigrations, migrationsDb.getAppliedMigrations())
        assertFalse(migrationsDb.isFailed)
        assertEquals(MigrationsManager.Status.APPLIED, migrationsManager.status.value)
    }
}
