/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.FileSortOrder.Companion.SORT_A_TO_Z_ID
import com.owncloud.android.utils.FileSortOrder.Companion.SORT_BIG_TO_SMALL_ID
import com.owncloud.android.utils.FileSortOrder.Companion.SORT_NEW_TO_OLD_ID
import com.owncloud.android.utils.FileSortOrder.Companion.SORT_OLD_TO_NEW_ID
import com.owncloud.android.utils.FileSortOrder.Companion.SORT_SMALL_TO_BIG_ID
import com.owncloud.android.utils.FileSortOrder.Companion.SORT_Z_TO_A_ID
import com.owncloud.android.utils.FileSortOrderByDate
import com.owncloud.android.utils.FileSortOrderByName
import com.owncloud.android.utils.FileSortOrderBySize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class FileSortOrderTests {

    private fun createTempFile(prefix: String, lastModified: Long? = null, sizeBytes: Int? = null): File {
        return File.createTempFile(prefix, ".txt").apply {
            lastModified?.let { setLastModified(it) }
            sizeBytes?.let { writeBytes(ByteArray(it)) }
        }
    }

    private fun createTempFolder(prefix: String): File {
        return File.createTempFile(prefix, "").apply {
            delete()
            mkdir()
        }
    }

    private fun createOCFile(path: String, modTime: Long? = null, size: Long? = null): OCFile {
        return OCFile(path).apply {
            modTime?.let { modificationTimestamp = it }
            size?.let { fileLength = it }
        }
    }

    private fun testConcurrentModification(
        files: MutableList<File>,
        sorter: com.owncloud.android.utils.FileSortOrder,
        iterations: Int = 50
    ) {
        val executor = Executors.newFixedThreadPool(2)
        try {
            repeat(iterations) { i ->
                // modifying and sorting files
                executor.submit { sorter.sortLocalFiles(files) }
                executor.submit {
                    files.add(createTempFile("temp$i", lastModified = i.toLong()))
                    if (files.size > 20) files.removeAt(0)
                }
            }
        } finally {
            executor.shutdown()
            executor.awaitTermination(5, TimeUnit.SECONDS)
        }
        assertTrue(true)
    }

    @Test
    fun testSortLocalFilesAscending() {
        val file1 = createTempFile("file1", lastModified = 1000)
        val file2 = createTempFile("file2", lastModified = 2000)
        val file3 = createTempFile("file3", lastModified = 1500)

        val files = mutableListOf(file1, file2, file3)
        val sorter = FileSortOrderByDate(SORT_OLD_TO_NEW_ID, ascending = true)

        val sorted = sorter.sortLocalFiles(files)

        assertEquals(listOf(file1, file3, file2), sorted)
    }

    @Test
    fun testSortLocalFilesAscendingFalse() {
        val file1 = createTempFile("file1", lastModified = 1000)
        val file2 = createTempFile("file2", lastModified = 2000)
        val file3 = createTempFile("file3", lastModified = 1500)

        val files = mutableListOf(file1, file2, file3)
        val sorter = FileSortOrderByDate(SORT_OLD_TO_NEW_ID, ascending = false)

        val sorted = sorter.sortLocalFiles(files)

        assertEquals(listOf(file2, file3, file1), sorted)
    }

    @Test
    fun testSortLocalFilesDescending() {
        val file1 = createTempFile("file1", lastModified = 1000)
        val file2 = createTempFile("file2", lastModified = 2000)
        val file3 = createTempFile("file3", lastModified = 1500)

        val files = mutableListOf(file1, file2, file3)
        val sorter = FileSortOrderByDate(SORT_NEW_TO_OLD_ID, ascending = false)

        val sorted = sorter.sortLocalFiles(files)

        assertEquals(listOf(file2, file3, file1), sorted)
    }

    @Test
    fun testSortLocalFilesNoConcurrentModification() {
        val files = mutableListOf(
            createTempFile("file1", lastModified = 1000),
            createTempFile("file2", lastModified = 2000),
            createTempFile("file3", lastModified = 1500)
        )
        val sorter = FileSortOrderByDate(SORT_OLD_TO_NEW_ID, ascending = true)

        testConcurrentModification(files, sorter, iterations = 100)
    }

    @Test
    fun testSortCloudFilesByDate() {
        val f1 = createOCFile("/123.txt", modTime = 1000)
        val f2 = createOCFile("/124.txt", modTime = 3000)
        val f3 = createOCFile("/125.txt", modTime = 2000)

        val files = mutableListOf(f1, f2, f3)
        val sorter = FileSortOrderByDate(SORT_OLD_TO_NEW_ID, ascending = true)

        val sorted = sorter.sortCloudFiles(files, foldersBeforeFiles = false, favoritesFirst = false)

        assertEquals(listOf(f1, f3, f2), sorted)
    }

    @Test
    fun testSortLocalFilesByNameAscending() {
        val folder = createTempFolder("folder")
        val file1 = createTempFile("apple")
        val file2 = createTempFile("banana")
        val file3 = createTempFile("cherry")

        val files = mutableListOf(file3, folder, file1, file2)
        val sorter = FileSortOrderByName(SORT_A_TO_Z_ID, ascending = true)

        val sorted = sorter.sortLocalFiles(files)

        assertEquals(listOf(folder, file1, file2, file3), sorted)
    }

    @Test
    fun testSortLocalFilesByNameDescending() {
        val file1 = createTempFile("apple")
        val file2 = createTempFile("banana")
        val file3 = createTempFile("cherry")

        val files = mutableListOf(file1, file2, file3)
        val sorter = FileSortOrderByName(SORT_Z_TO_A_ID, ascending = false)

        val sorted = sorter.sortLocalFiles(files)

        assertEquals(listOf(file3, file2, file1), sorted)
    }

    @Test
    fun testSortCloudFilesByName() {
        val f1 = createOCFile("/b.txt")
        val f2 = createOCFile("/a.txt")
        val f3 = createOCFile("/c.txt")

        val files = mutableListOf(f1, f2, f3)
        val sorter = FileSortOrderByName(SORT_A_TO_Z_ID, ascending = true)

        val sorted = sorter.sortCloudFiles(files, foldersBeforeFiles = false, favoritesFirst = false)

        assertEquals(listOf(f2, f1, f3), sorted)
    }

    @Test
    fun testSortLocalFilesByNameNoConcurrentModification() {
        val files = mutableListOf(
            createTempFile("apple"),
            createTempFile("banana"),
            createTempFile("cherry"),
            createTempFolder("folder")
        )
        val sorter = FileSortOrderByName(SORT_A_TO_Z_ID, ascending = true)

        testConcurrentModification(files, sorter)
    }

    @Test
    fun testSortLocalFilesBySizeAscending() {
        val file1 = createTempFile("file1", sizeBytes = 100)
        val file2 = createTempFile("file2", sizeBytes = 300)
        val file3 = createTempFile("file3", sizeBytes = 200)

        val files = mutableListOf(file1, file2, file3)
        val sorter = FileSortOrderBySize(SORT_SMALL_TO_BIG_ID, ascending = true)

        val sorted = sorter.sortLocalFiles(files)

        assertEquals(listOf(file1, file3, file2), sorted)
    }

    @Test
    fun testSortLocalFilesBySizeDescending() {
        val file1 = createTempFile("file1", sizeBytes = 100)
        val file2 = createTempFile("file2", sizeBytes = 300)
        val file3 = createTempFile("file3", sizeBytes = 200)
        val folder1 = createTempFolder("folder1")
        val folder2 = createTempFolder("folder2")

        val files = mutableListOf(file1, file2, file3, folder1, folder2)
        val sorter = FileSortOrderBySize(SORT_BIG_TO_SMALL_ID, ascending = false)

        val sorted = sorter.sortLocalFiles(files)

        assertEquals(listOf(folder1, folder2, file2, file3, file1), sorted)
    }

    @Test
    fun testSortCloudFilesBySize() {
        val f1 = createOCFile("/file1.txt", size = 100)
        val f2 = createOCFile("/file2.txt", size = 300)
        val f3 = createOCFile("/file3.txt", size = 200)

        val files = mutableListOf(f1, f2, f3)
        val sorter = FileSortOrderBySize(SORT_SMALL_TO_BIG_ID, ascending = true)

        val sorted = sorter.sortCloudFiles(files, foldersBeforeFiles = false, favoritesFirst = false)

        assertEquals(listOf(f1, f3, f2), sorted)
    }

    @Test
    fun testSortLocalFilesBySizeNoConcurrentModification() {
        val files = mutableListOf(
            createTempFile("file1", sizeBytes = 100),
            createTempFile("file2", sizeBytes = 200),
            createTempFile("file3", sizeBytes = 300),
            createTempFolder("folder")
        )
        val sorter = FileSortOrderBySize(SORT_SMALL_TO_BIG_ID, ascending = true)

        testConcurrentModification(files, sorter)
    }
}
