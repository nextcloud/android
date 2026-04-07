/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.utils

import com.owncloud.android.AbstractIT
import androidx.test.platform.app.InstrumentationRegistry
import com.owncloud.android.utils.FileUtil.isFolderWritable
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class FileUtilTest : AbstractIT() {

    private lateinit var context: android.content.Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun isFolderWritable_returnsTrueForInternalCache() = runBlocking {
        val writableDir = context.cacheDir
        val result = isFolderWritable(writableDir)
        assertTrue("Internal cache directory should be writable", result)
    }

    @Test
    fun isFolderWritable_returnsFalseForNonExistentFile() = runBlocking {
        val nonExistentFile = File(context.cacheDir, "ghost_folder_123")

        val result = isFolderWritable(nonExistentFile)

        assertFalse("Non-existent folder should not be writable", result)
    }

    @Test
    fun isFolderWritable_returnsFalseForRegularFile() = runBlocking {
        // Create a regular file, not a directory
        val regularFile = File(context.cacheDir, "test_file.txt")
        regularFile.createNewFile()

        val result = isFolderWritable(regularFile)

        assertFalse("A regular file should not be treated as a writable folder", result)
    }

    @Test
    fun isFolderWritable_returnsFalseForNull() = runBlocking {
        val result = isFolderWritable(null)

        assertFalse("Null input should return false", result)
    }

    @Test
    fun isFolderWritable_returnsFalseForReadOnlyDirectory() = runBlocking {
        val readOnlyDir = File(context.cacheDir, "readonly_test")
        readOnlyDir.mkdir()

        try {
            // Set directory to read-only
            readOnlyDir.setReadOnly()

            val result = isFolderWritable(readOnlyDir)

            assertFalse("Read-only directory should return false", result)
        } finally {
            // Cleanup: restore write permission so it can be deleted
            readOnlyDir.setWritable(true)
            readOnlyDir.delete()
        }
    }
}
