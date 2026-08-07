/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter.localFileList

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LocalFileListAdapterTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun sampleFiles(): List<File> = listOf(File("/sdcard/a.jpg"), File("/sdcard/b.png"), File("/sdcard/c.mp4"))

    private fun assertAllPositionsHaveUniqueIds(headerOffset: Int, files: List<File>) {
        val itemCount = files.size + 1 + headerOffset

        val ids = mutableSetOf<Long>()
        for (position in 0 until itemCount) {
            val id = LocalFileListAdapter.getStableItemId(position, headerOffset, files)
            assertTrue("Duplicate stable ID $id at position $position", ids.add(id))
        }
        assertEquals("Every position must produce a distinct stable ID", itemCount, ids.size)
    }

    @Test
    fun stableIdsAreUniqueWhenHeaderIsVisible() {
        assertAllPositionsHaveUniqueIds(headerOffset = 1, files = sampleFiles())
    }

    @Test
    fun stableIdsAreUniqueWhenHeaderIsHidden() {
        assertAllPositionsHaveUniqueIds(headerOffset = 0, files = sampleFiles())
    }

    @Test
    fun headerAndFooterKeepDedicatedIds() {
        val files = sampleFiles()

        val headerId = LocalFileListAdapter.getStableItemId(0, 1, files)
        val footerId = LocalFileListAdapter.getStableItemId(files.size + 1, 1, files)

        assertEquals(Long.MIN_VALUE, headerId)
        assertEquals(Long.MIN_VALUE + 1, footerId)
    }

    @Test
    fun stableIdIsDerivedFromAbsolutePath() {
        val file = File("/sdcard/a.jpg")

        val id = LocalFileListAdapter.getStableItemId(0, 0, listOf(file))

        assertEquals(file.absolutePath.hashCode().toLong(), id)
    }

    @Test
    fun filterMatchesFileNamesIgnoringCase() {
        val files = listOf(File("/sdcard/Holiday.jpg"), File("/sdcard/invoice.pdf"), File("/sdcard/HOLIDAY-2.png"))

        val result = LocalFileListAdapter.filterByName(files, "holiday")

        assertEquals(listOf(files[0], files[2]), result)
    }

    @Test
    fun filterKeepsOriginalOrderAndDropsNonMatchingFiles() {
        val files = sampleFiles()

        assertEquals(emptyList<File>(), LocalFileListAdapter.filterByName(files, "zip"))
        assertEquals(files, LocalFileListAdapter.filterByName(files, ""))
    }

    @Test
    fun filterIgnoresParentDirectoriesOfTheFilePath() {
        val files = listOf(File("/sdcard/holiday/invoice.pdf"))

        assertEquals(emptyList<File>(), LocalFileListAdapter.filterByName(files, "holiday"))
    }

    @Test
    fun footerCountsFoldersEvenWhenHiddenButSkipsHiddenFiles() {
        val folder = temporaryFolder.newFolder("folder")
        val hiddenFolder = temporaryFolder.newFolder(".hiddenFolder")
        val file = temporaryFolder.newFile("invoice.pdf")
        val hiddenFile = temporaryFolder.newFile(".hiddenInvoice.pdf")

        val (filesCount, foldersCount) = LocalFileListAdapter.countFooterEntries(
            listOf(folder, hiddenFolder, file, hiddenFile)
        )

        assertEquals(1, filesCount)
        assertEquals(2, foldersCount)
    }

    @Test
    fun footerCountsAreZeroForEmptyList() {
        val (filesCount, foldersCount) = LocalFileListAdapter.countFooterEntries(emptyList())

        assertEquals(0, filesCount)
        assertEquals(0, foldersCount)
    }
}
