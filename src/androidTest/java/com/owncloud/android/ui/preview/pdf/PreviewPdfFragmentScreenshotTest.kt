/*
 *  Nextcloud Android Library is available under MIT license
 *
 *  @author Álvaro Brey Vilas
 *  Copyright (C) 2022 Álvaro Brey Vilas
 *  Copyright (C) 2022 Nextcloud GmbH
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 *  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
 *  BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 *  ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package com.owncloud.android.ui.preview.pdf

import androidx.lifecycle.Lifecycle
import androidx.test.espresso.intent.rule.IntentsTestRule
import com.nextcloud.client.TestActivity
import com.nextcloud.client.preferences.AppPreferences
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Rule
import org.junit.Test

class PreviewPdfFragmentScreenshotTest : AbstractIT() {

    companion object {
        private const val PDF_FILE_ASSET = "test.pdf"
    }

    @get:Rule
    val testActivityRule = IntentsTestRule(TestActivity::class.java, true, false)

    @Test
    @ScreenshotTest
    fun showPdf() {
        val activity = testActivityRule.launchActivity(null)

        val preferences: AppPreferences = mock()
        whenever(preferences.isPdfZoomTipShown()).thenReturn(true)

        val vm = PreviewPdfViewModel(preferences)

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
            sut.setViewModel(vm)
            vm.process(ocFile)
        }

        shortSleep()
        waitForIdleSync()

        screenshot(activity)
    }
}
