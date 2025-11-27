/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 TSI-mc
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nmc.android.ui

import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.utils.EspressoIdlingResource
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherActivityIT : AbstractIT() {

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    @Test
    fun testSplashScreenWithEmptyTitlesShouldHideTitles() {
        launchActivity<LauncherActivity>().use { scenario ->
            scenario.onActivity { _ ->
                onIdleSync {
                    onView(withId(R.id.ivSplash)).check(matches(isCompletelyDisplayed()))
                    onView(
                        withId(R.id.splashScreenBold)
                    ).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
                    onView(
                        withId(R.id.splashScreenNormal)
                    ).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
                }
            }
        }
    }

    @Test
    fun testSplashScreenWithTitlesShouldShowTitles() {
        launchActivity<LauncherActivity>().use { scenario ->
            scenario.onActivity {
                onIdleSync {
                    onView(withId(R.id.ivSplash)).check(matches(isCompletelyDisplayed()))

                    EspressoIdlingResource.increment()
                    it.setSplashTitles("Example", "Cloud")
                    EspressoIdlingResource.decrement()

                    val onePercentArea = ViewMatchers.isDisplayingAtLeast(1)
                    onView(withId(R.id.splashScreenBold)).check(matches(onePercentArea))
                    onView(withId(R.id.splashScreenNormal)).check(matches(onePercentArea))
                }
            }
        }
    }
}
