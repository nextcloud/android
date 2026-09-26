/*
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.datamodel

import android.content.Context
import android.media.MediaScannerConnection
import android.text.TextUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FileDataStorageManagerTriggerMediaScanTest {

    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } answers { arg<CharSequence?>(0)?.toString().isNullOrEmpty() }
        mockkStatic(MediaScannerConnection::class)
        every { MediaScannerConnection.scanFile(any(), any(), any(), any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun triggerMediaScan_withValidPath_callsMediaScannerConnection() {
        val path = "/storage/emulated/0/DCIM/photo.jpg"

        FileDataStorageManager.triggerMediaScan(mockContext, path, null)

        val pathsSlot = slot<Array<String>>()
        verify(exactly = 1) {
            MediaScannerConnection.scanFile(
                mockContext,
                capture(pathsSlot),
                null,
                any()
            )
        }
        assertEquals(path, pathsSlot.captured.single())
    }

    @Test
    fun triggerMediaScan_withOCFile_passesMimeType() {
        val path = "/storage/emulated/0/DCIM/photo.jpg"
        val file = mockk<OCFile>(relaxed = true) {
            every { mimeType } returns "image/jpeg"
        }

        FileDataStorageManager.triggerMediaScan(mockContext, path, file)

        val pathsSlot = slot<Array<String>>()
        val mimeTypesSlot = slot<Array<String>>()
        verify(exactly = 1) {
            MediaScannerConnection.scanFile(
                mockContext,
                capture(pathsSlot),
                capture(mimeTypesSlot),
                any()
            )
        }
        assertEquals(path, pathsSlot.captured.single())
        assertEquals("image/jpeg", mimeTypesSlot.captured.single())
    }

    @Test
    fun triggerMediaScan_withEmptyPath_doesNotCallMediaScanner() {
        FileDataStorageManager.triggerMediaScan(mockContext, "", null)

        verify(exactly = 0) {
            MediaScannerConnection.scanFile(any(), any(), any(), any())
        }
    }

    @Test
    fun triggerMediaScan_withNullPath_doesNotCallMediaScanner() {
        FileDataStorageManager.triggerMediaScan(mockContext, null, null)

        verify(exactly = 0) {
            MediaScannerConnection.scanFile(any(), any(), any(), any())
        }
    }
}
