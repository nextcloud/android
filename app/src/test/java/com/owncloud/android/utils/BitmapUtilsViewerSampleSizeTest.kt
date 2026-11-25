/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.utils

import org.junit.Assert.assertEquals
import org.junit.Test

@Suppress("MagicNumber")
class BitmapUtilsViewerSampleSizeTest {

    private val maxPixels = BitmapUtils.VIEWER_MAX_PIXELS

    @Test
    fun `tall strip within the budget is decoded at full resolution`() {
        assertEquals(1, BitmapUtils.viewerSampleSize(540, 21242, maxPixels))
    }

    @Test
    fun `image exactly at the budget is decoded at full resolution`() {
        assertEquals(1, BitmapUtils.viewerSampleSize(4000, 4000, maxPixels))
    }

    @Test
    fun `image over the budget is halved`() {
        assertEquals(2, BitmapUtils.viewerSampleSize(6000, 4000, maxPixels))
    }

    @Test
    fun `very large image is sampled until it fits the budget`() {
        assertEquals(8, BitmapUtils.viewerSampleSize(20000, 20000, maxPixels))
    }

    @Test
    fun `dimensions whose pixel count overflows Int are handled`() {
        assertEquals(16, BitmapUtils.viewerSampleSize(50000, 50000, maxPixels))
    }
}
