/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Nextcloud GmbH
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
