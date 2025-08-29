/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.extensions

import com.nextcloud.utils.extensions.isNotBlankAndEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class StringExtensionTests {
    @Test
    fun testisNotBlankAndEqualsWhenGivenBothStringsAreNull() {
        val str1: String? = null
        val str2: String? = null
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testisNotBlankAndEqualsWhenGivenFirstStringIsNull() {
        val str1: String? = null
        val str2 = "hello"
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testisNotBlankAndEqualsWhenGivenSecondStringIsNull() {
        val str1 = "hello"
        val str2: String? = null
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testisNotBlankAndEqualsWhenGivenBothStringsAreEmpty() {
        val str1 = ""
        val str2 = ""
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testisNotBlankAndEqualsWhenGivenFirstStringIsEmpty() {
        val str1 = ""
        val str2 = "hello"
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testisNotBlankAndEqualsWhenGivenSecondStringIsEmpty() {
        val str1 = "hello"
        val str2 = ""
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testisNotBlankAndEqualsWhenGivenBothStringsAreWhitespaceOnly() {
        val str1 = "   "
        val str2 = "  \t  "
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testisNotBlankAndEqualsWhenGivenFirstStringIsWhitespaceOnly() {
        val str1 = "   "
        val str2 = "hello"
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testisNotBlankAndEqualsWhenGivenSecondStringIsWhitespaceOnly() {
        val str1 = "hello"
        val str2 = "   "
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testisNotBlankAndEqualsWhenGivenStringsAreDifferentButBothValid() {
        val str1 = "hello"
        val str2 = "world"
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testisNotBlankAndEqualsWhenGivenStringsHaveDifferentCase() {
        val str1 = "Hello"
        val str2 = "hello"
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testisNotBlankAndEqualsWhenGivenBothStringsAreIdenticalAndValid() {
        val str1 = "hello"
        val str2 = "hello"
        assertTrue(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testisNotBlankAndEqualsWhenGivenBothStringsAreIdenticalWithSpaces() {
        val str1 = "hello world"
        val str2 = "hello world"
        assertTrue(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testisNotBlankAndEqualsWhenGivenBothStringsAreIdenticalSingleCharacter() {
        val str1 = "a"
        val str2 = "a"
        assertTrue(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testisNotBlankAndEqualsWhenGivenBothStringsAreIdenticalWithSpecialCharacters() {
        val str1 = "hello@world!123"
        val str2 = "hello@world!123"
        assertTrue(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testisNotBlankAndEqualsWhenGivenOneHasLeadingWhitespaceAndOtherDoesNot() {
        val str1 = " hello"
        val str2 = "hello"
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testisNotBlankAndEqualsWhenGivenOneHasTrailingWhitespaceAndOtherDoesNot() {
        val str1 = "hello"
        val str2 = "hello "
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testisNotBlankAndEqualsWhenGivenBothHaveIdenticalWhitespacePadding() {
        val str1 = " hello "
        val str2 = " hello "
        assertTrue(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testisNotBlankAndEqualsWhenGivenMixedWhitespaceCharacters() {
        val str1 = "\t"
        val str2 = "\n"
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testisNotBlankAndEqualsWhenGivenOneIsNullAndOtherIsEmpty() {
        val str1: String? = null
        val str2 = ""
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testisNotBlankAndEqualsWhenGivenOneIsNullAndOtherIsWhitespace() {
        val str1: String? = null
        val str2 = "   "
        assertFalse(str1.isNotBlankAndEquals(str2))
    }
}
