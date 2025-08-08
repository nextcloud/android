/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.sso

import androidx.annotation.UiThread
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.owncloud.android.AbstractIT
import com.owncloud.android.ui.activity.SsoGrantPermissionActivity
import com.owncloud.android.utils.EspressoIdlingResource
import org.junit.After
import org.junit.Before
import org.junit.Test

class SSOActivityTests : AbstractIT() {

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
    fun testActivityTheme() {
        launchActivity<SsoGrantPermissionActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    assert(sut.binding != null)
                    assert(sut.materialAlertDialogBuilder != null)
                    onView(isRoot()).check(matches(isDisplayed()))
                }
            }
        }
    }
}
