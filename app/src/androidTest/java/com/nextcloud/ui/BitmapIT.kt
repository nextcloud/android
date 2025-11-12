/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.ui

import android.graphics.BitmapFactory
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.UiThread
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.utils.BitmapUtils
import com.owncloud.android.utils.EspressoIdlingResource
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class BitmapIT : AbstractIT() {
    private val testClassName = "com.nextcloud.ui.BitmapIT"

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun roundBitmap() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    val file = getFile("christine.jpg")
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)

                    val imageView = ImageView(activity).apply {
                        setImageBitmap(bitmap)
                    }

                    val bitmap2 = BitmapFactory.decodeFile(file.absolutePath)
                    val imageView2 = ImageView(activity).apply {
                        setImageBitmap(BitmapUtils.roundBitmap(bitmap2))
                    }

                    val linearLayout = LinearLayout(activity).apply {
                        orientation = LinearLayout.VERTICAL
                        setBackgroundColor(context.getColor(R.color.grey_200))
                    }
                    linearLayout.addView(imageView, 200, 200)
                    linearLayout.addView(imageView2, 200, 200)
                    activity.addView(linearLayout)
                    EspressoIdlingResource.decrement()

                    val screenShotName = createName(testClassName + "_" + "roundBitmap", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(activity, screenShotName)
                }
            }
        }
    }
}
