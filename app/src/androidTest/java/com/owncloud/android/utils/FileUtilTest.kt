/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import com.owncloud.android.AbstractIT
import org.junit.Assert
import org.junit.Test
import java.io.File

class FileUtilTest : AbstractIT() {
    @Test
    fun assertNullInput() {
        Assert.assertEquals("", FileUtil.getFilenameFromPathString(null))
    }

    @Test
    fun assertEmptyInput() {
        Assert.assertEquals("", FileUtil.getFilenameFromPathString(""))
    }

    @Test
    fun assertFileInput() {
        val file = getDummyFile("empty.txt")
        Assert.assertEquals("empty.txt", FileUtil.getFilenameFromPathString(file.absolutePath))
    }

    @Test
    fun assertSlashInput() {
        val tempPath = File(FileStorageUtils.getTemporalPath(account.name) + File.pathSeparator + "folder")
        if (!tempPath.exists()) {
            Assert.assertTrue(tempPath.mkdirs())
        }
        Assert.assertEquals("", FileUtil.getFilenameFromPathString(tempPath.absolutePath))
    }

    @Test
    fun assertDotFileInput() {
        val file = getDummyFile(".dotfile.ext")
        Assert.assertEquals(".dotfile.ext", FileUtil.getFilenameFromPathString(file.absolutePath))
    }

    @Test
    fun assertFolderInput() {
        val tempPath = File(FileStorageUtils.getTemporalPath(account.name))
        if (!tempPath.exists()) {
            Assert.assertTrue(tempPath.mkdirs())
        }

        Assert.assertEquals("", FileUtil.getFilenameFromPathString(tempPath.absolutePath))
    }

    @Test
    fun assertNoFileExtensionInput() {
        val file = getDummyFile("file")
        Assert.assertEquals("file", FileUtil.getFilenameFromPathString(file.absolutePath))
    }
}
