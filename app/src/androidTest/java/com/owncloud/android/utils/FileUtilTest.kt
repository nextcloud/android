/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import com.owncloud.android.AbstractIT
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class FileUtilTest : AbstractIT() {
    @Test
    fun assertNullInput() {
        assertEquals("", FileUtil.getFilenameFromPathString(null))
    }

    @Test
    fun assertEmptyInput() {
        assertEquals("", FileUtil.getFilenameFromPathString(""))
    }

    @Test
    fun assertFileInput() {
        val file = getDummyFile("empty.txt")
        assertEquals("empty.txt", FileUtil.getFilenameFromPathString(file.absolutePath))
    }

    @Test
    fun assertSlashInput() {
        val tempPath = File(FileStorageUtils.getTemporalPath(account.name) + File.pathSeparator + "folder")
        if (!tempPath.exists()) {
            Assert.assertTrue(tempPath.mkdirs())
        }
        assertEquals("", FileUtil.getFilenameFromPathString(tempPath.absolutePath))
    }

    @Test
    fun assertDotFileInput() {
        val file = getDummyFile(".dotfile.ext")
        assertEquals(".dotfile.ext", FileUtil.getFilenameFromPathString(file.absolutePath))
    }

    @Test
    fun assertFolderInput() {
        val tempPath = File(FileStorageUtils.getTemporalPath(account.name))
        if (!tempPath.exists()) {
            Assert.assertTrue(tempPath.mkdirs())
        }

        assertEquals("", FileUtil.getFilenameFromPathString(tempPath.absolutePath))
    }

    @Test
    fun assertNoFileExtensionInput() {
        val file = getDummyFile("file")
        assertEquals("file", FileUtil.getFilenameFromPathString(file.absolutePath))
    }

    @Test
    fun testGetRemotePathVariantsWithUppercaseExtension() {
        val path = "/TesTFolder/abc.JPG"
        val expected = Pair("/TesTFolder/abc.jpg", "/TesTFolder/abc.JPG")
        val actual = FileUtil.getRemotePathVariants(path)
        assertEquals(expected, actual)
    }

    @Test
    fun testGetRemotePathVariantsWithLowercaseExtension() {
        val path = "/TesTFolder/abc.png"
        val expected = Pair("/TesTFolder/abc.png", "/TesTFolder/abc.PNG")
        val actual = FileUtil.getRemotePathVariants(path)
        assertEquals(expected, actual)
    }

    @Test
    fun testGetRemotePathVariantsMixedCaseExtension() {
        val path = "/TesTFolder/abc.JpEg"
        val expected = Pair("/TesTFolder/abc.jpeg", "/TesTFolder/abc.JPEG")
        val actual = FileUtil.getRemotePathVariants(path)
        assertEquals(expected, actual)
    }

    @Test
    fun testGetRemotePathVariantsNoExtension() {
        val path = "/TesTFolder/abc"
        val expected = Pair(path, path)
        val actual = FileUtil.getRemotePathVariants(path)
        assertEquals(expected, actual)
    }

    @Test
    fun testGetRemotePathVariantsDotAtEnd() {
        val path = "/TesTFolder/abc."
        val expected = Pair(path, path)
        val actual = FileUtil.getRemotePathVariants(path)
        assertEquals(expected, actual)
    }
}
