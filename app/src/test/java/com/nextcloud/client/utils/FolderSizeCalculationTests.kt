/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.utils

import com.owncloud.android.utils.FileStorageUtils
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File

@Suppress("TooManyFunctions", "MagicNumber")
class FolderSizeCalculationTests {

    private lateinit var testDir: File

    @Before
    fun setUp() {
        testDir = File(System.getProperty("java.io.tmpdir"), "test_folder_${System.currentTimeMillis()}")
        testDir.mkdirs()
    }

    @After
    fun tearDown() {
        testDir.deleteRecursively()
    }

    @Test
    fun testReturnZeroWhenGivenNullDirectory() {
        val result = FileStorageUtils.getFolderSize(null)
        assertEquals(0L, result)
    }

    @Test
    fun testReturnZeroWhenGivenNonExistentDirectory() {
        val nonExistent = File(testDir, "does_not_exist")
        val result = FileStorageUtils.getFolderSize(nonExistent)
        assertEquals(0L, result)
    }

    @Test
    fun testReturnZeroWhenGivenRegularFile() {
        val file = File(testDir, "regular_file.txt")
        file.writeText("content")

        val result = FileStorageUtils.getFolderSize(file)
        assertEquals(0L, result)
    }

    @Test
    fun testReturnZeroWhenGivenEmptyDirectory() {
        val result = FileStorageUtils.getFolderSize(testDir)
        assertEquals(0L, result)
    }

    @Test
    fun testReturnCorrectSizeForSingleFile() {
        val file = File(testDir, "file.txt")
        file.writeText("12345") // 5 bytes

        val result = FileStorageUtils.getFolderSize(testDir)
        assertEquals(5L, result)
    }

    @Test
    fun testReturnCorrectSizeForMultipleFiles() {
        File(testDir, "file1.txt").writeText("123") // 3 bytes
        File(testDir, "file2.txt").writeText("12345") // 5 bytes
        File(testDir, "file3.txt").writeText("1234567") // 7 bytes

        val result = FileStorageUtils.getFolderSize(testDir)
        assertEquals(15L, result)
    }

    @Test
    fun testReturnCorrectSizeWithSubdirectory() {
        File(testDir, "file1.txt").writeText("123") // 3 bytes

        val subDir = File(testDir, "subdir")
        subDir.mkdirs()
        File(subDir, "file2.txt").writeText("12345") // 5 bytes

        val result = FileStorageUtils.getFolderSize(testDir)
        assertEquals(8L, result)
    }

    @Test
    fun testReturnCorrectSizeWithNestedSubdirectories() {
        File(testDir, "file1.txt").writeText("12") // 2 bytes

        val subDir1 = File(testDir, "subdir1")
        subDir1.mkdirs()
        File(subDir1, "file2.txt").writeText("123") // 3 bytes

        val subDir2 = File(subDir1, "subdir2")
        subDir2.mkdirs()
        File(subDir2, "file3.txt").writeText("1234") // 4 bytes

        val subDir3 = File(subDir2, "subdir3")
        subDir3.mkdirs()
        File(subDir3, "file4.txt").writeText("12345") // 5 bytes

        val result = FileStorageUtils.getFolderSize(testDir)
        assertEquals(14L, result) // 2 + 3 + 4 + 5
    }

    @Test
    fun testReturnCorrectSizeWithEmptySubdirectories() {
        File(testDir, "file1.txt").writeText("123") // 3 bytes

        val emptyDir1 = File(testDir, "empty1")
        emptyDir1.mkdirs()

        val emptyDir2 = File(testDir, "empty2")
        emptyDir2.mkdirs()

        val result = FileStorageUtils.getFolderSize(testDir)
        assertEquals(3L, result)
    }

    @Test
    fun testReturnCorrectSizeWithLargeFile() {
        val file = File(testDir, "large_file.txt")
        val content = "x".repeat(10000) // 10000 bytes
        file.writeText(content)

        val result = FileStorageUtils.getFolderSize(testDir)
        assertEquals(10000L, result)
    }

    @Test
    fun testReturnCorrectSizeWithMixedFilesAndDirectories() {
        // Root level files
        File(testDir, "root1.txt").writeText("12") // 2 bytes
        File(testDir, "root2.txt").writeText("123") // 3 bytes

        // First subdirectory
        val subDir1 = File(testDir, "sub1")
        subDir1.mkdirs()
        File(subDir1, "sub1_file1.txt").writeText("1234") // 4 bytes
        File(subDir1, "sub1_file2.txt").writeText("12345") // 5 bytes

        // Second subdirectory
        val subDir2 = File(testDir, "sub2")
        subDir2.mkdirs()
        File(subDir2, "sub2_file.txt").writeText("123456") // 6 bytes

        // Nested subdirectory
        val nestedDir = File(subDir1, "nested")
        nestedDir.mkdirs()
        File(nestedDir, "nested_file.txt").writeText("1234567") // 7 bytes

        val result = FileStorageUtils.getFolderSize(testDir)
        assertEquals(27L, result) // 2 + 3 + 4 + 5 + 6 + 7
    }

    @Test
    fun testReturnZeroForDirectoryWithOnlyEmptySubdirectories() {
        val sub1 = File(testDir, "sub1")
        sub1.mkdirs()

        val sub2 = File(testDir, "sub2")
        sub2.mkdirs()

        val nested = File(sub1, "nested")
        nested.mkdirs()

        val result = FileStorageUtils.getFolderSize(testDir)
        assertEquals(0L, result)
    }

    @Test
    fun testReturnCorrectSizeWithSpecialCharactersInFilenames() {
        File(testDir, "file with spaces.txt").writeText("12") // 2 bytes
        File(testDir, "file-with-dashes.txt").writeText("123") // 3 bytes
        File(testDir, "file_with_underscores.txt").writeText("1234") // 4 bytes

        val result = FileStorageUtils.getFolderSize(testDir)
        assertEquals(9L, result)
    }

    @Test
    fun testReturnCorrectSizeWithEmptyFiles() {
        File(testDir, "empty1.txt").writeText("") // 0 bytes
        File(testDir, "empty2.txt").writeText("") // 0 bytes
        File(testDir, "nonempty.txt").writeText("123") // 3 bytes

        val result = FileStorageUtils.getFolderSize(testDir)
        assertEquals(3L, result)
    }
}
