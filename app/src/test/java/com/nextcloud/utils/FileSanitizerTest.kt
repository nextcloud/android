/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileSanitizerTest {

    @Test
    fun sanitizeFilename_blankInput_returnsPlaceholder() {
        assertEquals("_", FileSanitizer.sanitizeFilename(""))
        assertEquals("_", FileSanitizer.sanitizeFilename("   "))
    }

    @Test
    fun sanitizeFilename_normalFilename_remainsUnchanged() {
        assertEquals("document.pdf", FileSanitizer.sanitizeFilename("document.pdf"))
        assertEquals("my-photo_2024", FileSanitizer.sanitizeFilename("my-photo_2024"))
    }

    @Test
    fun sanitizeFilename_unsafeCharacters_areReplaced() {
        assertEquals("file_name", FileSanitizer.sanitizeFilename("file<name"))
        assertEquals("file_name", FileSanitizer.sanitizeFilename("file>name"))
        assertEquals("file_name", FileSanitizer.sanitizeFilename("file:name"))
        assertEquals("file_name", FileSanitizer.sanitizeFilename("file\"name"))
        assertEquals("file_name", FileSanitizer.sanitizeFilename("file|name"))
        assertEquals("file_name", FileSanitizer.sanitizeFilename("file?name"))
        assertEquals("file_name", FileSanitizer.sanitizeFilename("file*name"))
    }

    @Test
    fun sanitizeFilename_pathSeparators_areReplaced() {
        assertEquals("file_name", FileSanitizer.sanitizeFilename("file/name"))
        assertEquals("file_name", FileSanitizer.sanitizeFilename("file\\name"))
    }

    @Test
    fun sanitizeFilename_leadingDotsAndSpaces_areStripped() {
        assertEquals("hidden", FileSanitizer.sanitizeFilename("...hidden"))
        assertEquals("file", FileSanitizer.sanitizeFilename("  file"))
        assertEquals("file", FileSanitizer.sanitizeFilename("file..."))
        assertEquals("file", FileSanitizer.sanitizeFilename("file  "))
    }

    @Test
    fun sanitizeFilename_windowsReservedNames_arePrefixed() {
        assertTrue(FileSanitizer.sanitizeFilename("CON").startsWith("_"))
        assertTrue(FileSanitizer.sanitizeFilename("PRN.txt").startsWith("_"))
        assertTrue(FileSanitizer.sanitizeFilename("NUL").startsWith("_"))
        assertTrue(FileSanitizer.sanitizeFilename("COM1").startsWith("_"))
        assertTrue(FileSanitizer.sanitizeFilename("LPT3").startsWith("_"))
    }

    @Test
    fun sanitizeFilename_controlCharacters_areReplaced() {
        assertEquals("file_name", FileSanitizer.sanitizeFilename("file\u0000name"))
        assertEquals("file_name", FileSanitizer.sanitizeFilename("file\u001Fname"))
        assertEquals("file_name", FileSanitizer.sanitizeFilename("file\tname"))
    }

    @Test
    fun sanitizeRemotePath_normalPath_remainsUnchanged() {
        assertEquals("/folder/subfolder/file.txt", FileSanitizer.sanitizeRemotePath("/folder/subfolder/file.txt"))
    }

    @Test
    fun sanitizeRemotePath_blankPath_returnsRoot() {
        assertEquals("/", FileSanitizer.sanitizeRemotePath(""))
        assertEquals("/", FileSanitizer.sanitizeRemotePath("   "))
    }

    @Test
    fun sanitizeRemotePath_pathWithUnsafeSegments_areSanitized() {
        assertEquals("/folder/_file/doc.txt", FileSanitizer.sanitizeRemotePath("/folder/<file/doc.txt"))
    }

    @Test
    fun containsPathTraversal_normalPaths_returnFalse() {
        assertFalse(FileSanitizer.containsPathTraversal("folder/file.txt"))
        assertFalse(FileSanitizer.containsPathTraversal("file.txt"))
        assertFalse(FileSanitizer.containsPathTraversal(""))
    }

    @Test
    fun containsPathTraversal_traversalPaths_returnTrue() {
        assertTrue(FileSanitizer.containsPathTraversal("../etc/passwd"))
        assertTrue(FileSanitizer.containsPathTraversal("folder/../../secret"))
        assertTrue(FileSanitizer.containsPathTraversal("..\\windows\\system32"))
    }

    @Test
    fun containsPathTraversal_singleDotInPath_returnTrue() {
        assertTrue(FileSanitizer.containsPathTraversal("./file.txt"))
    }

    @Test
    fun isWindowsReservedName_reservedNames_returnTrue() {
        assertTrue(FileSanitizer.isWindowsReservedName("CON"))
        assertTrue(FileSanitizer.isWindowsReservedName("con"))
        assertTrue(FileSanitizer.isWindowsReservedName("PRN.txt"))
        assertTrue(FileSanitizer.isWindowsReservedName("AUX"))
        assertTrue(FileSanitizer.isWindowsReservedName("NUL"))
        assertTrue(FileSanitizer.isWindowsReservedName("COM1"))
        assertTrue(FileSanitizer.isWindowsReservedName("LPT9"))
    }

    @Test
    fun isWindowsReservedName_normalNames_returnFalse() {
        assertFalse(FileSanitizer.isWindowsReservedName("document.txt"))
        assertFalse(FileSanitizer.isWindowsReservedName("CONNECTION"))
        assertFalse(FileSanitizer.isWindowsReservedName("my_aux_file"))
    }

    @Test
    fun truncateFilename_shortFilename_remainsUnchanged() {
        assertEquals("file.txt", FileSanitizer.truncateFilename("file.txt", 250))
    }

    @Test
    fun truncateFilename_longFilename_isTruncatedPreservingExtension() {
        val longName = "a".repeat(300) + ".pdf"
        val result = FileSanitizer.truncateFilename(longName, 20)
        assertTrue(result.length <= 20)
        assertTrue(result.endsWith(".pdf"))
    }

    @Test
    fun truncateFilename_noExtension_simpleTruncation() {
        val longName = "a".repeat(300)
        val result = FileSanitizer.truncateFilename(longName, 50)
        assertEquals(50, result.length)
    }

    @Test
    fun truncateFilename_veryLongExtension_truncatesWhole() {
        val name = "a" + ".".padEnd(300, 'x')
        val result = FileSanitizer.truncateFilename(name, 10)
        assertEquals(10, result.length)
    }

    @Test
    fun containsBidiCharacters_normalText_returnsFalse() {
        assertFalse(FileSanitizer.containsBidiCharacters("normal_file.txt"))
        assertFalse(FileSanitizer.containsBidiCharacters(""))
    }

    @Test
    fun containsBidiCharacters_bidiText_returnsTrue() {
        assertTrue(FileSanitizer.containsBidiCharacters("file\u202Ename.txt"))
        assertTrue(FileSanitizer.containsBidiCharacters("file\u200Fname.txt"))
        assertTrue(FileSanitizer.containsBidiCharacters("file\u2066name.txt"))
        assertTrue(FileSanitizer.containsBidiCharacters("file\u061Cname.txt"))
    }

    @Test
    fun removeBidiCharacters_cleanText_remainsUnchanged() {
        assertEquals("file.txt", FileSanitizer.removeBidiCharacters("file.txt"))
    }

    @Test
    fun removeBidiCharacters_bidiText_charactersRemoved() {
        assertEquals("filename.txt", FileSanitizer.removeBidiCharacters("file\u202Ename.txt"))
        assertEquals("filename.txt", FileSanitizer.removeBidiCharacters("file\u200Fname.txt"))
        assertEquals("filename.txt", FileSanitizer.removeBidiCharacters("file\u2066name.txt"))
    }

    @Test
    fun removeBidiCharacters_multipleBidiChars_allRemoved() {
        assertEquals("abc", FileSanitizer.removeBidiCharacters("\u200Ea\u200Fb\u202Ac"))
    }

    @Test
    fun generateUniqueFilename_noConflict_returnsOriginal() {
        val existing = setOf("other.txt", "another.doc")
        assertEquals("file.txt", FileSanitizer.generateUniqueFilename("file.txt", existing))
    }

    @Test
    fun generateUniqueFilename_singleConflict_appendsSuffix() {
        val existing = setOf("file.txt")
        assertEquals("file (2).txt", FileSanitizer.generateUniqueFilename("file.txt", existing))
    }

    @Test
    fun generateUniqueFilename_multipleConflicts_incrementsSuffix() {
        val existing = setOf("file.txt", "file (2).txt", "file (3).txt")
        assertEquals("file (4).txt", FileSanitizer.generateUniqueFilename("file.txt", existing))
    }

    @Test
    fun generateUniqueFilename_noExtension_appendsSuffix() {
        val existing = setOf("README", "README (2)")
        assertEquals("README (3)", FileSanitizer.generateUniqueFilename("README", existing))
    }

    @Test
    fun generateUniqueFilename_emptyExistingSet_returnsOriginal() {
        assertEquals("file.txt", FileSanitizer.generateUniqueFilename("file.txt", emptySet()))
    }
}
