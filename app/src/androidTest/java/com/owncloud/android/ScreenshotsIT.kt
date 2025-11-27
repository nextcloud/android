/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android

import android.Manifest
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.GrantPermissionRule
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.SettingsActivity
import com.owncloud.android.ui.activity.SyncedFoldersActivity
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.locale.LocaleTestRule

class ScreenshotsIT : AbstractIT() {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.POST_NOTIFICATIONS
    )

    @Test
    @ScreenshotTest
    fun gridViewScreenshot() {
        launchActivity<FileDisplayActivity>().use {
            onView(withId(R.id.switch_grid_view_button)).perform(click())

            onView(isRoot()).check(matches(isDisplayed()))
            Screengrab.screenshot("01_gridView")

            // Switch back
            onView(withId(R.id.switch_grid_view_button)).perform(click())

            assertTrue(true)
        }
    }

    @Test
    @ScreenshotTest
    fun listViewScreenshot() {
        launchActivity<FileDisplayActivity>().use {
            val path = "/Camera/"
            OCFile(path).apply {
                storageManager.saveFile(this)
            }
            onView(withId(R.id.list_root)).perform(click())

            onView(isRoot()).check(matches(isDisplayed()))
            Screengrab.screenshot("02_listView")
            assertTrue(true)
        }
    }

    @Test
    @ScreenshotTest
    fun drawerScreenshot() {
        launchActivity<FileDisplayActivity>().use {
            onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())

            onView(isRoot()).check(matches(isDisplayed()))
            Screengrab.screenshot("03_drawer")

            onView(withId(R.id.drawer_layout)).perform(DrawerActions.close())
            assertTrue(true)
        }
    }

    @Test
    @ScreenshotTest
    fun multipleAccountsScreenshot() {
        launchActivity<FileDisplayActivity>().use {
            onView(withId(R.id.switch_account_button)).perform(click())

            onView(isRoot()).check(matches(isDisplayed()))
            Screengrab.screenshot("04_accounts")

            Espresso.pressBack()
            assertTrue(true)
        }
    }

    @Test
    @ScreenshotTest
    fun autoUploadScreenshot() {
        launchActivity<SyncedFoldersActivity>().use {
            onView(isRoot()).check(matches(isDisplayed()))
            Screengrab.screenshot("05_autoUpload")
            assertTrue(true)
        }
    }

    @Test
    @ScreenshotTest
    fun davdroidScreenshot() {
        launchActivity<SettingsActivity>().use {
            onView(withText(R.string.prefs_category_more)).perform(scrollTo())

            onView(isRoot()).check(matches(isDisplayed()))
            Screengrab.screenshot("06_davdroid")
            assertTrue(true)
        }
    }

    companion object {
        @ClassRule
        @JvmField
        val localeTestRule: LocaleTestRule = LocaleTestRule()

        @BeforeClass
        @JvmStatic
        fun beforeScreenshot() {
            Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())
        }
    }
}
