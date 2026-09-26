/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import com.nextcloud.utils.extensions.forbiddenFilenameBaseNames
import com.nextcloud.utils.extensions.forbiddenFilenameCharacters
import com.nextcloud.utils.extensions.forbiddenFilenameExtensions
import com.nextcloud.utils.extensions.forbiddenFilenames
import com.owncloud.android.AbstractIT
import com.owncloud.android.lib.resources.status.OCCapability
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

@Suppress("MagicNumber", "TooManyFunctions")
class OCCapabilityJsonToListTests : AbstractIT() {
    private var capability: OCCapability = fileDataStorageManager.getCapability(account.name)

    // region Valid Input
    @Test
    fun testForbiddenFilenamesParsedCorrectly() {
        capability.forbiddenFilenamesJson = """[".htaccess", ".htaccess"]"""
        val result = capability.forbiddenFilenames()
        assertEquals(listOf(".htaccess", ".htaccess"), result)
    }

    @Test
    fun testForbiddenFilenameBaseNamesParsedCorrectly() {
        capability.forbiddenFilenameBaseNamesJson = """["con", "prn", "aux"]"""
        val result = capability.forbiddenFilenameBaseNames()
        assertEquals(listOf("con", "prn", "aux"), result)
    }

    @Test
    fun testForbiddenFilenameExtensionsParsedCorrectly() {
        capability.forbiddenFilenameExtensionJson = """[" ",".",".part"]"""
        val result = capability.forbiddenFilenameExtensions()
        assertEquals(listOf(" ", ".", ".part"), result)
    }

    @Test
    fun testForbiddenFilenameCharactersParsedCorrectly() {
        capability.forbiddenFilenameCharactersJson = """["<", ">", ":", "\\", "/", "|", "?", "*", "&"]"""
        val result = capability.forbiddenFilenameCharacters()
        assertEquals(listOf("<", ">", ":", "\\", "/", "|", "?", "*", "&"), result)
    }

    @Test
    fun testEmptyArrayReturnsEmptyList() {
        capability.forbiddenFilenamesJson = """[]"""
        val result = capability.forbiddenFilenames()
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun testSingleElementArray() {
        capability.forbiddenFilenamesJson = """[".htaccess"]"""
        val result = capability.forbiddenFilenames()
        assertEquals(listOf(".htaccess"), result)
    }

    @Test
    fun testArrayWithWhitespaceAroundJson() {
        capability.forbiddenFilenameBaseNamesJson = """
            ["con", "prn", "aux", "nul", "com0", "com1", "com2", "com3", "com4",
            "com5", "com6", "com7", "com8", "com9", "com¹", "com²", "com³",
            "lpt0", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7",
            "lpt8", "lpt9", "lpt¹", "lpt²", "lpt³"]
            """
        val result = capability.forbiddenFilenameBaseNames()
        assertEquals(30, result.size)
        assertTrue(result.contains("con"))
        assertTrue(result.contains("lpt³"))
    }

    @Test
    fun testUnicodeCharactersPreserved() {
        capability.forbiddenFilenameBaseNamesJson = """["com¹", "com²", "com³", "lpt¹", "lpt²", "lpt³"]"""
        val result = capability.forbiddenFilenameBaseNames()
        assertEquals(listOf("com¹", "com²", "com³", "lpt¹", "lpt²", "lpt³"), result)
    }

    @Test
    fun testDuplicateEntriesPreserved() {
        capability.forbiddenFilenameExtensionJson = """[".part", ".part"]"""
        val result = capability.forbiddenFilenameExtensions()
        assertEquals(listOf(".part", ".part"), result)
    }
    // endregion

    // region Null and Blank Input
    @Test
    fun testNullJsonReturnsEmptyList() {
        capability.forbiddenFilenamesJson = null
        val result = capability.forbiddenFilenames()
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun testBlankJsonReturnsEmptyList() {
        capability.forbiddenFilenamesJson = "   "
        val result = capability.forbiddenFilenames()
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun testEmptyStringJsonReturnsEmptyList() {
        capability.forbiddenFilenamesJson = ""
        val result = capability.forbiddenFilenames()
        assertEquals(emptyList<String>(), result)
    }
    // endregion

    // region Malformed Input
    @Test
    fun testMalformedJsonReturnsEmptyList() {
        capability.forbiddenFilenamesJson = """[".htaccess", """
        val result = capability.forbiddenFilenames()
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun testNonArrayJsonObjectReturnsEmptyList() {
        capability.forbiddenFilenamesJson = """{"key": "value"}"""
        val result = capability.forbiddenFilenames()
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun testPlainStringJsonReturnsEmptyList() {
        capability.forbiddenFilenamesJson = """.htaccess"""
        val result = capability.forbiddenFilenames()
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun testHtmlErrorPageReturnsEmptyList() {
        capability.forbiddenFilenamesJson = "<html><body>Internal Server Error</body></html>"
        val result = capability.forbiddenFilenames()
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun testJsonNullLiteralReturnsEmptyList() {
        capability.forbiddenFilenamesJson = "null"
        val result = capability.forbiddenFilenames()
        assertEquals(emptyList<String>(), result)
    }

    // endregion

    // region Oversized Input
    @Test
    fun testOversizedJsonReturnsEmptyList() {
        val hugeEntry = "a".repeat(1024)
        val entries = Array(600) { """"$hugeEntry"""" }
        capability.forbiddenFilenamesJson = "[${entries.joinToString(",")}]"
        val result = capability.forbiddenFilenames()
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun testJsonJustUnderSizeLimitIsParsed() {
        val entries = Array(100) { i -> """"entry$i"""" }
        capability.forbiddenFilenamesJson = "[${entries.joinToString(",")}]"
        val result = capability.forbiddenFilenames()
        assertEquals(100, result.size)
        assertEquals("entry0", result[0])
        assertEquals("entry99", result[99])
    }
    // endregion
}
