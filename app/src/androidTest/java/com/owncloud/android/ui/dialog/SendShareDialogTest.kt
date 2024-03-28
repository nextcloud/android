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

            onIdleSync {
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                shortSleep()
                shortSleep()
                sut.requireDialog().window?.decorView.let { screenshot(it) }
            }
        }
    }
}
