/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class E2eeByteArrayMediaDataSourceTest {
    @Test
    fun getSizeReturnsByteArraySize() {
        val source = E2eeByteArrayMediaDataSource(byteArrayOf(1, 2, 3))

        assertEquals(3L, source.size)
    }

    @Test
    fun readAtCopiesRequestedRangeIntoDestinationOffset() {
        val source = E2eeByteArrayMediaDataSource(byteArrayOf(10, 11, 12, 13))
        val destination = byteArrayOf(0, 0, 0, 0, 0)

        val bytesRead = source.readAt(position = 1, buffer = destination, offset = 2, size = 2)

        assertEquals(2, bytesRead)
        assertArrayEquals(byteArrayOf(0, 0, 11, 12, 0), destination)
    }

    @Test
    fun readAtReturnsRemainingBytesNearEnd() {
        val source = E2eeByteArrayMediaDataSource(byteArrayOf(10, 11, 12))
        val destination = byteArrayOf(0, 0, 0)

        val bytesRead = source.readAt(position = 2, buffer = destination, offset = 0, size = 3)

        assertEquals(1, bytesRead)
        assertArrayEquals(byteArrayOf(12, 0, 0), destination)
    }

    @Test
    fun readAtReturnsEndOfStreamWhenPositionIsPastEnd() {
        val source = E2eeByteArrayMediaDataSource(byteArrayOf(10, 11, 12))

        assertEquals(END_OF_STREAM, source.readAt(position = 3, buffer = ByteArray(1), offset = 0, size = 1))
    }

    @Test
    fun readAtReturnsEndOfStreamWhenPositionIsNegative() {
        val source = E2eeByteArrayMediaDataSource(byteArrayOf(10, 11, 12))

        assertEquals(END_OF_STREAM, source.readAt(position = -1, buffer = ByteArray(1), offset = 0, size = 1))
    }

    @Test
    fun readAtDoesNotWritePastDestinationEnd() {
        val source = E2eeByteArrayMediaDataSource(byteArrayOf(10, 11, 12))
        val destination = byteArrayOf(0, 0)

        val bytesRead = source.readAt(position = 0, buffer = destination, offset = 1, size = 3)

        assertEquals(1, bytesRead)
        assertArrayEquals(byteArrayOf(0, 10), destination)
    }

    companion object {
        private const val END_OF_STREAM = -1
    }
}
