/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
    fun showGroupfolder() {
        val sut = GroupfolderListFragment()
        scenario = activityRule.scenario
        scenario.onActivity { activity ->
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

            onIdleSync {
                shortSleep()
                screenshot(activity)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun showGroupfolders() {
        val sut = GroupfolderListFragment()
        scenario = activityRule.scenario
        scenario.onActivity { activity ->
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

            onIdleSync {
                shortSleep()
                screenshot(activity)
            }
        }
    }
}
