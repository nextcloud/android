/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

class FileDisplayActivityScreenshotIT : AbstractIT() {
    private lateinit var scenario: ActivityScenario<FileDisplayActivity>
    val intent = Intent(ApplicationProvider.getApplicationContext(), FileDisplayActivity::class.java)

    @get:Rule
    val activityRule = ActivityScenarioRule<FileDisplayActivity>(intent)

    @After
    fun cleanup() {
        scenario.close()
    }

    @Test
    @ScreenshotTest
    fun open() {
        scenario = activityRule.scenario
        scenario.onActivity { sut ->
            shortSleep()
            sut.runOnUiThread {
                sut.listOfFilesFragment!!.setFabEnabled(false)
                sut.resetScrolling(true)
                sut.listOfFilesFragment!!.setEmptyListLoadingMessage()
                sut.listOfFilesFragment!!.isLoading = false
            }
            shortSleep()
            onIdleSync {
                screenshot(sut)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun showMediaThenAllFiles() {
        scenario = activityRule.scenario
        scenario.onActivity { fileDisplayActivity ->
            val sut = fileDisplayActivity.listOfFilesFragment
            Assert.assertNotNull(sut)
            sut!!.setFabEnabled(false)
            sut.setEmptyListLoadingMessage()
            sut.isLoading = false

            onIdleSync {
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
        }
    }

    @Test
    @ScreenshotTest
    fun drawer() {
        scenario = activityRule.scenario
        scenario.onActivity { sut ->
            onIdleSync {
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
                screenshot(sut)
            }
        }
    }
}
