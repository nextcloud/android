/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Philipp Hasper <vcs@hasper.info>
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
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.datamodel.ThumbnailsCacheManager.PREFIX_RESIZED_IMAGE
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.model.ImageDimension
import com.owncloud.android.ui.adapter.GalleryRowHolder
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import java.util.Random
import org.hamcrest.Matchers.`is` as isSameView

class GalleryFragmentIT : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.fragment.GalleryFragmentIT"
    private val random = Random(1)

    @Before
    fun registerIdlingResource() {
        // initialise thumbnails cache on background thread
        @Suppress("DEPRECATION")
        ThumbnailsCacheManager.initDiskCacheAsync()
    }

    @After
    fun unregisterIdlingResource() {
        ThumbnailsCacheManager.clearCache()
    }

    @Test
    @ScreenshotTest
    fun showEmpty() {
        launchActivity<TestActivity>().use { scenario ->
            var activity: TestActivity? = null
            scenario.onActivity { testActivity ->
                activity = testActivity
                val sut = GalleryFragment()
                activity.addFragment(sut)
            }

            val screenShotName = createName(testClassName + "_" + "showEmpty", "")
            onView(isRoot()).check(matches(isDisplayed()))
            screenshotViaName(activity, screenShotName)
        }
    }

    @Test
    @ScreenshotTest
    fun showGallery() {
        launchActivity<TestActivity>().use { scenario ->
            var activity: TestActivity? = null
            scenario.onActivity { testActivity ->
                activity = testActivity
                createImage(10000001, 700, 300)
                createImage(10000002, 500, 300)
                createImage(10000007, 300, 400)

                val sut = GalleryFragment()
                activity.addFragment(sut)
            }

            val screenShotName = createName(testClassName + "_" + "showGallery", "")
            onView(isRoot()).check(matches(isDisplayed()))
            screenshotViaName(activity, screenShotName)
        }
    }

    @Test
    fun multiSelect() {
        val imageCount = 100
        for (num in 1..imageCount) {
            // Spread the files over multiple days to also get multiple sections
            val secondsPerDay = 1L * 24 * 60 * 60
            createImage(10000000 + num * 7 * secondsPerDay, 700, 300)
        }

        // Test that scrolling through the whole list is possible without a crash
        launchActivity<TestActivity>().use { scenario ->
            lateinit var galleryFragment: GalleryFragment
            scenario.onActivity { testActivity ->
                galleryFragment = GalleryFragment()
                testActivity.addFragment(galleryFragment)
            }
            onView(isRoot()).check(matches(isDisplayed()))

            onView(withId(R.id.list_root))
                .perform(RecyclerViewActions.scrollToLastPosition<GalleryRowHolder>())
                .perform(RecyclerViewActions.scrollToPosition<GalleryRowHolder>(0))
        }

        // Test selection of all entries
        launchActivity<TestActivity>().use { scenario ->
            lateinit var galleryFragment: GalleryFragment
            scenario.onActivity { testActivity ->
                galleryFragment = GalleryFragment()
                testActivity.addFragment(galleryFragment)
            }
            onView(isRoot()).check(matches(isDisplayed()))

            // get the RecyclerView and itemCount on the UI thread
            val recyclerView = findRecyclerViewRecursively(galleryFragment.view)
                ?: throw AssertionError("RecyclerView not found")
            val adapterCount = recyclerView.adapter?.itemCount ?: 0

            // Perform the view action on each adapter position (row)
            for (pos in 0 until adapterCount) {
                onView(isSameView(recyclerView))
                    .perform(actionOnItemAtPosition<RecyclerView.ViewHolder>(pos, longClickAllThumbnailsInRow()))
            }

            val checked = galleryFragment.commonAdapter.getCheckedItems()
            assertEquals(imageCount, checked.size)
        }
    }

    /** Recursively walk view tree to find the first RecyclerView. Runs on the same thread that calls it. */
    @Suppress("ReturnCount")
    private fun findRecyclerViewRecursively(root: View?): RecyclerView? {
        if (root == null) return null
        if (root is RecyclerView) return root
        if (root !is ViewGroup) return null
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            val found = findRecyclerViewRecursively(child)
            if (found != null) return found
        }
        return null
    }

    /** For the given row view, long-click each thumbnail inside its FrameLayouts */
    @Suppress("NestedBlockDepth")
    fun longClickAllThumbnailsInRow() = object : ViewAction {
        override fun getConstraints() = isDisplayed()

        override fun getDescription() = "Long-click all thumbnail ImageViews inside a GalleryRowHolder"

        override fun perform(uiController: UiController, view: View) {
            if (view is ViewGroup) {
                // each child of the row is a FrameLayout representing one gallery cell
                for (i in 0 until view.childCount) {
                    val cell = view.getChildAt(i)
                    if (cell is FrameLayout) {
                        // GalleryRowHolder builds FrameLayout with children:
                        // 0 = shimmer, 1 = thumbnail ImageView, 2 = checkbox
                        val thumbnail = if (cell.childCount > 1) cell.getChildAt(1) else cell
                        thumbnail.performLongClick()
                    }
                }
            }
        }
    }

    private fun createImage(id: Long, width: Int? = null, height: Int? = null) {
        val defaultSize = ThumbnailsCacheManager.getThumbnailDimension().toFloat()
        val file = OCFile("/$id.png").apply {
            fileId = id
            remoteId = "$id"
            mimeType = "image/png"
            isPreviewAvailable = true
            modificationTimestamp = (1658475504 + id) * 1000
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
