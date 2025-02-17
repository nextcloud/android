/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class OCFileTest {
    @Test
    fun testLongIds() {
        val sut = OCFile("/")

        sut.remoteId = "12345678ocjycgrudn78"
        assertEquals(12345678, sut.localId)

        sut.remoteId = "00000008ocjycgrudn78"
        assertEquals(8, sut.localId)

        // this will fail as fileId is too large
        sut.remoteId = "1234567891011ocjycgrudn78"
        assertNotEquals(1234567891011L, sut.localId)

        sut.localId = 1234567891011L
        assertEquals(1234567891011L, sut.localId)
    }

    @Test
    fun testThumbnailKeyWhenGivenOnlyRemoteIdShouldReturnThumbnailKey() {
        val sut = OCFile("/abc").apply {
            remoteId = "00001"
        }

        assertNotNull(sut.thumbnailKey)
    }

    @Test
    fun testThumbnailKeyWhenGivenOnlyRemoteIdForFilesShouldReturnDifferentThumbnailKeys() {
        val file1 = OCFile("/abc/a1.jpg").apply {
            remoteId = "00001"
            fileLength = 100
        }

        val file2 = OCFile("/abc/a1.jpg").apply {
            remoteId = "00002"
            fileLength = 101
        }

        assert(file1.thumbnailKey != file2.thumbnailKey)
    }

    @Test
    fun testThumbnailKeyWhenRenameFileThumbnailKeyShouldNotChange() {
        val file = OCFile("/abc").apply {
            fileLength = 100
            fileName = "a1.jpg"
        }

        val thumbnailKeyBeforeRename = file.thumbnailKey

        file.fileName = "a2.jpg"
        val thumbnailKeyAfterRename = file.thumbnailKey

        assert(thumbnailKeyBeforeRename == thumbnailKeyAfterRename)
    }

    @Test
    fun testThumbnailKeyWhenGivenEmptyRemoteIdShouldReturnThumbnailKey() {
        val sut = OCFile("/abc").apply {
            fileLength = 100
            fileName = "a1.jpg"
        }

        assertNotNull(sut.thumbnailKey)
    }

    @Test
    fun testThumbnailKeyWhenGivenEmptyRemoteIdForFilesShouldReturnDifferentThumbnailKeys() {
        val file1 = OCFile("/abc").apply {
            fileLength = 100
            fileName = "a1.jpg"
        }

        val file2 = OCFile("/abc").apply {
            fileLength = 101
            fileName = "a2.jpg"
        }

        assert(file1.thumbnailKey != file2.thumbnailKey)
    }
}
