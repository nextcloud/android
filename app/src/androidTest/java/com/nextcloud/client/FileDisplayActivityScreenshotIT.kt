/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client

import android.Manifest
import androidx.test.espresso.Espresso
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.rule.GrantPermissionRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class FileDisplayActivityScreenshotIT : AbstractIT() {
    @get:Rule
    val activityRule = IntentsTestRule(
        FileDisplayActivity::class.java,
        true,
        false
    )

    @get:Rule
    val permissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    @Test
    @ScreenshotTest
    fun open() {
        val sut = activityRule.launchActivity(null)

        shortSleep()
        sut.runOnUiThread {
            sut.listOfFilesFragment!!.setFabEnabled(false)
            sut.resetScrolling(true)
            sut.listOfFilesFragment!!.setEmptyListLoadingMessage()
            sut.listOfFilesFragment!!.isLoading = false
        }
        shortSleep()
        waitForIdleSync()
        screenshot(sut)
    }

    @Test
    @ScreenshotTest
    fun showMediaThenAllFiles() {
        val fileDisplayActivity = activityRule.launchActivity(null)
        val sut = fileDisplayActivity.listOfFilesFragment
        Assert.assertNotNull(sut)
        sut!!.setFabEnabled(false)
        sut.setEmptyListLoadingMessage()
        sut.isLoading = false

        // open drawer
        Espresso.onView(ViewMatchers.withId(R.id.drawer_layout)).perform(DrawerActions.open())

        // click "all files"
        Espresso.onView(ViewMatchers.withId(R.id.nav_view))
            .perform(NavigationViewActions.navigateTo(R.id.nav_gallery))

        // wait
        shortSleep()

        // click "all files"
        Espresso.onView(ViewMatchers.withId(R.id.drawer_layout)).perform(DrawerActions.open())
        Espresso.onView(ViewMatchers.withId(R.id.nav_view))
            .perform(NavigationViewActions.navigateTo(R.id.nav_all_files))

        // then compare screenshot
        shortSleep()
        sut.setFabEnabled(false)
        sut.setEmptyListLoadingMessage()
        sut.isLoading = false
        shortSleep()
        screenshot(fileDisplayActivity)
    }

    @Test
    @ScreenshotTest
    fun drawer() {
        val sut = activityRule.launchActivity(null)
        Espresso.onView(ViewMatchers.withId(R.id.drawer_layout)).perform(DrawerActions.open())

        shortSleep()
        sut.runOnUiThread {
            sut.hideInfoBox()
            sut.resetScrolling(true)
            sut.listOfFilesFragment!!.setFabEnabled(false)
            sut.listOfFilesFragment!!.setEmptyListLoadingMessage()
            sut.listOfFilesFragment!!.isLoading = false
        }
        shortSleep()
        waitForIdleSync()
        screenshot(sut)
    }
}
