/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro.brey@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.preview.pdf

import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class PreviewPdfFragmentScreenshotIT : AbstractIT() {

    companion object {
        private const val PDF_FILE_ASSET = "test.pdf"
    }

    private lateinit var scenario: ActivityScenario<TestActivity>
    val intent = Intent(ApplicationProvider.getApplicationContext(), TestActivity::class.java)

    @get:Rule
    val activityRule = ActivityScenarioRule<TestActivity>(intent)

    @After
    fun cleanup() {
        scenario.close()
    }

    @Test
    @ScreenshotTest
    fun showPdf() {
        scenario = activityRule.scenario
        scenario.onActivity { activity ->
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
            onIdleSync {
                screenshot(activity)
            }
        }
    }
}
