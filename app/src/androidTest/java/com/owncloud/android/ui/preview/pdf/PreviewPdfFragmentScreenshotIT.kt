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
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Test

class PreviewPdfFragmentScreenshotIT : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.preview.pdf.PreviewPdfFragmentScreenshotIT"

    companion object {
        private const val PDF_FILE_ASSET = "test.pdf"
    }

    @Test
    @ScreenshotTest
    fun showPdf() {
        launchActivity<TestActivity>().use { scenario ->
            var activity: TestActivity? = null
            scenario.onActivity { testActivity ->
                activity = testActivity
                val pdfFile = getFile(PDF_FILE_ASSET)
                val ocFile = OCFile("/test.pdf").apply {
                    storagePath = pdfFile.absolutePath
                }

                val sut = PreviewPdfFragment.newInstance(ocFile)
                activity.addFragment(sut)
                activity.supportFragmentManager.executePendingTransactions()
                sut.dismissSnack()
            }

            val screenShotName = createName(testClassName + "_" + "showPdf", "")
            onView(isRoot()).check(matches(isDisplayed()))
            screenshotViaName(activity, screenShotName)
        }
    }
}
