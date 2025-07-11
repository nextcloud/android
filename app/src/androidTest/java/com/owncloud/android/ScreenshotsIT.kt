/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android

import android.content.Intent
import androidx.annotation.UiThread
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.owncloud.android.lib.resources.files.CreateFolderRemoteOperation
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.SettingsActivity
import com.owncloud.android.ui.activity.SyncedFoldersActivity
import com.owncloud.android.utils.EspressoIdlingResource
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.ClassRule
import org.junit.Test
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
import tools.fastlane.screengrab.locale.LocaleTestRule

class ScreenshotsIT : AbstractOnServerIT() {

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun gridViewScreenshot() {
        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    onView(withId(R.id.switch_grid_view_button)).perform(click())
                    EspressoIdlingResource.decrement()

                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, "01_gridView")

                    // Switch back
                    onView(withId(R.id.switch_grid_view_button)).perform(click())
                }
            }
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun listViewScreenshot() {
        val path = "/Camera/"
        if (storageManager.getFileByEncryptedRemotePath(path) == null) {
            val result = CreateFolderRemoteOperation(path, true).execute(client)
            assert(result.isSuccess)
        }

        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    onView(withId(R.id.list_root)).perform(click())
                    EspressoIdlingResource.decrement()

                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, "02_listView")
                }
            }
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun drawerScreenshot() {
        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
                    EspressoIdlingResource.decrement()

                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, "03_drawer")

                    onView(withId(R.id.drawer_layout)).perform(DrawerActions.close())
                }
            }
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun multipleAccountsScreenshot() {
        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    onView(withId(R.id.switch_account_button)).perform(click())
                    EspressoIdlingResource.decrement()

                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, "04_accounts")

                    Espresso.pressBack()
                }
            }
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun autoUploadScreenshot() {
        launchActivity<SyncedFoldersActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, "05_autoUpload")
                }
            }
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun davdroidScreenshot() {
        val intent = Intent(targetContext, SettingsActivity::class.java)

        launchActivity<SettingsActivity>(intent).use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    onView(withText(R.string.prefs_category_more)).perform(scrollTo())
                    EspressoIdlingResource.decrement()

                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, "06_davdroid")
                }
            }
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
