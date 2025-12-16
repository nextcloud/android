/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.providers

import android.content.ContentValues
import android.content.ContentUris
import android.net.Uri
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import com.owncloud.android.db.ProviderMeta
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FileContentProviderTests {

    private lateinit var provider: FileContentProvider
    private lateinit var db: SupportSQLiteDatabase

    @Before
    fun setup() {
        provider = FileContentProvider()
        db = mockk()
    }

    @Test
    fun insertNewFileShouldReturnNewId() {
        val values = ContentValues().apply {
            put(ProviderMeta.ProviderTableMeta.FILE_PATH, "/path/to/file.txt")
            put(ProviderMeta.ProviderTableMeta.FILE_ACCOUNT_OWNER, "user@example.com")
        }

        // Mock insert to return new ID
        every { db.insert(any(), any(), any()) } returns 42L

        val result: Uri = provider.upsertSingleFile(
            db,
            ProviderMeta.ProviderTableMeta.CONTENT_URI_FILE,
            values
        )

        assertEquals(
            ContentUris.withAppendedId(ProviderMeta.ProviderTableMeta.CONTENT_URI_FILE, 42),
            result
        )
    }

    @Test
    fun updateExistingFileShouldReturnSameId() {
        val values = ContentValues().apply {
            put(ProviderMeta.ProviderTableMeta.FILE_PATH, "/path/to/file.txt")
            put(ProviderMeta.ProviderTableMeta.FILE_ACCOUNT_OWNER, "user@example.com")
        }

        // Simulate insert conflict
        every { db.insert(ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME, any(), values) } returns -1L

        // Simulate update returning 1 row affected
        every {
            db.update(
                ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME,
                any(),
                values,
                any(),
                any()
            )
        } returns 1

        // Mock cursor to return ID 99
        val cursor = mockk<android.database.Cursor>()
        every { cursor.moveToFirst() } returns true
        every { cursor.getLong(0) } returns 99L
        every { cursor.close() } just Runs

        every { db.query(any<SimpleSQLiteQuery>()) } returns cursor

        val result: Uri = provider.upsertSingleFile(
            db,
            ProviderMeta.ProviderTableMeta.CONTENT_URI_FILE,
            values
        )

        assertEquals(ContentUris.withAppendedId(ProviderMeta.ProviderTableMeta.CONTENT_URI_FILE, 99), result)

        cursor.close()
    }

    @Test
    fun testConcurrentUpserts() = runBlocking {
        val values = ContentValues().apply {
            put(ProviderMeta.ProviderTableMeta.FILE_PATH, "/path/to/file.txt")
            put(ProviderMeta.ProviderTableMeta.FILE_ACCOUNT_OWNER, "user@example.com")
        }

        // shared state to simulate race
        val inserted = mutableListOf<Long>()

        // mock insert: fail first call, succeed second
        every { db.insert(any(), any(), any()) } answers {
            synchronized(inserted) {
                if (inserted.isEmpty()) {
                    // first thread "fails" insert which means already existing file id will be returned
                    inserted.add(-1L)
                    -1L
                } else {
                    // second thread "succeeds" it will update existing one
                    inserted.add(42L)
                    42L
                }
            }
        }

        // mock update only one row should be affected
        every { db.update(any(), any(), any(), any(), any()) } returns 1

        // mock query existing file id will return 99
        val cursor = mockk<android.database.Cursor>()
        every { cursor.moveToFirst() } returns true
        every { cursor.getLong(0) } returns 99L
        every { cursor.close() } just Runs
        every { db.query(any<SimpleSQLiteQuery>()) } returns cursor

        // launch two coroutines simulating concurrent threads
        val results = mutableListOf<Uri>()
        coroutineScope {
            val job1 =
                async {
                    results.add(provider.upsertSingleFile(db, ProviderMeta.ProviderTableMeta.CONTENT_URI_FILE, values))
                }
            val job2 =
                async {
                    results.add(provider.upsertSingleFile(db, ProviderMeta.ProviderTableMeta.CONTENT_URI_FILE, values))
                }
            awaitAll(job1, job2)
        }

        // both URIs should be correct (one updated, one inserted)
        assertTrue(results.contains(ContentUris.withAppendedId(ProviderMeta.ProviderTableMeta.CONTENT_URI_FILE, 99)))
        assertTrue(results.contains(ContentUris.withAppendedId(ProviderMeta.ProviderTableMeta.CONTENT_URI_FILE, 42)))

        cursor.close()
    }
}
