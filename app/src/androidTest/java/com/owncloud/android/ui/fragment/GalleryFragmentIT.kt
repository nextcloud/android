/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2022 Tobias Kaminsky
 * Copyright (C) 2022 Nextcloud GmbH
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

package com.owncloud.android.ui.fragment

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.espresso.intent.rule.IntentsTestRule
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.ImageDimension
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.ThumbnailsCacheManager.PREFIX_RESIZED_IMAGE
import com.owncloud.android.lib.common.utils.Log_OC
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Random

class GalleryFragmentIT : AbstractIT() {
    @get:Rule
    val testActivityRule = IntentsTestRule(TestActivity::class.java, true, false)

    lateinit var activity: TestActivity
    val random = Random()

    @Before
    fun before() {
        activity = testActivityRule.launchActivity(null)

        createImage(10000001, true, 700, 300)
        createImage(10000002, true, 500, 300)

        createImage(10000007, true, 300, 400)
    }

    @After
    override fun after() {
        ThumbnailsCacheManager.clearCache()

        super.after()
    }

    @Test
    fun showGallery() {
        val sut = GalleryFragment()
        activity.addFragment(sut)

        longSleep()
    }

    private fun createImage(id: Int, createPreview: Boolean = true, width: Int? = null, height: Int? = null) {
        val defaultSize = ThumbnailsCacheManager.getThumbnailDimension().toFloat()
        val file = OCFile("/$id.png").apply {
            fileId = id.toLong()
            remoteId = "$id"
            mimeType = "image/png"
            isPreviewAvailable = true
            modificationTimestamp = (1658475504 + id.toLong()) * 1000
            imageDimension = ImageDimension(width?.toFloat() ?: defaultSize, height?.toFloat() ?: defaultSize)
            storageManager.saveFile(this)
        }

        if (!createPreview) {
            return
        }

        // create dummy thumbnail
        var w: Int
        var h: Int
        if (width == null || height == null) {
            if (random.nextBoolean()) {
                // portrait
                w = (random.nextInt(3) + 2) * 100 // 200-400
                h = (random.nextInt(5) + 4) * 100 // 400-800
            } else {
                // landscape
                w = (random.nextInt(5) + 4) * 100 // 400-800
                h = (random.nextInt(3) + 2) * 100 // 200-400
            }
        } else {
            w = width
            h = height
        }

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256))
            drawCircle(w / 2f, h / 2f, Math.min(w, h) / 2f, Paint().apply { color = Color.BLACK })
        }
        ThumbnailsCacheManager.addBitmapToCache(PREFIX_RESIZED_IMAGE + file.remoteId, bitmap)

        assertNotNull(ThumbnailsCacheManager.getBitmapFromDiskCache(PREFIX_RESIZED_IMAGE + file.remoteId))

        Log_OC.d("Gallery_thumbnail", "created $id with ${bitmap.width} x ${bitmap.height}")
    }
}
