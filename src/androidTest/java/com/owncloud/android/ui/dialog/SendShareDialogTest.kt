/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.dialog

import androidx.fragment.app.FragmentManager
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.platform.app.InstrumentationRegistry
import com.nextcloud.client.TestActivity
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
