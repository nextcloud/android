/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity

import androidx.annotation.UiThread
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.owncloud.android.AbstractIT
import com.owncloud.android.lib.common.Quota
import com.owncloud.android.lib.common.UserInfo
import com.owncloud.android.utils.EspressoIdlingResource
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class ManageAccountsActivityIT : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.activity.ManageAccountsActivityIT"

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
    fun open() {
        launchActivity<ManageAccountsActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    val screenShotName = createName(testClassName + "_" + "open", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }

    @Test
    @UiThread
    @ScreenshotTest
    fun userInfoDetail() {
        launchActivity<ManageAccountsActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    EspressoIdlingResource.increment()

                    val user = sut.accountManager.user

                    val userInfo = UserInfo(
                        "test",
                        true,
                        "Test User",
                        "test@nextcloud.com",
                        "+49 123 456",
                        "Address 123, Berlin",
                        "https://www.nextcloud.com",
                        "https://twitter.com/Nextclouders",
                        Quota(),
                        ArrayList()
                    )
                    EspressoIdlingResource.decrement()

                    sut.showUser(user, userInfo)

                    val screenShotName = createName(testClassName + "_" + "open", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(getCurrentActivity(), screenShotName)
                }
            }
        }
    }
}
