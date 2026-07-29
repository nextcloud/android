/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LocalFileListAdapterTest {

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
}
