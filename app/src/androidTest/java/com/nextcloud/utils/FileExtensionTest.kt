/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import com.nextcloud.utils.fileNameValidator.FileNameValidator.isExtensionChanged
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class FileExtensionTest {

    @Test
    fun sameExtensionReturnsFalse() {
        assertFalse(isExtensionChanged("file.txt", "other.txt"))
    }

    @Test
    fun differentExtensionReturnsTrue() {
        assertTrue(isExtensionChanged("file.txt", "file.pdf"))
    }

    @Test
    fun caseDifferenceDoesNotTriggerChange() {
        assertFalse(isExtensionChanged("file.JPG", "file.jpg"))
    }

    @Test
    fun bothWithoutExtensionReturnsFalse() {
        assertFalse(isExtensionChanged("README", "LICENSE"))
    }

    @Test
    fun noExtensionToExtensionReturnsTrue() {
        assertTrue(isExtensionChanged("README", "file.txt"))
    }

    @Test
    fun extensionToNoExtensionReturnsTrue() {
        assertTrue(isExtensionChanged("file.txt", "README"))
    }

    @Test
    fun hiddenFilesWithoutExtensionReturnFalse() {
        assertFalse(isExtensionChanged(".gitignore", ".env"))
    }

    @Test
    fun hiddenFileToNormalExtensionReturnsTrue() {
        assertTrue(isExtensionChanged(".gitignore", "file.txt"))
    }

    @Test
    fun multipleDotsSameLastExtensionReturnsFalse() {
        assertFalse(isExtensionChanged("archive.tar.gz", "backup.gz"))
    }

    @Test
    fun multipleDotsDifferentLastExtensionReturnsTrue() {
        assertTrue(isExtensionChanged("archive.tar.gz", "archive.tar.zip"))
    }

    @Test
    fun trailingDotTreatedAsNoExtensionReturnsTrue() {
        assertTrue(isExtensionChanged("file.", "file.txt"))
    }

    @Test
    fun bothTrailingDotReturnFalse() {
        assertFalse(isExtensionChanged("file.", "another."))
    }

    @Test
    fun emptyStringsReturnFalse() {
        assertFalse(isExtensionChanged("", ""))
    }

    @Test
    fun emptyStringToExtensionReturnsTrue() {
        assertTrue(isExtensionChanged("", "file.txt"))
    }

    @Test
    fun bothNullReturnFalse() {
        assertFalse(isExtensionChanged(null, null))
    }

    @Test
    fun previousNullNewNotNullReturnsTrue() {
        assertTrue(isExtensionChanged(null, "file.txt"))
    }

    @Test
    fun previousNotNullNewNullReturnsTrue() {
        assertTrue(isExtensionChanged("file.txt", null))
    }

    @Test
    fun singleDotFilenameReturnsFalse() {
        assertFalse(isExtensionChanged(".", "."))
    }

    @Test
    fun dotToExtensionReturnsTrue() {
        assertTrue(isExtensionChanged(".", "file.txt"))
    }

    @Test
    fun filenamesEndingWithDotReturnFalse() {
        assertFalse(isExtensionChanged("test.", "another."))
    }
}
