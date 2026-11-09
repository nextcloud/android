/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 <Ruba Mogalli> <rubamogalli@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.extensions

import org.junit.Assert.*
import org.junit.Test

class IntArrayExtensionsTest {

    // region IntArray.toByteArray()

    @Test
    fun `empty int array results in empty byte array`() {
        assertEquals(0, intArrayOf().toByteArray().size)
    }

    @Test
    fun `single int takes four bytes`() {
        assertEquals(4, intArrayOf(1).toByteArray().size)
    }

    @Test
    fun `size grows by four bytes per int`() {
        assertEquals(12, intArrayOf(1, 2, 3).toByteArray().size)
    }

    @Test
    fun `ints are written in big endian order`() {
        assertArrayEquals(
            "most significant byte must come first",
            bytes(0x00, 0x00, 0x01, 0x02),
            intArrayOf(0x0102).toByteArray()
        )
    }

    @Test
    fun `negative one sets all bits`() {
        assertArrayEquals(bytes(0xFF, 0xFF, 0xFF, 0xFF), intArrayOf(-1).toByteArray())
    }

    @Test
    fun `max value keeps its sign bit cleared`() {
        assertArrayEquals(
            bytes(0x7F, 0xFF, 0xFF, 0xFF),
            intArrayOf(Int.MAX_VALUE).toByteArray()
        )
    }

    @Test
    fun `min value keeps its sign bit set`() {
        assertArrayEquals(
            bytes(0x80, 0x00, 0x00, 0x00),
            intArrayOf(Int.MIN_VALUE).toByteArray()
        )
    }

    // endregion

    // region ByteArray.toIntArray()

    @Test
    fun `empty byte array results in empty int array`() {
        assertEquals(0, byteArrayOf().toIntArray().size)
    }

    @Test
    fun `bytes are read in big endian order`() {
        assertArrayEquals(intArrayOf(0x0102), bytes(0x00, 0x00, 0x01, 0x02).toIntArray())
    }

    @Test
    fun `trailing bytes that do not fill an int are ignored`() {
        assertArrayEquals(intArrayOf(7), bytes(0x00, 0x00, 0x00, 0x07, 0x55).toIntArray())
    }

    @Test
    fun `fewer than four bytes result in empty int array`() {
        assertEquals(0, bytes(0x01, 0x02, 0x03).toIntArray().size)
    }

    // endregion

    // region round trips

    @Test
    fun `int array gets through a round trip through bytes`() {
        val original = intArrayOf(0, 1, -1, 42, -42, 0x01020304, Int.MAX_VALUE, Int.MIN_VALUE)

        assertArrayEquals(original, original.toByteArray().toIntArray())
    }

    @Test
    fun `byte array with a multiple of four size gets through a round trip through ints`() {
        val original = bytes(0x01, 0x02, 0x03, 0x04, 0xFB, 0xFA, 0xF9, 0xF8)

        assertArrayEquals(original, original.toIntArray().toByteArray())
    }

    // endregion

    // Allows values above 127 (e.g. 0xFF) without a .toByte() on every literal
    private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }
}