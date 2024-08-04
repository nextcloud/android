/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.migrations

import android.content.Context
import android.content.SharedPreferences
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class MigrationsDbTest {

    private lateinit var context: Context
    private lateinit var store: MockSharedPreferences
    private lateinit var db: MigrationsDb

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
        store = MockSharedPreferences()
        assertTrue("State from previous test run found?", store.all.isEmpty())
        db = MigrationsDb(store)
    }

    @Test
    fun applied_migrations_are_returned_in_order() {
        // GIVEN
        //      some migrations are marked as applied
        //      migration ids are stored in random order
        val mockStore: SharedPreferences = mock()
        val storedMigrationIds = LinkedHashSet<String>()
        storedMigrationIds.apply {
            add("3")
            add("0")
            add("2")
            add("1")
        }
        whenever(mockStore.getStringSet(eq(MigrationsDb.DB_KEY_APPLIED_MIGRATIONS), any()))
            .thenReturn(storedMigrationIds)

        // WHEN
        //      applied migration ids are retrieved
        val db = MigrationsDb(mockStore)
        val ids = db.getAppliedMigrations()

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
        store.edit().putStringSet(MigrationsDb.DB_KEY_APPLIED_MIGRATIONS, appliedMigrationIds).apply()

        // WHEN
        //     new set of migration ids are registered
        //      some ids are added again
        db.addAppliedMigration(2, 3, 4)

        // THEN
        //      new ids are appended to set of existing ids
        val expectedIds = setOf("0", "1", "2", "3", "4")
        val storedIds = store.getStringSet(MigrationsDb.DB_KEY_APPLIED_MIGRATIONS, mutableSetOf())
        assertEquals(expectedIds, storedIds)
    }

    @Test
    fun failed_status_sets_status_flag_and_error_message() {
        // GIVEN
        //      failure flag is not set
        assertFalse(db.isFailed)

        // WHEN
        //      failure status is set
        val failureReason = "error message"
        db.setFailed(0, failureReason)

        // THEN
        //      failed flag is set
        //      error message is set
        assertTrue(db.isFailed)
        assertEquals(failureReason, db.failureReason)
    }

    @Test
    fun last_migrated_version_is_set() {
        // GIVEN
        //      last migrated version is not set
        val oldVersion = db.lastMigratedVersion
        assertEquals(MigrationsDb.NO_LAST_MIGRATED_VERSION, oldVersion)

        // WHEN
        //      migrated version is set to a new value
        val newVersion = 200
        db.lastMigratedVersion = newVersion

        // THEN
        //      new value is stored
        assertEquals(newVersion, db.lastMigratedVersion)
    }
}
