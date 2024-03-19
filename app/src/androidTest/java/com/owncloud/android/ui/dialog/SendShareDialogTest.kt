/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.dialog

import android.content.Intent
import androidx.fragment.app.FragmentManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class SendShareDialogTest : AbstractIT() {
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
    fun showDialog() {
        scenario = activityRule.scenario
        scenario.onActivity { activity ->
            val fm: FragmentManager = activity.supportFragmentManager
            val ft = fm.beginTransaction()
            ft.addToBackStack(null)

            val file = OCFile("/1.jpg").apply {
                mimeType = "image/jpg"
            }

            val sut = SendShareDialog.newInstance(file, false, OCCapability())
            sut.show(ft, "TAG_SEND_SHARE_DIALOG")

            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            shortSleep()
            shortSleep()
            sut.requireDialog().window?.decorView.let { screenshot(it) }
        }
    }
}
