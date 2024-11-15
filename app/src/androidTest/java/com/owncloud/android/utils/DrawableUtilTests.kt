/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test

class DrawableUtilTests {

    private var context: Context? = null

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().context
    }

    @Test
    fun testAddDrawableAsOverlayWhenGivenValidDrawablesShouldContainTwoDrawable() {
        val bitmap: Bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        val drawable = BitmapDrawable(context?.resources, bitmap)

        val layerDrawable = DrawableUtil.addDrawableAsOverlay(drawable, drawable)

        assert(layerDrawable.numberOfLayers == 2)
    }

    @After
    fun destroy() {
        context = null
    }
}
