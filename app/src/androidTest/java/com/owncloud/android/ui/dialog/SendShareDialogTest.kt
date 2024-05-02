/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.dialog

import androidx.fragment.app.FragmentManager
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.platform.app.InstrumentationRegistry
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Rule
import org.junit.Test

class SendShareDialogTest : AbstractIT() {
    @get:Rule
    val testActivityRule = IntentsTestRule(TestActivity::class.java, true, false)

    @Test
    @ScreenshotTest
    fun showDialog() {
        val activity = testActivityRule.launchActivity(null)

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
