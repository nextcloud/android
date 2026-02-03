/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class GalleryAdapterFolderDateTest {

    @Test
    fun `extractFolderDate returns null for null path`() {
        assertNull(GalleryAdapter.extractFolderDate(null))
    }

    @Test
    fun `extractFolderDate returns null for path without date pattern`() {
        assertNull(GalleryAdapter.extractFolderDate("/Photos/vacation/image.jpg"))
        assertNull(GalleryAdapter.extractFolderDate("/Documents/file.pdf"))
        assertNull(GalleryAdapter.extractFolderDate(""))
    }

    @Test
    fun `extractFolderDate extracts YYYY MM pattern`() {
        val result = GalleryAdapter.extractFolderDate("/Photos/2025/01/image.jpg")
        assertNotNull(result)

        val cal = Calendar.getInstance().apply { timeInMillis = result!! }
        assertEquals(2025, cal.get(Calendar.YEAR))
        assertEquals(0, cal.get(Calendar.MONTH)) // January is 0
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH)) // defaults to 1
    }

    @Test
    fun `extractFolderDate extracts YYYY MM DD pattern`() {
        val result = GalleryAdapter.extractFolderDate("/Photos/2025/01/15/image.jpg")
        assertNotNull(result)

        val cal = Calendar.getInstance().apply { timeInMillis = result!! }
        assertEquals(2025, cal.get(Calendar.YEAR))
        assertEquals(0, cal.get(Calendar.MONTH)) // January is 0
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `extractFolderDate handles single digit month as year only`() {
        // Single digit month doesn't match pattern, so only year is captured
        val result = GalleryAdapter.extractFolderDate("/Photos/2025/3/image.jpg")
        assertNotNull(result)

        val cal = Calendar.getInstance().apply { timeInMillis = result!! }
        assertEquals(2025, cal.get(Calendar.YEAR))
        assertEquals(0, cal.get(Calendar.MONTH)) // defaults to January (0)
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH)) // defaults to 1
    }

    @Test
    fun `extractFolderDate ignores single digit day and defaults to 1`() {
        // /2025/03/5/ matches YYYY/MM only, day defaults to 1
        val result = GalleryAdapter.extractFolderDate("/Photos/2025/03/5/image.jpg")
        assertNotNull(result)

        val cal = Calendar.getInstance().apply { timeInMillis = result!! }
        assertEquals(2025, cal.get(Calendar.YEAR))
        assertEquals(2, cal.get(Calendar.MONTH)) // March is 2
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH)) // defaults to 1
    }

    @Test
    fun `extractFolderDate works with nested paths`() {
        val result = GalleryAdapter.extractFolderDate("/InstantUpload/Camera/2024/12/25/IMG_001.jpg")
        assertNotNull(result)

        val cal = Calendar.getInstance().apply { timeInMillis = result!! }
        assertEquals(2024, cal.get(Calendar.YEAR))
        assertEquals(11, cal.get(Calendar.MONTH)) // December is 11
        assertEquals(25, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `extractFolderDate finds first match in path with multiple date patterns`() {
        val result = GalleryAdapter.extractFolderDate("/2023/06/backup/2024/12/25/image.jpg")
        assertNotNull(result)

        val cal = Calendar.getInstance().apply { timeInMillis = result!! }
        assertEquals(2023, cal.get(Calendar.YEAR))
        assertEquals(5, cal.get(Calendar.MONTH)) // June is 5
    }

    @Test
    fun `extractFolderDate returns midnight timestamp`() {
        val result = GalleryAdapter.extractFolderDate("/Photos/2025/01/15/image.jpg")
        assertNotNull(result)

        val cal = Calendar.getInstance().apply { timeInMillis = result!! }
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
        assertEquals(0, cal.get(Calendar.SECOND))
        assertEquals(0, cal.get(Calendar.MILLISECOND))
    }

    @Test
    fun `folder date ordering - newer dates should be greater`() {
        val jan15 = GalleryAdapter.extractFolderDate("/Photos/2025/01/15/a.jpg")!!
        val jan20 = GalleryAdapter.extractFolderDate("/Photos/2025/01/20/b.jpg")!!
        val feb01 = GalleryAdapter.extractFolderDate("/Photos/2025/02/01/c.jpg")!!

        assert(jan20 > jan15) { "Jan 20 should be after Jan 15" }
        assert(feb01 > jan20) { "Feb 1 should be after Jan 20" }
        assert(feb01 > jan15) { "Feb 1 should be after Jan 15" }
    }

    @Test
    fun `extractFolderDate handles year only path`() {
        val result = GalleryAdapter.extractFolderDate("/Photos/2025/image.jpg")
        assertNotNull(result)

        val cal = Calendar.getInstance().apply { timeInMillis = result!! }
        assertEquals(2025, cal.get(Calendar.YEAR))
        assertEquals(0, cal.get(Calendar.MONTH)) // defaults to January (0)
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH)) // defaults to 1
    }

    @Test
    fun `extractFolderDate handles invalid month 00 as year only`() {
        // Month 00 is invalid, so it defaults to January
        val result = GalleryAdapter.extractFolderDate("/Photos/2025/00/image.jpg")
        assertNotNull(result)

        val cal = Calendar.getInstance().apply { timeInMillis = result!! }
        assertEquals(2025, cal.get(Calendar.YEAR))
        assertEquals(0, cal.get(Calendar.MONTH)) // defaults to January (0)
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH)) // defaults to 1
    }

    @Test
    fun `extractFolderDate handles month 12`() {
        val result = GalleryAdapter.extractFolderDate("/Photos/2025/12/image.jpg")
        assertNotNull(result)

        val cal = Calendar.getInstance().apply { timeInMillis = result!! }
        assertEquals(2025, cal.get(Calendar.YEAR))
        assertEquals(11, cal.get(Calendar.MONTH)) // December is 11
    }

    @Test
    fun `extractFolderDate handles day 31`() {
        val result = GalleryAdapter.extractFolderDate("/Photos/2025/01/31/image.jpg")
        assertNotNull(result)

        val cal = Calendar.getInstance().apply { timeInMillis = result!! }
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `extractFolderDate handles invalid day Feb 30 as Feb 1`() {
        // Feb 30 is invalid, so day defaults to 1
        val result = GalleryAdapter.extractFolderDate("/Photos/2025/02/30/image.jpg")
        assertNotNull(result)

        val cal = Calendar.getInstance().apply { timeInMillis = result!! }
        assertEquals(2025, cal.get(Calendar.YEAR))
        assertEquals(1, cal.get(Calendar.MONTH)) // February is 1
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH)) // defaults to 1
    }

    @Test
    fun `extractFolderDate handles invalid day 00 as day 1`() {
        // Day 00 is invalid, so day defaults to 1
        val result = GalleryAdapter.extractFolderDate("/Photos/2025/03/00/image.jpg")
        assertNotNull(result)

        val cal = Calendar.getInstance().apply { timeInMillis = result!! }
        assertEquals(2025, cal.get(Calendar.YEAR))
        assertEquals(2, cal.get(Calendar.MONTH)) // March is 2
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH)) // defaults to 1
    }

    @Test
    fun `extractFolderDate requires trailing slash after date components`() {
        // No trailing slash after month, so only year is captured
        val result = GalleryAdapter.extractFolderDate("/Photos/2025/03image.jpg")
        assertNotNull(result)

        val cal = Calendar.getInstance().apply { timeInMillis = result!! }
        assertEquals(2025, cal.get(Calendar.YEAR))
        assertEquals(0, cal.get(Calendar.MONTH)) // defaults to January (0)
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH)) // defaults to 1
    }

    @Test
    fun `extractFolderDate returns null when no trailing slash after year`() {
        // Pattern requires trailing slash after year at minimum
        assertNull(GalleryAdapter.extractFolderDate("/Photos/2025image.jpg"))
    }

    @Test
    fun `extractFolderDate works at start of path`() {
        val result = GalleryAdapter.extractFolderDate("/2025/06/15/image.jpg")
        assertNotNull(result)

        val cal = Calendar.getInstance().apply { timeInMillis = result!! }
        assertEquals(2025, cal.get(Calendar.YEAR))
        assertEquals(5, cal.get(Calendar.MONTH)) // June is 5
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `extractFolderDate handles different years`() {
        val y2020 = GalleryAdapter.extractFolderDate("/Photos/2020/06/image.jpg")!!
        val y2025 = GalleryAdapter.extractFolderDate("/Photos/2025/06/image.jpg")!!

        assert(y2025 > y2020) { "2025 should be after 2020" }
    }
}
