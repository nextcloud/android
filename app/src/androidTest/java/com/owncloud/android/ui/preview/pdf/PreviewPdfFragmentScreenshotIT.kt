/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.preview.pdf

import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.EspressoIdlingResource
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class PreviewPdfFragmentScreenshotIT : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.preview.pdf.PreviewPdfFragmentScreenshotIT"

    companion object {
        private const val PDF_FILE_ASSET = "test.pdf"
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
    @ScreenshotTest
    fun showPdf() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    val pdfFile = getFile(PDF_FILE_ASSET)
                    val ocFile = OCFile("/test.pdf").apply {
                        storagePath = pdfFile.absolutePath
                    }

                    val sut = PreviewPdfFragment.newInstance(ocFile)
                    activity.addFragment(sut)
                    sut.dismissSnack()

                    EspressoIdlingResource.decrement()

                    val screenShotName = createName(testClassName + "_" + "showPdf", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(activity, screenShotName)
                }
            }
        }
    }
}
