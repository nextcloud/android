/*
 *
 * Nextcloud Android client application
 *
 * @author TSI-mc
 * Copyright (C) 2023 TSI-mc
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

package com.nmc.android.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherActivityIT : AbstractIT() {

    @get:Rule
    val activityRule = ActivityScenarioRule(LauncherActivity::class.java)

    @Test
    fun testSplashScreenWithEmptyTitlesShouldHideTitles() {
        waitForIdleSync()

        onView(withId(R.id.ivSplash)).check(matches(isCompletelyDisplayed()))

        onView(withId(R.id.splashScreenBold)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
        onView(withId(R.id.splashScreenNormal)).check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))
    }

    @Test
    fun testSplashScreenWithTitlesShouldShowTitles() {
        waitForIdleSync()
        onView(withId(R.id.ivSplash)).check(matches(isCompletelyDisplayed()))

        activityRule.scenario.onActivity {
            it.setSplashTitles("Example", "Cloud")
        }

        val onePercentArea = ViewMatchers.isDisplayingAtLeast(1)
        onView(withId(R.id.splashScreenBold)).check(matches(onePercentArea))
        onView(withId(R.id.splashScreenNormal)).check(matches(onePercentArea))
    }
}
