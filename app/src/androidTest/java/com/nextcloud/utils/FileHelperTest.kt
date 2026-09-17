/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
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

    private fun collectPages(directory: File?, pageSize: Int, fetchFolders: Boolean): List<List<File>> {
        val pages = mutableListOf<List<File>>()

        runBlocking {
            FileHelper.forEachDirectoryPage(directory, pageSize, fetchFolders) { page ->
                pages.add(page)
            }
        }

        return pages
    }

    @Test
    fun testForEachDirectoryPageWhenGivenNullDirectoryShouldReportNoPage() {
        assertTrue(collectPages(null, 10, false).isEmpty())
    }

    @Test
    fun testForEachDirectoryPageWhenGivenNonExistentDirectoryShouldReportNoPage() {
        val nonExistent = File(testDirectory, "does_not_exist")

        assertTrue(collectPages(nonExistent, 10, false).isEmpty())
    }

    @Test
    fun testForEachDirectoryPageWhenGivenFileInsteadOfDirectoryShouldReportNoPage() {
        val file = File(testDirectory, "test.txt")
        file.createNewFile()

        assertTrue(collectPages(file, 10, false).isEmpty())
    }

    @Test
    fun testForEachDirectoryPageWhenGivenEmptyDirectoryShouldReportNoPage() {
        assertTrue(collectPages(testDirectory, 10, false).isEmpty())
    }

    @Test
    fun testForEachDirectoryPageWhenPageSizeIsZeroShouldReportNoPage() {
        for (i in 1..5) File(testDirectory, "file$i.txt").createNewFile()

        assertTrue(collectPages(testDirectory, 0, false).isEmpty())
    }

    @Test
    fun testForEachDirectoryPageWhenFetchingFoldersShouldReportOnlyFolders() {
        File(testDirectory, "folder1").mkdir()
        File(testDirectory, "folder2").mkdir()
        File(testDirectory, "file1.txt").createNewFile()
        File(testDirectory, "file2.txt").createNewFile()

        val entries = collectPages(testDirectory, 10, true).flatten()

        assertEquals(2, entries.size)
        assertTrue(entries.all { it.isDirectory })
    }

    @Test
    fun testForEachDirectoryPageWhenFetchingFilesShouldReportOnlyFiles() {
        File(testDirectory, "folder1").mkdir()
        File(testDirectory, "folder2").mkdir()
        File(testDirectory, "file1.txt").createNewFile()
        File(testDirectory, "file2.txt").createNewFile()

        val entries = collectPages(testDirectory, 10, false).flatten()

        assertEquals(2, entries.size)
        assertTrue(entries.all { it.isFile })
    }

    @Test
    fun testForEachDirectoryPageWhenGivenOnlyFoldersAndFetchingFilesShouldReportNoPage() {
        for (i in 1..5) File(testDirectory, "folder$i").mkdir()

        assertTrue(collectPages(testDirectory, 10, false).isEmpty())
    }

    @Test
    fun testForEachDirectoryPageWhenGivenOnlyFilesAndFetchingFoldersShouldReportNoPage() {
        for (i in 1..5) File(testDirectory, "file$i.txt").createNewFile()

        assertTrue(collectPages(testDirectory, 10, true).isEmpty())
    }

    @Test
    fun testForEachDirectoryPageWhenPageSizeExceedsContentShouldReportSinglePage() {
        for (i in 1..3) File(testDirectory, "file$i.txt").createNewFile()

        val pages = collectPages(testDirectory, 100, false)

        assertEquals(1, pages.size)
        assertEquals(3, pages.first().size)
    }

    @Test
    fun testForEachDirectoryPageWhenPaginatingFoldersShouldFillEveryPageButTheLast() {
        for (i in 1..10) File(testDirectory, "folder$i").mkdir()

        val pages = collectPages(testDirectory, 3, true)

        assertEquals(listOf(3, 3, 3, 1), pages.map { it.size })
    }

    @Test
    fun testForEachDirectoryPageWhenContentIsAMultipleOfPageSizeShouldNotReportAnEmptyPage() {
        for (i in 1..9) File(testDirectory, "file$i.txt").createNewFile()

        val pages = collectPages(testDirectory, 3, false)

        assertEquals(listOf(3, 3, 3), pages.map { it.size })
    }

    @Test
    fun testForEachDirectoryPageWhenGivenMixedContentShouldReportEveryEntryExactlyOnce() {
        for (i in 1..3) File(testDirectory, "folder$i").mkdir()
        for (i in 1..7) File(testDirectory, "file$i.txt").createNewFile()

        val folders = collectPages(testDirectory, 2, true).flatten()
        val files = collectPages(testDirectory, 2, false).flatten()

        assertEquals(3, folders.size)
        assertEquals(7, files.size)
        assertEquals(folders.size, folders.distinct().size)
        assertEquals(files.size, files.distinct().size)
        assertTrue(folders.all { it.isDirectory })
        assertTrue(files.all { it.isFile })
    }
}
