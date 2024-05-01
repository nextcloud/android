/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.preview.pdf

import androidx.lifecycle.Lifecycle
import androidx.test.espresso.intent.rule.IntentsTestRule
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Rule
import org.junit.Test

class PreviewPdfFragmentScreenshotIT : AbstractIT() {

    companion object {
        private const val PDF_FILE_ASSET = "test.pdf"
    }

    @get:Rule
    val testActivityRule = IntentsTestRule(TestActivity::class.java, true, false)

    @Test
    @ScreenshotTest
    fun showPdf() {
        val activity = testActivityRule.launchActivity(null)

        val pdfFile = getFile(PDF_FILE_ASSET)
        val ocFile = OCFile("/test.pdf").apply {
            storagePath = pdfFile.absolutePath
        }

        val sut = PreviewPdfFragment.newInstance(ocFile)
        activity.addFragment(sut)

        while (!sut.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            shortSleep()
        }

        activity.runOnUiThread {
            sut.dismissSnack()
        }

        shortSleep()
        waitForIdleSync()

        screenshot(activity)
    }
}
