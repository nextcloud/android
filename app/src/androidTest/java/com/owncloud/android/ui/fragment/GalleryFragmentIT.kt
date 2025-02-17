/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.espresso.intent.rule.IntentsTestRule
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.ThumbnailsCacheManager.InitDiskCacheTask
import com.owncloud.android.datamodel.ThumbnailsCacheManager.PREFIX_RESIZED_IMAGE
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.model.ImageDimension
import com.owncloud.android.utils.ScreenshotTest
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
    val random = Random(1)

    @Before
    fun before() {
        activity = testActivityRule.launchActivity(null)

        // initialise thumbnails cache on background thread
        InitDiskCacheTask().execute()
    }

    @After
    override fun after() {
        ThumbnailsCacheManager.clearCache()

        super.after()
    }

    @ScreenshotTest
    @Test
    fun showEmpty() {
        val sut = GalleryFragment()
        activity.addFragment(sut)

        waitForIdleSync()

        screenshot(activity)
    }

    @Test
    @ScreenshotTest
    fun showGallery() {
        createImage(10000001, 700, 300)
        createImage(10000002, 500, 300)
        createImage(10000007, 300, 400)

        val sut = GalleryFragment()
        activity.addFragment(sut)

        waitForIdleSync()
        shortSleep()
        screenshot(activity)
    }

    private fun createImage(id: Int, width: Int? = null, height: Int? = null) {
        val defaultSize = ThumbnailsCacheManager.getThumbnailDimension().toFloat()
        val file = OCFile("/$id.png").apply {
            fileId = id.toLong()
            fileLength = (0..100).random().toLong()
            remoteId = "$id"
            mimeType = "image/png"
            isPreviewAvailable = true
            modificationTimestamp = (1658475504 + id.toLong()) * 1000
            imageDimension = ImageDimension(width?.toFloat() ?: defaultSize, height?.toFloat() ?: defaultSize)
            storageManager.saveFile(this)
        }

        // create dummy thumbnail
        val w: Int
        val h: Int
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
            drawCircle(w / 2f, h / 2f, w.coerceAtMost(h) / 2f, Paint().apply { color = Color.BLACK })
        }
        ThumbnailsCacheManager.addBitmapToCache(PREFIX_RESIZED_IMAGE + file.remoteId, bitmap)

        assertNotNull(ThumbnailsCacheManager.getBitmapFromDiskCache(PREFIX_RESIZED_IMAGE + file.remoteId))

        Log_OC.d("Gallery_thumbnail", "created $id with ${bitmap.width} x ${bitmap.height}")
    }
}
