/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.FileSortOrder
import com.owncloud.android.utils.FileSortOrder.Companion.SORT_A_TO_Z_ID
import com.owncloud.android.utils.FileSortOrder.Companion.SORT_BIG_TO_SMALL_ID
import com.owncloud.android.utils.FileSortOrder.Companion.SORT_NEW_TO_OLD_ID
import com.owncloud.android.utils.FileSortOrder.Companion.SORT_OLD_TO_NEW_ID
import com.owncloud.android.utils.FileSortOrder.Companion.SORT_SMALL_TO_BIG_ID
import com.owncloud.android.utils.FileSortOrderByDate
import com.owncloud.android.utils.FileSortOrderByName
import com.owncloud.android.utils.FileSortOrderBySize
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

@Suppress("MagicNumber", "TooManyFunctions")
class FileSortOrderTests {

    private fun tmpFile(prefix: String, lastModified: Long? = null, size: Int? = null): File =
        File.createTempFile(prefix, ".txt").apply {
            lastModified?.let { setLastModified(it) }
            size?.let { writeBytes(ByteArray(it)) }
        }

    private fun tmpFolder(prefix: String): File = File.createTempFile(prefix, "").apply {
        delete()
        mkdir()
    }

    private fun ocFile(path: String, mod: Long? = null, size: Long? = null) = OCFile(path).apply {
        mod?.let { modificationTimestamp = it }
        size?.let { fileLength = it }
    }

    private fun runSortFiles(name: String, items: MutableList<File>, sorter: FileSortOrder, expected: List<File>) {
        val actual = sorter.sortLocalFiles(items)
        assertEquals(name, expected, actual)
    }

