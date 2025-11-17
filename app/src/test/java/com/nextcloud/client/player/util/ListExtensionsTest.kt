/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.util

import org.junit.Assert
import org.junit.Test

class ListExtensionsTest {
    private val inputList = listOf(1, 2, 3)

    @Test
    fun `calculateShift returns expected shift`() {
        val shift = inputList.calculateShift(0, inputList[2])
        Assert.assertEquals(1, shift)
    }

    @Test
    fun `rotate returns expected list`() {
        val expectedList = listOf(3, 1, 2)
        val rotatedList = inputList.rotate(1)
        Assert.assertEquals(expectedList, rotatedList)
    }
}
