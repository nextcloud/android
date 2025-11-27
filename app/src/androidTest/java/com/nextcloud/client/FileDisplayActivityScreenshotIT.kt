/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client

import android.Manifest
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.rule.GrantPermissionRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.fragment.EmptyListState
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class FileDisplayActivityScreenshotIT : AbstractIT() {
    private val testClassName = "com.nextcloud.client.FileDisplayActivityScreenshotIT"

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.POST_NOTIFICATIONS
    )

    companion object {
        private const val TAG = "FileDisplayActivityScreenshotIT"
    }

    @Test
    @ScreenshotTest
    fun open() {
        try {
            launchActivity<FileDisplayActivity>().use { scenario ->
                scenario.onActivity { sut ->
                    sut.run {
                        listOfFilesFragment?.let {
                            it.setFabEnabled(false)
                            resetScrolling(true)
                            it.setEmptyListMessage(EmptyListState.LOADING)
                            it.isLoading = false
                        }
                    }
                }

                val screenShotName = createName(testClassName + "_" + "open", "")
                onView(isRoot()).check(matches(isDisplayed()))

                scenario.onActivity { sut ->
                    screenshotViaName(sut, screenShotName)
                }
            }
        } catch (e: SecurityException) {
            Log_OC.e(TAG, "Error caught at open $e")
        }
    }

    @Test
    @ScreenshotTest
    fun showMediaThenAllFiles() {
        try {
            launchActivity<FileDisplayActivity>().use { scenario ->
                var activity: FileDisplayActivity? = null
                scenario.onActivity { sut ->
                    activity = sut
                    val fragment = sut.listOfFilesFragment
                    Assert.assertNotNull(fragment)
                    fragment!!.setFabEnabled(false)
                    fragment.setEmptyListMessage(EmptyListState.LOADING)
                    fragment.isLoading = false
                }

                onView(ViewMatchers.withId(R.id.drawer_layout)).perform(DrawerActions.open())
                onView(ViewMatchers.withId(R.id.nav_view))
                    .perform(NavigationViewActions.navigateTo(R.id.nav_gallery))
                onView(ViewMatchers.withId(R.id.drawer_layout)).perform(DrawerActions.open())
                onView(ViewMatchers.withId(R.id.nav_view))
                    .perform(NavigationViewActions.navigateTo(R.id.nav_all_files))

                val fragment = activity!!.listOfFilesFragment
                fragment!!.setFabEnabled(false)
                fragment.setEmptyListMessage(EmptyListState.LOADING)
                fragment.isLoading = false

                val screenShotName = createName(testClassName + "_" + "showMediaThenAllFiles", "")
                onView(isRoot()).check(matches(isDisplayed()))
                screenshotViaName(activity, screenShotName)
            }
        } catch (e: SecurityException) {
            Log_OC.e(TAG, "Error caught at open $e")
        }
    }

    @Test
    @ScreenshotTest
    fun drawer() {
        try {
            launchActivity<FileDisplayActivity>().use { scenario ->
                onView(ViewMatchers.withId(R.id.drawer_layout)).perform(DrawerActions.open())

                scenario.onActivity { sut ->
                    sut.run {
                        hideInfoBox()
                        resetScrolling(true)

                        listOfFilesFragment?.let {
                            it.setFabEnabled(false)
                            it.setEmptyListMessage(EmptyListState.LOADING)
                            it.isLoading = false
                        }
                    }
                }

                val screenShotName = createName(testClassName + "_" + "drawer", "")
                onView(isRoot()).check(matches(isDisplayed()))

                scenario.onActivity { sut ->
                    screenshotViaName(sut, screenShotName)
                }
            }
        } catch (e: SecurityException) {
            Log_OC.e(TAG, "Error caught at open $e")
        }
    }
}
