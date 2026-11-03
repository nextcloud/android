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
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@Suppress("MagicNumber")
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

        val cursor = mockk<android.database.Cursor>()
        every { cursor.moveToFirst() } returns false
        every { cursor.close() } just Runs
        every { db.query(any<SimpleSQLiteQuery>()) } returns cursor

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
}