    private fun runSortCloudFiles(
        name: String,
        items: MutableList<OCFile>,
        sorter: FileSortOrder,
        expected: List<OCFile>
    ) {
        val actual = sorter.sortCloudFiles(items, foldersBeforeFiles = false, favoritesFirst = false)
        assertEquals(name, expected, actual)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun testConcurrency(files: MutableList<File>, sorter: FileSortOrder, iterations: Int = 50) {
        val latch = CountDownLatch(iterations * 2)
        val errors = mutableListOf<Throwable>()

        repeat(iterations) { i ->
            thread {
                try {
                    sorter.sortLocalFiles(files)
                } catch (e: Throwable) {
                    errors.add(e)
                } finally {
                    latch.countDown()
                }
            }

            thread {
                try {
                    files.add(tmpFile("tmp$i", lastModified = i.toLong()))
                    if (files.size > 20) files.removeAt(0)
                } catch (e: Throwable) {
                    errors.add(e)
                } finally {
                    latch.countDown()
                }
            }
        }

        // Wait for threads to finish
        val completed = latch.await(5, TimeUnit.SECONDS)

        if (errors.isNotEmpty()) {
            throw AssertionError("Exceptions occurred in background threads: ${errors.first()}", errors.first())
        }

        if (!completed) {
            throw AssertionError("Concurrent test timed out")
        }
    }

    @Test
    fun sortDateOldToNew() {
        val file1 = tmpFile("file1", 1000)
        val file2 = tmpFile("file2", 2000)
        val file3 = tmpFile("file3", 1500)

        runSortFiles(
            "old→new asc",
            mutableListOf(file1, file2, file3),
            FileSortOrderByDate(SORT_OLD_TO_NEW_ID, true),
            listOf(file1, file3, file2)
        )

        runSortFiles(
            "old→new desc",
            mutableListOf(file1, file2, file3),
            FileSortOrderByDate(SORT_OLD_TO_NEW_ID, false),
            listOf(file2, file3, file1)
        )
    }

    @Test
    fun sortDateNewToOld() {
        val file1 = tmpFile("file1", 1000)
        val file2 = tmpFile("file2", 2000)
        val file3 = tmpFile("file3", 1500)

        runSortFiles(
            "new→old asc",
            mutableListOf(file1, file2, file3),
            FileSortOrderByDate(SORT_NEW_TO_OLD_ID, true),
            listOf(file1, file3, file2)
        )

        runSortFiles(
            "new→old desc",
            mutableListOf(file1, file2, file3),
            FileSortOrderByDate(SORT_NEW_TO_OLD_ID, false),
            listOf(file2, file3, file1)
        )
    }

    @Test
    fun sortDateCloud() {
        val file1 = ocFile("/1", mod = 1000)
        val file2 = ocFile("/2", mod = 3000)
        val file3 = ocFile("/3", mod = 2000)

        runSortCloudFiles(
            "cloud old→new asc",
            mutableListOf(file1, file2, file3),
            FileSortOrderByDate(SORT_OLD_TO_NEW_ID, true),
            listOf(file1, file3, file2)
        )
    }

    @Test
    fun sortDateConcurrency() {
        val items = mutableListOf(
            tmpFile("file1", 1000),
            tmpFile("file2", 2000),
            tmpFile("file3", 1500)
        )
        testConcurrency(items, FileSortOrderByDate(SORT_OLD_TO_NEW_ID, true))
    }

    @Test
    fun sortNameLocal() {
        val folder = tmpFolder("folder")
        val file1 = tmpFile("apple")
        val file2 = tmpFile("banana")
        val file3 = tmpFile("cherry")

        runSortFiles(
            "A→Z asc",
            mutableListOf(file3, folder, file1, file2),
            FileSortOrderByName(SORT_A_TO_Z_ID, true),
            listOf(folder, file1, file2, file3)
        )

        runSortFiles(
            "A→Z desc",
            mutableListOf(file3, folder, file1, file2),
            FileSortOrderByName(SORT_A_TO_Z_ID, false),
            listOf(folder, file3, file2, file1)
        )
    }

    @Test
    fun sortNameCloud() {
        val file1 = ocFile("/b.txt")
        val file2 = ocFile("/a.txt")
        val file3 = ocFile("/c.txt")

        runSortCloudFiles(
            "cloud A→Z asc",
            mutableListOf(file1, file2, file3),
            FileSortOrderByName(SORT_A_TO_Z_ID, true),
            listOf(file2, file1, file3)
        )
    }

    @Test
    fun sortNameConcurrency() {
        testConcurrency(
            mutableListOf(
                tmpFile("apple"),
                tmpFile("banana"),
                tmpFile("cherry"),
                tmpFolder("folder")
            ),
            FileSortOrderByName(SORT_A_TO_Z_ID, true)
        )
    }

    @Test
    fun sortSizeLocal() {
        val file1 = tmpFile("file1", size = 100)
        val file2 = tmpFile("file2", size = 300)
        val file3 = tmpFile("file3", size = 200)
        val d1 = tmpFolder("folder1")
        val d2 = tmpFolder("folder2")

        runSortFiles(
            "small→big asc",
            mutableListOf(file1, file2, file3),
            FileSortOrderBySize(SORT_SMALL_TO_BIG_ID, true),
            listOf(file1, file3, file2)
        )

        runSortFiles(
            "small→big desc",
            mutableListOf(file1, file2, file3),
            FileSortOrderBySize(SORT_SMALL_TO_BIG_ID, false),
            listOf(file2, file3, file1)
        )

        runSortFiles(
            "big→small asc (folders first)",
            mutableListOf(file1, file2, file3, d1, d2),
            FileSortOrderBySize(SORT_BIG_TO_SMALL_ID, true),
            listOf(d1, d2, file1, file3, file2)
        )

        runSortFiles(
            "big→small desc (folders first)",
            mutableListOf(file1, file2, file3, d1, d2),
            FileSortOrderBySize(SORT_BIG_TO_SMALL_ID, false),
            listOf(d1, d2, file2, file3, file1)
        )
    }

    @Test
    fun sortSizeCloud() {
        val file1 = ocFile("/1", size = 100)
        val file2 = ocFile("/2", size = 300)
        val file3 = ocFile("/3", size = 200)

        runSortCloudFiles(
            "cloud small→big asc",
            mutableListOf(file1, file2, file3),
            FileSortOrderBySize(SORT_SMALL_TO_BIG_ID, true),
            listOf(file1, file3, file2)
        )
    }

    @Test
    fun sortSizeConcurrency() {
        testConcurrency(
            mutableListOf(
                tmpFile("file1", size = 100),
                tmpFile("file2", size = 200),
                tmpFile("file3", size = 300),
                tmpFolder("folder")
            ),
            FileSortOrderBySize(SORT_SMALL_TO_BIG_ID, true)
        )
    }
}
