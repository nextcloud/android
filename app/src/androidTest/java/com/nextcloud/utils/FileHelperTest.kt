/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

@Suppress("TooManyFunctions")
class FileHelperTest {

    private lateinit var testDirectory: File

    @Before
    fun setup() {
        testDirectory = Files.createTempDirectory("test").toFile()
    }

    @After
    fun tearDown() {
        testDirectory.deleteRecursively()
    }

    @Test
    fun testListDirectoryEntriesWhenGivenNullDirectoryShouldReturnEmptyList() {
        val result = FileHelper.listDirectoryEntries(null, 0, 10, false)
        assertTrue(result.isEmpty())
    }

    @Test
    fun testListDirectoryEntriesWhenGivenNonExistentDirectoryShouldReturnEmptyList() {
        val nonExistent = File(testDirectory, "does_not_exist")
        val result = FileHelper.listDirectoryEntries(nonExistent, 0, 10, false)
        assertTrue(result.isEmpty())
    }

    @Test
    fun testListDirectoryEntriesWhenGivenFileInsteadOfDirectoryShouldReturnEmptyList() {
        val file = File(testDirectory, "test.txt")
        file.createNewFile()
        val result = FileHelper.listDirectoryEntries(file, 0, 10, false)
        assertTrue(result.isEmpty())
    }

    @Test
    fun testListDirectoryEntriesWhenGivenEmptyDirectoryShouldReturnEmptyList() {
        val result = FileHelper.listDirectoryEntries(testDirectory, 0, 10, false)
        assertTrue(result.isEmpty())
    }

    @Test
    fun testListDirectoryEntriesWhenFetchingFoldersShouldReturnOnlyFolders() {
        File(testDirectory, "folder1").mkdir()
        File(testDirectory, "folder2").mkdir()
        File(testDirectory, "file1.txt").createNewFile()
        File(testDirectory, "file2.txt").createNewFile()

        val result = FileHelper.listDirectoryEntries(testDirectory, 0, 10, true)

        assertEquals(2, result.size)
        assertTrue(result.all { it.isDirectory })
    }

    @Test
    fun testListDirectoryEntriesWhenFetchingFilesShouldReturnOnlyFiles() {
        File(testDirectory, "folder1").mkdir()
        File(testDirectory, "folder2").mkdir()
        File(testDirectory, "file1.txt").createNewFile()
        File(testDirectory, "file2.txt").createNewFile()

        val result = FileHelper.listDirectoryEntries(testDirectory, 0, 10, false)

        assertEquals(2, result.size)
        assertTrue(result.all { it.isFile })
    }

    @Test
    fun testListDirectoryEntriesWhenStartIndexProvidedShouldSkipCorrectNumberOfItems() {
        for (i in 1..5) File(testDirectory, "file$i.txt").createNewFile()
        val result = FileHelper.listDirectoryEntries(testDirectory, 2, 10, false)
        assertEquals(3, result.size)
    }

    @Test
    fun testListDirectoryEntriesWhenMaxItemsProvidedShouldLimitResults() {
        for (i in 1..10) File(testDirectory, "file$i.txt").createNewFile()
        val result = FileHelper.listDirectoryEntries(testDirectory, 0, 5, false)
        assertEquals(5, result.size)
    }

    @Test
    fun testListDirectoryEntriesWhenGivenStartIndexAndMaxItemsShouldReturnCorrectSubset() {
        for (i in 1..10) File(testDirectory, "file$i.txt").createNewFile()
        val result = FileHelper.listDirectoryEntries(testDirectory, 3, 4, false)
        assertEquals(4, result.size)
    }

    @Test
    fun testListDirectoryEntriesWhenStartIndexBeyondAvailableShouldReturnEmptyList() {
        for (i in 1..3) File(testDirectory, "file$i.txt").createNewFile()
        val result = FileHelper.listDirectoryEntries(testDirectory, 10, 5, false)
        assertTrue(result.isEmpty())
    }

