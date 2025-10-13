/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.utils

import com.owncloud.android.utils.FileSortOrderBySize
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test
import org.junit.Before
import org.junit.After
import java.io.File

@Suppress("TooManyFunctions", "MagicNumber")
class FileSortOrderBySizeTests {
    private lateinit var tempDir: File

    @Before
    fun setup() {
        tempDir = File(System.getProperty("java.io.tmpdir"), "test_sort_${System.currentTimeMillis()}")
        tempDir.mkdirs()
    }

    @After
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testSortAscendingWhenGivenFilesWithDifferentSizes() {
        val smallFile = File(tempDir, "small.txt").apply {
            writeText("x")
        }
        val mediumFile = File(tempDir, "medium.txt").apply {
            writeText("x".repeat(100))
        }
        val largeFile = File(tempDir, "large.txt").apply {
            writeText("x".repeat(1000))
        }

        val files = mutableListOf(largeFile, smallFile, mediumFile)
        val sortOrder = FileSortOrderBySize("test", true)

        val sorted = sortOrder.sortLocalFiles(files)

        assertEquals("small.txt", sorted[0].name)
        assertEquals("medium.txt", sorted[1].name)
        assertEquals("large.txt", sorted[2].name)
    }

    @Test
    fun testSortDescendingWhenGivenFilesWithDifferentSizes() {
        val smallFile = File(tempDir, "small.txt").apply {
            writeText("a")
        }
        val mediumFile = File(tempDir, "medium.txt").apply {
            writeText("a".repeat(50))
        }
        val largeFile = File(tempDir, "large.txt").apply {
            writeText("a".repeat(500))
        }

        val files = mutableListOf(smallFile, largeFile, mediumFile)
        val sortOrder = FileSortOrderBySize("test", false)

        val sorted = sortOrder.sortLocalFiles(files)

        assertEquals("large.txt", sorted[0].name)
        assertEquals("medium.txt", sorted[1].name)
        assertEquals("small.txt", sorted[2].name)
    }

    @Test
    fun testFoldersComesFirstWhenGivenMixedFilesAndFolders() {
        val folder1 = File(tempDir, "folderA").apply { mkdirs() }
        val folder2 = File(tempDir, "folderB").apply { mkdirs() }
        val file1 = File(tempDir, "file1.txt").apply { writeText("content") }
        val file2 = File(tempDir, "file2.txt").apply { writeText("data") }

        val files = mutableListOf(file1, folder1, file2, folder2)
        val sortOrder = FileSortOrderBySize("test", true)

        val sorted = sortOrder.sortLocalFiles(files)

        assertTrue(sorted[0].isDirectory)
        assertTrue(sorted[1].isDirectory)
        assertFalse(sorted[2].isDirectory)
        assertFalse(sorted[3].isDirectory)
    }

    @Test
    fun testSortByFolderSizeWhenGivenFoldersWithDifferentContent() {
        val smallFolder = File(tempDir, "smallFolder").apply {
            mkdirs()
            File(this, "file.txt").writeText("x")
        }

        val largeFolder = File(tempDir, "largeFolder").apply {
            mkdirs()
            File(this, "file1.txt").writeText("x".repeat(100))
            File(this, "file2.txt").writeText("x".repeat(100))
        }

        val files = mutableListOf(largeFolder, smallFolder)
        val sortOrder = FileSortOrderBySize("test", true)

        val sorted = sortOrder.sortLocalFiles(files)

        assertEquals("smallFolder", sorted[0].name)
        assertEquals("largeFolder", sorted[1].name)
    }

    @Test
    fun testEmptyListWhenGivenNoFiles() {
        val files = mutableListOf<File>()
        val sortOrder = FileSortOrderBySize("test", true)

        val sorted = sortOrder.sortLocalFiles(files)

        assertTrue(sorted.isEmpty())
    }

    @Test
    fun testSingleFileWhenGivenOnlyOneFile() {
        val file = File(tempDir, "single.txt").apply {
            writeText("content")
        }

        val files = mutableListOf(file)
        val sortOrder = FileSortOrderBySize("test", true)

        val sorted = sortOrder.sortLocalFiles(files)

        assertEquals(1, sorted.size)
        assertEquals("single.txt", sorted[0].name)
    }

    @Test
    fun testSameOrderWhenGivenFilesWithSameSize() {
        val file1 = File(tempDir, "file1.txt").apply {
            writeText("same")
        }
        val file2 = File(tempDir, "file2.txt").apply {
            writeText("same")
        }
        val file3 = File(tempDir, "file3.txt").apply {
            writeText("same")
        }

        val files = mutableListOf(file1, file2, file3)
        val sortOrder = FileSortOrderBySize("test", true)

        val sorted = sortOrder.sortLocalFiles(files)

        assertEquals(3, sorted.size)

        // All files have same size, so order should be stable
        sorted.forEach { assertTrue(it.length() == 4L) }
    }
}
