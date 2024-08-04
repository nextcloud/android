/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class TextDrawableTest {

    @Parameterized.Parameter(0)
    lateinit var expected: String

    @Parameterized.Parameter(1)
    lateinit var input: String

    @Test
    fun twoDigitAvatars() {
        assertEquals(
            "Avatar chars from displayname not correct",
            expected,
            TextDrawable.extractCharsFromDisplayName(input)
        )
    }

    companion object {
        @Parameterized.Parameters(name = "{1}")
        @JvmStatic
        fun data(): Iterable<Array<String>> = listOf(
            arrayOf("A", "Admin"),
            arrayOf("TS", "Test Server Admin"),
            arrayOf("", ""),
            arrayOf("CP", "Cormier Paulette"),
            arrayOf("WB", "winston brent"),
            arrayOf("BJ", "Baker James Lorena"),
            arrayOf("BJ", "Baker  James   Lorena"),
            arrayOf("E", "email@nextcloud.localhost"),
            arrayOf("SA", "  Spaces At Start"),
            arrayOf("SA", "Spaces At End   "),
            arrayOf("SA", "  Spaces At Start And End   "),
            arrayOf("", "  ")
        )
    }
}