    @Test
    fun testListDirectoryEntriesWhenMaxItemsBeyondAvailableShouldReturnAllItems() {
        for (i in 1..3) File(testDirectory, "file$i.txt").createNewFile()
        val result = FileHelper.listDirectoryEntries(testDirectory, 0, 100, false)
        assertEquals(3, result.size)
    }

    @Test
    fun testListDirectoryEntriesWhenFetchingFoldersWithOffsetShouldSkipCorrectly() {
        for (i in 1..5) File(testDirectory, "folder$i").mkdir()
        for (i in 1..3) File(testDirectory, "file$i.txt").createNewFile()

        val result = FileHelper.listDirectoryEntries(testDirectory, 2, 10, true)

        assertEquals(3, result.size)
        assertTrue(result.all { it.isDirectory })
    }

    @Test
    fun testListDirectoryEntriesWhenFetchingFilesWithOffsetShouldSkipCorrectly() {
        for (i in 1..3) File(testDirectory, "folder$i").mkdir()
        for (i in 1..5) File(testDirectory, "file$i.txt").createNewFile()

        val result = FileHelper.listDirectoryEntries(testDirectory, 2, 10, false)

        assertEquals(3, result.size)
        assertTrue(result.all { it.isFile })
    }

    @Test
    fun testListDirectoryEntriesWhenGivenOnlyFoldersAndFetchingFilesShouldReturnEmptyList() {
        for (i in 1..5) File(testDirectory, "folder$i").mkdir()
        val result = FileHelper.listDirectoryEntries(testDirectory, 0, 10, false)
        assertTrue(result.isEmpty())
    }

    @Test
    fun testListDirectoryEntriesWhenGivenOnlyFilesAndFetchingFoldersShouldReturnEmptyList() {
        for (i in 1..5) File(testDirectory, "file$i.txt").createNewFile()
        val result = FileHelper.listDirectoryEntries(testDirectory, 0, 10, true)
        assertTrue(result.isEmpty())
    }

    @Test
    fun testListDirectoryEntriesWhenMaxItemsIsZeroShouldReturnEmptyList() {
        for (i in 1..5) File(testDirectory, "file$i.txt").createNewFile()
        val result = FileHelper.listDirectoryEntries(testDirectory, 0, 0, false)
        assertTrue(result.isEmpty())
    }

    @Test
    fun testListDirectoryEntriesWhenGivenMixedContentShouldFilterCorrectly() {
        for (i in 1..3) File(testDirectory, "folder$i").mkdir()
        for (i in 1..7) File(testDirectory, "file$i.txt").createNewFile()

        val folders = FileHelper.listDirectoryEntries(testDirectory, 0, 10, true)
        val files = FileHelper.listDirectoryEntries(testDirectory, 0, 10, false)

        assertEquals(3, folders.size)
        assertEquals(7, files.size)
        assertTrue(folders.all { it.isDirectory })
        assertTrue(files.all { it.isFile })
    }

    @Test
    fun testListDirectoryEntriesWhenPaginatingFoldersShouldWorkCorrectly() {
        for (i in 1..10) File(testDirectory, "folder$i").mkdir()

        val page1 = FileHelper.listDirectoryEntries(testDirectory, 0, 3, true)
        val page2 = FileHelper.listDirectoryEntries(testDirectory, 3, 3, true)
        val page3 = FileHelper.listDirectoryEntries(testDirectory, 6, 3, true)
        val page4 = FileHelper.listDirectoryEntries(testDirectory, 9, 3, true)

        assertEquals(3, page1.size)
        assertEquals(3, page2.size)
        assertEquals(3, page3.size)
        assertEquals(1, page4.size)
    }

    @Test
    fun testListDirectoryEntriesWhenPaginatingFilesShouldWorkCorrectly() {
        for (i in 1..10) File(testDirectory, "file$i.txt").createNewFile()

        val page1 = FileHelper.listDirectoryEntries(testDirectory, 0, 4, false)
        val page2 = FileHelper.listDirectoryEntries(testDirectory, 4, 4, false)
        val page3 = FileHelper.listDirectoryEntries(testDirectory, 8, 4, false)

        assertEquals(4, page1.size)
        assertEquals(4, page2.size)
        assertEquals(2, page3.size)
    }
}
