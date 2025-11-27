/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client

import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.ui.activity.UploadListActivity
import com.owncloud.android.utils.EspressoIdlingResource
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class UploadListActivityActivityIT : AbstractIT() {
    private val testClassName = "com.nextcloud.client.UploadListActivityActivityIT"

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    @Test
    @ScreenshotTest
    fun openDrawer() {
        launchActivity<UploadListActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    onView(isRoot()).check(matches(isDisplayed()))
                    onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())

                    val screenShotName = createName(testClassName + "_" + "openDrawer", "")
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }
}
