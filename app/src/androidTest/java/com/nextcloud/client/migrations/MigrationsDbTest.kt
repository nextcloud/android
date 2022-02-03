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

import android.content.Context
import android.content.SharedPreferences
import androidx.test.platform.app.InstrumentationRegistry
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

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
