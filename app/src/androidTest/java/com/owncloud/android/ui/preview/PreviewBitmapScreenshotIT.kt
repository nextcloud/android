/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.preview

import android.content.Intent
import androidx.annotation.UiThread
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.owncloud.android.AbstractIT
import com.owncloud.android.utils.EspressoIdlingResource
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class PreviewBitmapScreenshotIT : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.preview.PreviewBitmapScreenshotIT"

    companion object {
        private const val PNG_FILE_ASSET = "imageFile.png"
    }

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
    fun showBitmap() {
        val pngFile = getFile(PNG_FILE_ASSET)
        val intent = Intent(targetContext, PreviewBitmapActivity::class.java).putExtra(
            PreviewBitmapActivity.EXTRA_BITMAP_PATH,
            pngFile.absolutePath
        )

        launchActivity<PreviewBitmapActivity>(intent).use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    val screenShotName = createName(testClassName + "_" + "showBitmap", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }
}
