/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2023 Tobias Kaminsky
 * Copyright (C) 2023 Nextcloud GmbH
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

package com.owncloud.android.ui.fragment

import androidx.test.espresso.intent.rule.IntentsTestRule
import com.nextcloud.android.lib.resources.groupfolders.Groupfolder
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GroupfolderListFragmentIT : AbstractIT() {
    @get:Rule
    val testActivityRule = IntentsTestRule(TestActivity::class.java, true, false)

    lateinit var activity: TestActivity

    @Before
    fun before() {
        activity = testActivityRule.launchActivity(null)
    }

    @Test
    @ScreenshotTest
    fun showGroupfolder() {
        val sut = GroupfolderListFragment()
        activity.addFragment(sut)

        shortSleep() // to let async task finish

        activity.runOnUiThread {
            sut.setAdapter(null)
            sut.setData(
                mapOf(
                    Pair("2", Groupfolder(2, "/subfolder/group"))
                )
            )
        }

        waitForIdleSync()
        shortSleep()
        screenshot(activity)
    }

    @Test
    @ScreenshotTest
    fun showGroupfolders() {
        val sut = GroupfolderListFragment()
        activity.addFragment(sut)

        shortSleep() // to let async task finish

        activity.runOnUiThread {
            sut.setAdapter(null)
            sut.setData(
                mapOf(
                    Pair("1", Groupfolder(1, "/test/")),
                    Pair("2", Groupfolder(2, "/subfolder/group"))
                )
            )
        }

        waitForIdleSync()
        shortSleep()
        screenshot(activity)
    }
}
