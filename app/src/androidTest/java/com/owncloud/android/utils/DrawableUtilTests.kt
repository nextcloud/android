/*
 * Nextcloud Android client application
 *
 * @author Alper Ozturk
 * Copyright (C) 2023 Alper Ozturk
 * Copyright (C) 2023 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
        val bitmap: Bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
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
