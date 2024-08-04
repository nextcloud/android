/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter

import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import org.junit.Assert.assertEquals
import org.junit.Test

class OCShareToOCFileConverterTest {

    @Test
    fun testSingleOCShare() {
        val shares = listOf(
            OCShare("/foo")
                .apply {
                    shareType = ShareType.PUBLIC_LINK
                    isFavorite = true
                }
        )

        val result = OCShareToOCFileConverter.buildOCFilesFromShares(shares)

        assertEquals("Wrong file list size", 1, result.size)
        val ocFile = result[0]
        assertEquals("Wrong file path", "/foo", ocFile.remotePath)
        assertEquals("File should have link attribute", true, ocFile.isSharedViaLink)
        assertEquals("File should not have sharee attribute", false, ocFile.isSharedWithSharee)
        assertEquals("Wrong favorite status", true, ocFile.isFavorite)
    }

    @Test
    fun testMultipleSharesSamePath() {
        val shares = listOf(
            OCShare("/foo")
                .apply {
                    shareType = ShareType.PUBLIC_LINK
                    sharedDate = 10
                },
            OCShare("/foo")
                .apply {
                    shareType = ShareType.EMAIL
                    sharedDate = 22
                },
            OCShare("/foo")
                .apply {
                    shareType = ShareType.INTERNAL
                    sharedDate = 11
                    shareWith = "abcd"
                    sharedWithDisplayName = "Ab Cd"
                }
        )

        val result = OCShareToOCFileConverter.buildOCFilesFromShares(shares)

        assertEquals("Wrong file list size", 1, result.size)
        val ocFile = result[0]
        assertEquals("Wrong file path", "/foo", ocFile.remotePath)
        assertEquals("File should have link attribute", true, ocFile.isSharedViaLink)
        assertEquals("File should have sharee attribute", true, ocFile.isSharedWithSharee)
        assertEquals("Wrong name of sharees", 1, ocFile.sharees.size)
        assertEquals("Wrong shared timestamp", 10000, ocFile.firstShareTimestamp)
        assertEquals("Wrong favorite status", false, ocFile.isFavorite)
    }

    @Test
    fun testMultipleSharesMultiplePaths() {
        val shares = listOf(
            OCShare("/foo")
                .apply {
                    shareType = ShareType.INTERNAL
                    sharedDate = 10
                    shareWith = "aabc"
                    sharedWithDisplayName = "Aa Bc"
                },
            OCShare("/foo")
                .apply {
                    shareType = ShareType.INTERNAL
                    sharedDate = 22
                    shareWith = "cccc"
                    sharedWithDisplayName = "Cc Cc"
                },
            OCShare("/foo")
                .apply {
                    shareType = ShareType.INTERNAL
                    sharedDate = 11
                    shareWith = "abcd"
                    sharedWithDisplayName = "Ab Cd"
                },
            OCShare("/bar")
                .apply {
                    shareType = ShareType.EMAIL
                    sharedDate = 5
                }
        )

        val result = OCShareToOCFileConverter.buildOCFilesFromShares(shares)

        assertEquals("Wrong file list size", 2, result.size)

        val ocFile = result[0]
        assertEquals("Wrong file path", "/foo", ocFile.remotePath)
        assertEquals("File should have no link attribute", false, ocFile.isSharedViaLink)
        assertEquals("File should have sharee attribute", true, ocFile.isSharedWithSharee)
        assertEquals("Wrong name of sharees", 3, ocFile.sharees.size)
        assertEquals("Wrong shared timestamp", 10000, ocFile.firstShareTimestamp)

        val ocFile2 = result[1]
        assertEquals("Wrong file path", "/bar", ocFile2.remotePath)
        assertEquals("File should have link attribute", true, ocFile2.isSharedViaLink)
        assertEquals("File should have no sharee attribute", false, ocFile2.isSharedWithSharee)
    }
}
