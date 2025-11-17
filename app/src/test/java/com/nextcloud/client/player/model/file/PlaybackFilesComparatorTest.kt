/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.model.file

import com.owncloud.android.utils.FileSortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackFilesComparatorTest {

    @Test
    fun `NONE comparator always returns 0`() {
        val a = file("a")
        val b = file("b")
        assertEquals(0, PlaybackFilesComparator.NONE.compare(a, b))
        assertEquals(0, PlaybackFilesComparator.NONE.compare(b, a))
    }

    @Test
    fun `FAVORITE uses natural alphanumeric ordering`() {
        val list = listOf(file("file10"), file("file2"), file("file1"))
        val sorted = list.sortedWith(PlaybackFilesComparator.FAVORITE)
        assertEquals(listOf("file1", "file2", "file10"), sorted.map { it.name })
    }

    @Test
    fun `GALLERY sorts by lastModified descending`() {
        val a = file("a", modified = 100)
        val b = file("b", modified = 300)
        val c = file("c", modified = 200)
        val sorted = listOf(a, b, c).sortedWith(PlaybackFilesComparator.GALLERY)
        assertEquals(listOf("b", "c", "a"), sorted.map { it.name })
    }

    @Test
    fun `SHARED sorts by lastModified descending (same as GALLERY)`() {
        val a = file("a", modified = 1)
        val b = file("b", modified = 5)
        val sorted = listOf(a, b).sortedWith(PlaybackFilesComparator.SHARED)
        assertEquals(listOf("b", "a"), sorted.map { it.name })
    }

    @Test
    fun `Folder comparator ALPHABET ascending with favorites first`() {
        val list = listOf(
            file("b2", favorite = true),
            file("a10"),
            file("a2", favorite = true),
            file("a1"),
            file("b10", favorite = true)
        )
        val cmp = PlaybackFilesComparator.Folder(FileSortOrder.SortType.ALPHABET, isAscending = true)
        val sorted = list.sortedWith(cmp)
        assertEquals(listOf("a2", "b2", "b10", "a1", "a10"), sorted.map { it.name })
        assertTrue(sorted.take(3).all { it.isFavorite })
        assertTrue(sorted.drop(3).none { it.isFavorite })
    }

    @Test
    fun `Folder comparator ALPHABET descending with favorites first`() {
        val list = listOf(
            file("x1"),
            file("x10", favorite = true),
            file("x2", favorite = true),
            file("x11")
        )
        val cmp = PlaybackFilesComparator.Folder(FileSortOrder.SortType.ALPHABET, isAscending = false)
        val sorted = list.sortedWith(cmp)
        assertEquals(listOf("x10", "x2", "x11", "x1"), sorted.map { it.name })
    }

    @Test
    fun `Folder comparator SIZE ascending then favorites`() {
        val list = listOf(
            file("bigFav", favorite = true, size = 300),
            file("smallFav", favorite = true, size = 100),
            file("mid", size = 200),
            file("tiny", size = 50)
        )
        val cmp = PlaybackFilesComparator.Folder(FileSortOrder.SortType.SIZE, isAscending = true)
        val sorted = list.sortedWith(cmp)
        assertEquals(listOf("smallFav", "bigFav", "tiny", "mid"), sorted.map { it.name })
    }

    @Test
    fun `Folder comparator SIZE descending`() {
        val list = listOf(
            file("a", size = 100),
            file("b", size = 500),
            file("c", size = 300)
        )
        val cmp = PlaybackFilesComparator.Folder(FileSortOrder.SortType.SIZE, isAscending = false)
        val sorted = list.sortedWith(cmp)
        assertEquals(listOf("b", "c", "a"), sorted.map { it.name })
    }

    @Test
    fun `Folder comparator DATE ascending`() {
        val list = listOf(
            file("newFav", favorite = true, modified = 300),
            file("oldFav", favorite = true, modified = 100),
            file("old", modified = 50),
            file("mid", modified = 200)
        )
        val cmp = PlaybackFilesComparator.Folder(FileSortOrder.SortType.DATE, isAscending = true)
        val sorted = list.sortedWith(cmp)
        assertEquals(listOf("oldFav", "newFav", "old", "mid"), sorted.map { it.name })
    }

    @Test
    fun `Folder comparator DATE descending`() {
        val list = listOf(
            file("a", modified = 100),
            file("b", modified = 400),
            file("c", modified = 200)
        )
        val cmp = PlaybackFilesComparator.Folder(FileSortOrder.SortType.DATE, isAscending = false)
        val sorted = list.sortedWith(cmp)
        assertEquals(listOf("b", "c", "a"), sorted.map { it.name })
    }

    @Test
    fun `Alphanumeric edge cases with leading zeros`() {
        val list = listOf(file("track02"), file("track2"), file("track10"), file("track01"))
        val cmp = PlaybackFilesComparator.Folder(FileSortOrder.SortType.ALPHABET, isAscending = true)
        val sorted = list.sortedWith(cmp)
        assertEquals(listOf("track01", "track2", "track02", "track10"), sorted.map { it.name })
    }

    private fun file(name: String, favorite: Boolean = false, size: Long = 0, modified: Long = 0) = PlaybackFile(
        id = "id_$name",
        uri = "uri_$name",
        name = name,
        mimeType = "audio/mpeg",
        contentLength = size,
        lastModified = modified,
        isFavorite = favorite
    )
}
