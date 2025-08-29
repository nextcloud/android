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
    fun testIsNotBlankAndEqualsWhenGivenBothStringsAreNull() {
        val str1: String? = null
        val str2: String? = null
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testIsNotBlankAndEqualsWhenGivenFirstStringIsNull() {
        val str1: String? = null
        val str2 = "hello"
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testIsNotBlankAndEqualsWhenGivenSecondStringIsNull() {
        val str1 = "hello"
        val str2: String? = null
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testIsNotBlankAndEqualsWhenGivenBothStringsAreEmpty() {
        val str1 = ""
        val str2 = ""
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testIsNotBlankAndEqualsWhenGivenFirstStringIsEmpty() {
        val str1 = ""
        val str2 = "hello"
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testIsNotBlankAndEqualsWhenGivenSecondStringIsEmpty() {
        val str1 = "hello"
        val str2 = ""
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testIsNotBlankAndEqualsWhenGivenBothStringsAreWhitespaceOnly() {
        val str1 = "   "
        val str2 = "  \t  "
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testIsNotBlankAndEqualsWhenGivenFirstStringIsWhitespaceOnly() {
        val str1 = "   "
        val str2 = "hello"
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testIsNotBlankAndEqualsWhenGivenSecondStringIsWhitespaceOnly() {
        val str1 = "hello"
        val str2 = "   "
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testIsNotBlankAndEqualsWhenGivenStringsAreDifferentButBothValid() {
        val str1 = "hello"
        val str2 = "world"
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testIsNotBlankAndEqualsWhenGivenStringsHaveDifferentCase() {
        val str1 = "Hello"
        val str2 = "hello"
        assertTrue(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testIsNotBlankAndEqualsWhenGivenMixedCaseStrings() {
        val str1 = "HeLLo WoRLd"
        val str2 = "hello world"
        assertTrue(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testIsNotBlankAndEqualsWhenGivenUppercaseStrings() {
        val str1 = "HELLO"
        val str2 = "hello"
        assertTrue(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testIsNotBlankAndEqualsWhenGivenBothStringsAreIdenticalAndValid() {
        val str1 = "hello"
        val str2 = "hello"
        assertTrue(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testIsNotBlankAndEqualsWhenGivenBothStringsAreIdenticalWithSpaces() {
        val str1 = "hello world"
        val str2 = "hello world"
        assertTrue(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testIsNotBlankAndEqualsWhenGivenBothStringsAreIdenticalSingleCharacter() {
        val str1 = "a"
        val str2 = "A"
        assertTrue(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testIsNotBlankAndEqualsWhenGivenBothStringsAreIdenticalWithSpecialCharacters() {
        val str1 = "hello@world!123"
        val str2 = "HELLO@WORLD!123"
        assertTrue(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testIsNotBlankAndEqualsWhenGivenOneHasLeadingWhitespaceAndOtherDoesNot() {
        val str1 = " hello"
        val str2 = "HELLO"
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testIsNotBlankAndEqualsWhenGivenOneHasTrailingWhitespaceAndOtherDoesNot() {
        val str1 = "hello"
        val str2 = "HELLO "
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testIsNotBlankAndEqualsWhenGivenBothHaveIdenticalWhitespacePaddingDifferentCase() {
        val str1 = " hello "
        val str2 = " HELLO "
        assertTrue(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testIsNotBlankAndEqualsWhenGivenMixedWhitespaceCharacters() {
        val str1 = "\t"
        val str2 = "\n"
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testIsNotBlankAndEqualsWhenGivenOneIsNullAndOtherIsEmpty() {
        val str1: String? = null
        val str2 = ""
        assertFalse(str1.isNotBlankAndEquals(str2))
    }

    @Test
    fun testIsNotBlankAndEqualsWhenGivenOneIsNullAndOtherIsWhitespace() {
        val str1: String? = null
        val str2 = "   "
        assertFalse(str1.isNotBlankAndEquals(str2))
    }
}
