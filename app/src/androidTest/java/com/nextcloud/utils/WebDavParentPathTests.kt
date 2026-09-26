/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import com.nextcloud.utils.extensions.webDavParentPath
import org.junit.Assert.assertEquals
import org.junit.Test

@Suppress("TooManyFunctions")
class WebDavParentPathTests {

    @Test
    fun testWebDavParentPathWhenGivenCorrectParentShouldReturnOneLevelAbove() {
        assertEquals("/Photos/Vacation/", "/Photos/Vacation/beach.jpg".webDavParentPath())
        assertEquals("/work/docs/", "/work/docs/notes.txt".webDavParentPath())
    }

    @Test
    fun testWebDavParentPathWhenGivenDeepNestingShouldReturnDirectParent() {
        assertEquals("/a/b/c/d/", "/a/b/c/d/e.txt".webDavParentPath())
    }

    @Test
    fun testWebDavParentPathWhenGivenRootFileShouldReturnRoot() {
        assertEquals("/", "/image.png".webDavParentPath())
    }

    @Test
    fun testWebDavParentPathWhenGivenSlashShouldReturnRoot() {
        assertEquals("/", "/".webDavParentPath())
    }

    @Test
    fun testWebDavParentPathWhenGivenEmptyStringShouldReturnRoot() {
        assertEquals("/", "".webDavParentPath())
    }

    @Test
    fun testWebDavParentPathWhenGivenOnlySlashesShouldReturnRoot() {
        assertEquals("/", "///".webDavParentPath())
    }

    @Test
    fun testWebDavParentPathWhenGivenRelativePathShouldReturnOneLevelAbove() {
        assertEquals("Documents/", "Documents/file.pdf".webDavParentPath())
    }

    @Test
    fun testWebDavParentPathWhenGivenSingleWordPathShouldReturnRoot() {
        assertEquals("/", "readme.md".webDavParentPath())
    }

    @Test
    fun testWebDavParentPathWhenGivenTrailingSlashShouldReturnOneLevelAbove() {
        assertEquals("/Photos/", "/Photos/Vacation/".webDavParentPath())
    }

    @Test
    fun testWebDavParentPathWhenGivenMultipleTrailingSlashesShouldReturnOneLevelAbove() {
        assertEquals("/Photos/", "/Photos/Vacation///".webDavParentPath())
    }

    @Test
    fun testWebDavParentPathWhenGivenEncodedSpacesShouldPreserveEncoding() {
        assertEquals("/My%20Photos/", "/My%20Photos/beach%20photo.jpg".webDavParentPath())
    }

    @Test
    fun testWebDavParentPathWhenGivenEncodedSpecialCharsShouldPreserveEncoding() {
        assertEquals("/files/%23reports/", "/files/%23reports/q1%262.pdf".webDavParentPath())
    }

    @Test
    fun testWebDavParentPathWhenGivenUnicodeCharsShouldReturnOneLevelAbove() {
        assertEquals("/照片/假期/", "/照片/假期/海滩.jpg".webDavParentPath())
    }

    @Test
    fun testWebDavParentPathWhenGivenSingleCharFileAtRootShouldReturnRoot() {
        assertEquals("/", "/a".webDavParentPath())
    }

    @Test
    fun testWebDavParentPathWhenGivenSingleCharDirShouldReturnOneLevelAbove() {
        assertEquals("/a/", "/a/b".webDavParentPath())
    }
}
