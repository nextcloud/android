/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
