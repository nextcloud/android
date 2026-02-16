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

class RemotePathUtilsTest {

    @Test
    fun getFilename_normalFilePath_returnsFilename() {
        assertEquals("file.txt", RemotePathUtils.getFilename("/folder/file.txt"))
    }

    @Test
    fun getFilename_folderPath_returnsFolderName() {
        assertEquals("subfolder", RemotePathUtils.getFilename("/folder/subfolder/"))
    }

    @Test
    fun getFilename_rootPath_returnsEmpty() {
        assertEquals("", RemotePathUtils.getFilename("/"))
    }

    @Test
    fun getFilename_emptyString_returnsEmpty() {
        assertEquals("", RemotePathUtils.getFilename(""))
    }

    @Test
    fun getFilename_topLevelFile_returnsFilename() {
        assertEquals("file.txt", RemotePathUtils.getFilename("/file.txt"))
    }

    @Test
    fun getParentPath_filePath_returnsParent() {
        assertEquals("/folder/", RemotePathUtils.getParentPath("/folder/file.txt"))
    }

    @Test
    fun getParentPath_folderPath_returnsParent() {
        assertEquals("/folder/", RemotePathUtils.getParentPath("/folder/subfolder/"))
    }

    @Test
    fun getParentPath_topLevelFile_returnsRoot() {
        assertEquals("/", RemotePathUtils.getParentPath("/file.txt"))
    }

    @Test
    fun getParentPath_rootPath_returnsRoot() {
        assertEquals("/", RemotePathUtils.getParentPath("/"))
    }

    @Test
    fun getExtension_fileWithExtension_returnsExtension() {
        assertEquals("txt", RemotePathUtils.getExtension("/folder/file.txt"))
        assertEquals("pdf", RemotePathUtils.getExtension("/docs/report.pdf"))
    }

    @Test
    fun getExtension_fileWithMultipleExtensions_returnsLast() {
        assertEquals("gz", RemotePathUtils.getExtension("/archive.tar.gz"))
    }

    @Test
    fun getExtension_noExtension_returnsEmpty() {
        assertEquals("", RemotePathUtils.getExtension("/folder/README"))
    }

    @Test
    fun getExtension_dotFile_returnsExtension() {
        assertEquals("gitignore", RemotePathUtils.getExtension("/project/.gitignore"))
    }

    @Test
    fun getExtension_trailingDot_returnsEmpty() {
        assertEquals("", RemotePathUtils.getExtension("/folder/file."))
    }

    @Test
    fun getDepth_rootPath_returnsZero() {
        assertEquals(0, RemotePathUtils.getDepth("/"))
    }

    @Test
    fun getDepth_emptyPath_returnsZero() {
        assertEquals(0, RemotePathUtils.getDepth(""))
    }

    @Test
    fun getDepth_topLevelFolder_returnsOne() {
        assertEquals(1, RemotePathUtils.getDepth("/folder/"))
    }

    @Test
    fun getDepth_nestedFile_returnsCorrectDepth() {
        assertEquals(3, RemotePathUtils.getDepth("/a/b/c.txt"))
    }

    @Test
    fun getDepth_deeplyNestedFolder_returnsCorrectDepth() {
        assertEquals(4, RemotePathUtils.getDepth("/a/b/c/d/"))
    }

    @Test
    fun joinPath_normalJoin_correctResult() {
        assertEquals("/folder/file.txt", RemotePathUtils.joinPath("/folder", "file.txt"))
        assertEquals("/folder/file.txt", RemotePathUtils.joinPath("/folder/", "file.txt"))
    }

    @Test
    fun joinPath_rootAndChild_correctResult() {
        assertEquals("/file.txt", RemotePathUtils.joinPath("/", "file.txt"))
    }

    @Test
    fun joinPath_childWithLeadingSlash_slashTrimmed() {
        assertEquals("/folder/file.txt", RemotePathUtils.joinPath("/folder", "/file.txt"))
    }

    @Test
    fun normalizePath_duplicateSlashes_collapsed() {
        assertEquals("/folder/file.txt", RemotePathUtils.normalizePath("//folder///file.txt"))
    }

    @Test
    fun normalizePath_missingLeadingSlash_added() {
        assertEquals("/folder/file.txt", RemotePathUtils.normalizePath("folder/file.txt"))
    }

    @Test
    fun normalizePath_blankPath_returnsRoot() {
        assertEquals("/", RemotePathUtils.normalizePath(""))
        assertEquals("/", RemotePathUtils.normalizePath("   "))
    }

    @Test
    fun normalizePath_alreadyNormal_unchanged() {
        assertEquals("/folder/file.txt", RemotePathUtils.normalizePath("/folder/file.txt"))
    }

    @Test
    fun isFolderPath_folderPath_returnsTrue() {
        assertTrue(RemotePathUtils.isFolderPath("/folder/"))
        assertTrue(RemotePathUtils.isFolderPath("/"))
    }

    @Test
    fun isFolderPath_filePath_returnsFalse() {
        assertFalse(RemotePathUtils.isFolderPath("/folder/file.txt"))
    }

    @Test
    fun getAncestorPaths_rootPath_returnsEmpty() {
        assertEquals(emptyList<String>(), RemotePathUtils.getAncestorPaths("/"))
    }

    @Test
    fun getAncestorPaths_topLevelFile_returnsRoot() {
        assertEquals(listOf("/"), RemotePathUtils.getAncestorPaths("/file.txt"))
    }

    @Test
    fun getAncestorPaths_nestedFile_returnsAllAncestors() {
        val expected = listOf("/", "/a/", "/a/b/")
        assertEquals(expected, RemotePathUtils.getAncestorPaths("/a/b/c.txt"))
    }

    @Test
    fun getAncestorPaths_deeplyNestedFolder_returnsAllAncestors() {
        val expected = listOf("/", "/a/", "/a/b/", "/a/b/c/")
        assertEquals(expected, RemotePathUtils.getAncestorPaths("/a/b/c/d/"))
    }
}
