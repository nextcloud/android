/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment

import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.nextcloud.android.lib.resources.groupfolders.Groupfolder
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Test

class GroupfolderListFragmentIT : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.fragment.GroupfolderListFragmentIT"

    @Test
    @ScreenshotTest
    fun showGroupfolder() {
        launchActivity<TestActivity>().use { scenario ->
            var activity: TestActivity? = null
            scenario.onActivity { testActivity ->
                activity = testActivity
                val sut = GroupfolderListFragment()
                activity.addFragment(sut)
                activity.supportFragmentManager.executePendingTransactions()

                sut.setAdapter(null)
                sut.setData(
                    mapOf(
                        Pair("2", Groupfolder(2, "/subfolder/group"))
                    )
                )
            }

            val screenShotName = createName(testClassName + "_" + "showGroupfolder", "")
            onView(isRoot()).check(matches(isDisplayed()))
            screenshotViaName(activity, screenShotName)
        }
    }

    @Test
    @ScreenshotTest
    fun showGroupfolders() {
        launchActivity<TestActivity>().use { scenario ->
            var activity: TestActivity? = null
            scenario.onActivity { testActivity ->
                activity = testActivity
                val sut = GroupfolderListFragment()
                activity.addFragment(sut)
                activity.supportFragmentManager.executePendingTransactions()
                sut.setAdapter(null)
                sut.setData(
                    mapOf(
                        Pair("1", Groupfolder(1, "/test/")),
                        Pair("2", Groupfolder(2, "/subfolder/group"))
                    )
                )
            }

            val screenShotName = createName(testClassName + "_" + "showGroupfolders", "")
            onView(isRoot()).check(matches(isDisplayed()))
            screenshotViaName(activity, screenShotName)
        }
    }
}
