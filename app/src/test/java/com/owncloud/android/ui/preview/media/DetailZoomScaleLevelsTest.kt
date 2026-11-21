/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.preview.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.min

@Suppress("MagicNumber")
class DetailZoomScaleLevelsTest {

    private val viewWidth = 1080
    private val viewHeight = 2400

    private fun fitScale(imageWidth: Int, imageHeight: Int): Float =
        min(viewWidth.toFloat() / imageWidth, viewHeight.toFloat() / imageHeight)

    private fun levels(imageWidth: Int, imageHeight: Int, bitmapWidth: Int = imageWidth): DetailZoomScaleLevels {
        val levels = DetailZoomScaleLevels.of(viewWidth, viewHeight, imageWidth, imageHeight, bitmapWidth)
        assertNotNull(levels)
        return levels!!
    }

    @Test
    fun `double tap on a tall strip fits its width to the view`() {
        val levels = levels(540, 21242)

        val renderedWidth = 540 * fitScale(540, 21242) * levels.medium
        assertEquals(viewWidth.toFloat(), renderedWidth, 0.5f)
    }

    @Test
    fun `maximum zoom of a tall strip goes past fit to width and past one pixel per screen pixel`() {
        val levels = levels(540, 21242)

        assertTrue(levels.maximum >= levels.medium * 3)
        assertTrue(540 * fitScale(540, 21242) * levels.maximum >= 540)
    }

    @Test
    fun `image shaped like the view keeps PhotoView default double tap level`() {
        val levels = levels(1080, 2400)

        assertEquals(1f, levels.minimum)
        assertEquals(1.75f, levels.medium)
        assertTrue(levels.maximum >= 3f)
    }

    @Test
    fun `maximum zoom reaches one pixel per screen pixel for a large photo`() {
        val levels = levels(8000, 6000)

        assertTrue(8000 * fitScale(8000, 6000) * levels.maximum >= 8000)
    }

    @Test
    fun `density-scaled drawable uses bitmap pixels for one pixel per screen pixel`() {
        val levels = levels(imageWidth = 540, imageHeight = 1200, bitmapWidth = 2160)

        assertTrue(540 * fitScale(540, 1200) * levels.maximum >= 2160)
    }

    @Test
    fun `levels are unknown before the view is laid out`() {
        assertNull(DetailZoomScaleLevels.of(0, 0, 540, 21242, 540))
    }
}
