/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StringExtensionsTest {

    @Test
    fun getRandomString_generatesCorrectLength() {
        val result = "prefix".getRandomString(10)
        assertEquals(16, result.length) // "prefix" (6) + 10 random chars
    }

    @Test
    fun getRandomString_preservesPrefix() {
        val prefix = "test_"
        val result = prefix.getRandomString(5)
        assertTrue(result.startsWith(prefix))
    }

    @Test
    fun getRandomString_zeroLength_returnsOriginal() {
        val result = "hello".getRandomString(0)
        assertEquals("hello", result)
    }

    @Test
    fun getRandomString_containsOnlyAlphanumeric() {
        val result = "".getRandomString(100)
        val allowedPattern = Regex("^[A-Za-z0-9]*$")
        assertTrue(allowedPattern.matches(result))
    }

    @Test
    fun removeFileExtension_withExtension_removesIt() {
        assertEquals("document", "document.pdf".removeFileExtension())
        assertEquals("image", "image.png".removeFileExtension())
    }

    @Test
    fun removeFileExtension_multipleExtensions_removesLast() {
        assertEquals("archive.tar", "archive.tar.gz".removeFileExtension())
    }

    @Test
    fun removeFileExtension_noExtension_returnsOriginal() {
        assertEquals("README", "README".removeFileExtension())
    }

    @Test
    fun removeFileExtension_dotAtStart_removesExtension() {
        // ".hidden" -> "" (dot at index 0, so dotIndex is 0, substring(0,0) = "")
        // Actually dotIndex is 0, and the condition is dotIndex != -1, so it returns substring(0,0) = ""
        assertEquals("", ".hidden".removeFileExtension())
    }

    @Test
    fun removeFileExtension_emptyString_returnsEmpty() {
        assertEquals("", "".removeFileExtension())
    }

    @Test
    fun eTagChanged_bothNull_returnsTrue() {
        assertTrue(null.eTagChanged(null))
    }

    @Test
    fun eTagChanged_thisNull_returnsTrue() {
        assertTrue(null.eTagChanged("abc"))
    }

    @Test
    fun eTagChanged_serverNull_returnsTrue() {
        assertTrue("abc".eTagChanged(null))
    }

    @Test
    fun eTagChanged_bothEmpty_returnsTrue() {
        assertTrue("".eTagChanged(""))
    }

    @Test
    fun eTagChanged_sameValue_returnsFalse() {
        assertFalse("abc123".eTagChanged("abc123"))
    }

    @Test
    fun eTagChanged_sameValueDifferentCase_returnsFalse() {
        assertFalse("ABC123".eTagChanged("abc123"))
    }

    @Test
    fun eTagChanged_differentValues_returnsTrue() {
        assertTrue("abc".eTagChanged("xyz"))
    }

    @Test
    fun truncateWithEllipsis_shortString_noEllipsis() {
        assertEquals("hi", "hi".truncateWithEllipsis(10))
    }

    @Test
    fun truncateWithEllipsis_exactLength_noEllipsis() {
        assertEquals("hello", "hello".truncateWithEllipsis(5))
    }

    @Test
    fun truncateWithEllipsis_longString_truncatesWithEllipsis() {
        assertEquals("hel...", "hello world".truncateWithEllipsis(3))
    }

    @Test
    fun truncateWithEllipsis_zeroLimit_returnsEllipsisOnly() {
        assertEquals("...", "hello".truncateWithEllipsis(0))
    }
}
