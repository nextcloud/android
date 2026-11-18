/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 PeterPablo <4340267+PeterPablo@users.noreply.github.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.database.migrations

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nextcloud.client.database.NextcloudDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * 106 → 107 collapses the rows that were stored twice for one file and then forbids new ones.
 *
 * The duplicates were reachable because `filelist` had no unique index while
 * [com.owncloud.android.providers.FileContentProvider.upsertSingleFile] relied on an `INSERT … CONFLICT_IGNORE` to
 * detect an existing row — with no constraint, nothing could conflict, so a second row was always written and the
 * `UPDATE` behind that check was unreachable. A SAF consumer then showed the folder twice (nextcloud/android#17051).
 */
@RunWith(AndroidJUnit4::class)
class Migration106to107Test {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NextcloudDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun twinsCollapseToOldestRow_otherAccountsAndPathlessRowsSurvive() {
        var db = helper.createDatabase(TEST_DB, 106)

        db.execSQL("INSERT INTO filelist (filename, path, file_owner, parent) VALUES ('tmp','/tmp/','a@cloud',1)")
        db.execSQL("INSERT INTO filelist (filename, path, file_owner, parent) VALUES ('tmp','/tmp/','a@cloud',1)")
        db.execSQL("INSERT INTO filelist (filename, path, file_owner, parent) VALUES ('tmp','/tmp/','a@cloud',1)")
        // same path, different account: a different file, not a twin
        db.execSQL("INSERT INTO filelist (filename, path, file_owner, parent) VALUES ('tmp','/tmp/','b@cloud',1)")
        // no path: nothing to compare, must not be collapsed
        db.execSQL("INSERT INTO filelist (filename, path, file_owner, parent) VALUES ('pending',NULL,'a@cloud',9)")
        db.execSQL("INSERT INTO filelist (filename, path, file_owner, parent) VALUES ('pending',NULL,'a@cloud',9)")
        db.close()

        // runMigrationsAndValidate also asserts the result matches 107.json — i.e. this migration and the entity agree
        db = helper.runMigrationsAndValidate(TEST_DB, 107, true, MIGRATION_106_107)

        assertEquals(
            "the three twins must collapse to one row, the other account stays",
            2,
            count(db, "SELECT count(*) FROM filelist WHERE parent = 1")
        )
        assertEquals(
            "rows without a path have no identity to compare and must survive",
            2,
            count(db, "SELECT count(*) FROM filelist WHERE path IS NULL")
        )
        assertEquals(
            "the oldest row of the twins is the one kept",
            1,
            count(db, "SELECT _id FROM filelist WHERE path='/tmp/' AND file_owner='a@cloud'")
        )

        // the invariant now lives in the database: a further twin cannot be written at all
        db.execSQL(
            "INSERT OR IGNORE INTO filelist (filename, path, file_owner, parent) VALUES ('tmp','/tmp/','a@cloud',1)"
        )
        assertEquals(
            "a unique index on (path, file_owner) must reject the second row",
            2,
            count(db, "SELECT count(*) FROM filelist WHERE parent = 1")
        )

        db.close()
    }

    private fun count(db: androidx.sqlite.db.SupportSQLiteDatabase, sql: String): Int = db.query(sql).use { cursor ->
        cursor.moveToFirst()
        cursor.getInt(0)
    }

    companion object {
        private const val TEST_DB = "migration-106-107-test"
    }
}
