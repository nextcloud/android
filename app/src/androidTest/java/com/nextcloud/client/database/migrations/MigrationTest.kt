/*
 * Nextcloud Android client application
 *
 *  @author Álvaro Brey
 *  Copyright (C) 2023 Álvaro Brey
 *  Copyright (C) 2023 Nextcloud GmbH
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

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NextcloudDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate67to68() {
        val nullId = 6
        val notNullId = 7
        val notNullLocalIdValue = 1234

        var db = helper.createDatabase(TEST_DB, 67)

        // create some data
        db.apply {
            execSQL(
                "INSERT INTO filelist VALUES($nullId,'foo.zip','foo.zip','/foo.zip','/foo.zip',1,1643648081," +
                    "1643648081000,'application/zip',178382355,NULL,'test@nextcloud',1674554955638,0,0,''," +
                    "'f45028679b68652c6b345b5d8c9a5d63',0,'RGDNVW','00014889ocb5tqw7y2f3',NULL,0,0,0,0,NULL,0,NULL," +
                    "0,0,'test','test','','[]',NULL,'null',0,-1,NULL,NULL,NULL,0,0,NULL);"
            )
            execSQL(
                "INSERT INTO filelist VALUES($notNullId,'foo.zip','foo.zip','/foo.zip','/foo.zip',1,1643648081," +
                    "1643648081000,'application/zip',178382355,NULL,'test@nextcloud',1674554955638,0,0,''," +
                    "'f45028679b68652c6b345b5d8c9a5d63',0,'RGDNVW','00014889ocb5tqw7y2f3',NULL,0,0,0,0,NULL,0,NULL," +
                    "0,0,'test','test','','[]',NULL,'null',0,-1,NULL,NULL,NULL,0,0,NULL);"
            )
            execSQL("UPDATE filelist SET local_id = NULL WHERE _id = $nullId")
            execSQL("UPDATE filelist SET local_id = $notNullLocalIdValue WHERE _id = $notNullId")

            close()
        }

        // run migration and validate schema matches
        db = helper.runMigrationsAndValidate(TEST_DB, 68, true, Migration67to68())

        // check values are correct
        db.query("SELECT local_id FROM filelist WHERE _id=$nullId").use { cursor ->
            cursor.moveToFirst()
            val localId = cursor.getInt(cursor.getColumnIndex("local_id"))
            assertEquals("NULL localId is not -1 after migration", -1, localId)
        }

        db.query("SELECT local_id FROM filelist WHERE _id=$notNullId").use { cursor ->
            cursor.moveToFirst()
            val localId = cursor.getInt(cursor.getColumnIndex("local_id"))
            assertEquals("Not null localId is not the same after migration", notNullLocalIdValue, localId)
        }

        db.close()
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
