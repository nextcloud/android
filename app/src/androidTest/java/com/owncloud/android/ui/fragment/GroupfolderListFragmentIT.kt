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

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.nextcloud.android.lib.resources.groupfolders.Groupfolder
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class GroupfolderListFragmentIT : AbstractIT() {
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
    fun showGroupFolder() {
        val sut = GroupfolderListFragment()
        scenario = activityRule.scenario
        scenario.onActivity { activity ->
            activity.addFragment(sut)

            onIdleSync {
                activity.runOnUiThread {
                    sut.setAdapter(null)
                    sut.setData(
                        mapOf(
                            Pair("2", Groupfolder(2, "/subfolder/group"))
                        )
                    )
                }

                shortSleep()
                screenshot(activity)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun showGroupFolders() {
        val sut = GroupfolderListFragment()
        scenario = activityRule.scenario
        scenario.onActivity { activity ->
            activity.addFragment(sut)

            shortSleep() // to let async task finish
            onIdleSync {
                activity.runOnUiThread {
                    sut.setAdapter(null)
                    sut.setData(
                        mapOf(
                            Pair("1", Groupfolder(1, "/test/")),
                            Pair("2", Groupfolder(2, "/subfolder/group"))
                        )
                    )
                }

                shortSleep()
                screenshot(activity)
            }
        }
    }
}
