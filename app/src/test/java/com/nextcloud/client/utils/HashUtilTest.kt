/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: MIT
 */
package com.nextcloud.client.utils

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class HashUtilTest(private val input: String, private val expected: String) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params(): List<Array<Any>> = listOf(
            arrayOf("", "d41d8cd98f00b204e9800998ecf8427e"),
            arrayOf("test", "098f6bcd4621d373cade4e832627b4f6"),
            arrayOf("test@nextcloud.localhost", "12aa338095d171f307c3e3f724702ab1"),
            arrayOf("tost@nextcloud.localhost", "e01e5301f90c123a65e872d68e84c4b2")
        )
    }

    @Test
    fun testMd5Hash() {
        val hash = HashUtil.md5Hash(input)
        Assert.assertEquals("Wrong hash for input", expected, hash)
    }
}
