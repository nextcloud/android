/*
 * Nextcloud Android Library is available under MIT license
 * @author Alper Öztürk
 * Copyright (C) 2023 Alper Öztürk
 * Copyright (C) 2023 Nextcloud GmbH
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *  BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *  ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package com.owncloud.android.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class DrawableUtilTests {

    private var sut: DrawableUtil? = null
    private var context: Context? = null

    @Before
    fun setUp() {
        sut = DrawableUtil()
        context = InstrumentationRegistry.getInstrumentation().context
    }

    @Test
    fun testAddDrawableAsOverlayWhenGivenValidDrawablesShouldContainTwoDrawable() {
        val bitmapSize = 10
        val bitmap: Bitmap = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888)
        val drawable = BitmapDrawable(context?.resources, bitmap)

        val layerDrawable = sut?.addDrawableAsOverlay(drawable, drawable)

        if (layerDrawable == null) {
            fail("Layer drawable expected to be not null")
        }

        assert(layerDrawable?.numberOfLayers == 2)
    }

    @After
    fun destroy() {
        sut = null
        context = null
    }
}
