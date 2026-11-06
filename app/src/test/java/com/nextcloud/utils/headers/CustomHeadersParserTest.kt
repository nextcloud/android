/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.headers

import org.junit.Assert.assertEquals
import org.junit.Test

class CustomHeadersParserTest {

    @Test
    fun `lines are parsed in order with names and values trimmed`() {
        val result = CustomHeadersParser.parse("  CF-Access-Client-Id :  abc \n\nX-Token: a:b:c\n")

        assertEquals(
            CustomHeadersParseResult.Valid(linkedMapOf("CF-Access-Client-Id" to "abc", "X-Token" to "a:b:c")),
            result
        )
        assertEquals(
            listOf("CF-Access-Client-Id", "X-Token"),
            (result as CustomHeadersParseResult.Valid).headers.keys.toList()
        )
    }

    @Test
    fun `empty text is valid without headers`() {
        assertEquals(CustomHeadersParseResult.Valid(emptyMap()), CustomHeadersParser.parse(" \n "))
    }

    @Test
    fun `line without separator is reported with its number`() {
        assertEquals(CustomHeadersParseResult.Invalid(2), CustomHeadersParser.parse("X-A: 1\nX-B 2"))
    }

    @Test
    fun `duplicate names are rejected regardless of case`() {
        assertEquals(CustomHeadersParseResult.Invalid(2), CustomHeadersParser.parse("X-A: 1\nx-a: 2"))
    }

    @Test
    fun `reserved names and empty values are rejected`() {
        assertEquals(CustomHeadersParseResult.Invalid(1), CustomHeadersParser.parse("Authorization: Bearer x"))
        assertEquals(CustomHeadersParseResult.Invalid(1), CustomHeadersParser.parse("X-A:"))
        assertEquals(CustomHeadersParseResult.Invalid(1), CustomHeadersParser.parse(": value"))
    }

    @Test
    fun `more than ten headers are rejected`() {
        val text = (1..11).joinToString("\n") { "X-$it: v" }

        assertEquals(CustomHeadersParseResult.Invalid(11), CustomHeadersParser.parse(text))
    }

    @Test
    fun `formatted headers parse back to the same headers`() {
        val headers = linkedMapOf("X-A" to "1", "X-B" to "two words")

        val formatted = CustomHeadersParser.format(headers)

        assertEquals(CustomHeadersParseResult.Valid(headers), CustomHeadersParser.parse(formatted))
    }
}
