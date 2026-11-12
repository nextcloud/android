/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.datamodel

import android.content.ContentResolver
import android.database.Cursor
import com.nextcloud.client.database.NextcloudDatabase
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta
import com.owncloud.android.db.UploadResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

class UploadsStorageManagerCursorTest {
    private lateinit var manager: UploadsStorageManager
    private lateinit var cursor: Cursor
    private val columns = mutableMapOf<String, Int>()
    private var lockToken: String? = null

    @Before
    fun setUp() {
        val database = mockk<NextcloudDatabase>(relaxed = true)
        mockkObject(NextcloudDatabase.Companion)
        every { NextcloudDatabase.instance() } returns database
        mockkObject(MainApp.Companion)
        every { MainApp.authority } returns "test.authority"
        every { MainApp.getAppContext() } throws IllegalStateException("No capability available")

        cursor = mockk(relaxed = true)
        every { cursor.getColumnIndexOrThrow(any()) } answers {
            columns.getOrPut(firstArg()) { columns.size }
        }
        every { cursor.getColumnIndex(any()) } returns -1
        every { cursor.getString(any()) } answers {
            when (columns.entries.first { it.value == firstArg<Int>() }.key) {
                ProviderTableMeta.UPLOADS_LOCAL_PATH -> File.separator + "photo.jpg"
                ProviderTableMeta.UPLOADS_REMOTE_PATH -> "/private/photo.jpg"
                ProviderTableMeta.UPLOADS_ACCOUNT_NAME -> "test-account"
                ProviderTableMeta.UPLOADS_FOLDER_UNLOCK_TOKEN -> lockToken
                else -> null
            }
        }
        every { cursor.getLong(any()) } returns 1L
        every { cursor.getInt(any()) } answers {
            when (columns.entries.first { it.value == firstArg<Int>() }.key) {
                ProviderTableMeta.UPLOADS_STATUS -> UploadStatus.UPLOAD_FAILED.value
                ProviderTableMeta.UPLOADS_LAST_RESULT -> UploadResult.SYNC_CONFLICT.value
                else -> 0
            }
        }
        every { cursor.moveToFirst() } returns true
        every { cursor.moveToNext() } returns false

        val resolver = mockk<ContentResolver>()
        every { resolver.query(any(), null, any(), any(), any()) } returnsMany listOf(cursor, null)
        manager = UploadsStorageManager(mockk(), resolver)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `cleared encryption lock token does not prevent reading a conflict`() {
        val upload = manager.getUploadsForAccount("test-account").single()

        assertEquals("", upload.folderUnlockToken)
        assertEquals(UploadStatus.UPLOAD_FAILED, upload.uploadStatus)
        assertEquals(UploadResult.SYNC_CONFLICT, upload.lastResult)
        assertEquals("/private/photo.jpg", upload.remotePath)
    }

    @Test
    fun `active encryption lock token is preserved`() {
        lockToken = "active-token"

        assertEquals("active-token", manager.getUploadsForAccount("test-account").single().folderUnlockToken)
    }
}
