/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.annotation.UiThread
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.ThumbnailsCacheManager.InitDiskCacheTask
import com.owncloud.android.datamodel.ThumbnailsCacheManager.PREFIX_RESIZED_IMAGE
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.model.ImageDimension
import com.owncloud.android.utils.EspressoIdlingResource
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.util.Random

class GalleryFragmentIT : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.fragment.GalleryFragmentIT"
    val random = Random(1)

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)

        // initialise thumbnails cache on background thread
        @Suppress("DEPRECATION")
        InitDiskCacheTask().execute()
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        ThumbnailsCacheManager.clearCache()
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun showEmpty() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    val sut = GalleryFragment()
                    activity.addFragment(sut)
                    EspressoIdlingResource.decrement()

                    val screenShotName = createName(testClassName + "_" + "showEmpty", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(activity, screenShotName)
                }
            }
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun showGallery() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    createImage(10000001, 700, 300)
                    createImage(10000002, 500, 300)
                    createImage(10000007, 300, 400)

                    val sut = GalleryFragment()
                    activity.addFragment(sut)
                    EspressoIdlingResource.decrement()

                    val screenShotName = createName(testClassName + "_" + "showGallery", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(activity, screenShotName)
                }
            }
        }
    }

    private fun createImage(id: Int, width: Int? = null, height: Int? = null) {
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
