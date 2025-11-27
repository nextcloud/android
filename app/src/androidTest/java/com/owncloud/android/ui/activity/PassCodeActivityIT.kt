/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity

import android.content.Intent
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.owncloud.android.AbstractIT
import com.owncloud.android.utils.EspressoIdlingResource
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class PassCodeActivityIT : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.activity.PassCodeActivityIT"

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
    fun check() {
        val intent = Intent(targetContext, PassCodeActivity::class.java).apply {
            action = PassCodeActivity.ACTION_CHECK
        }

        launchActivity<PassCodeActivity>(intent).use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    sut.binding.txt0.clearFocus()
                    Espresso.closeSoftKeyboard()
                    EspressoIdlingResource.decrement()

                    val screenShotName = createName(testClassName + "_" + "check", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }

    @Test
    @ScreenshotTest
    fun request() {
        val intent = Intent(targetContext, PassCodeActivity::class.java).apply {
            action = PassCodeActivity.ACTION_REQUEST_WITH_RESULT
        }

        launchActivity<PassCodeActivity>(intent).use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    sut.binding.txt0.clearFocus()
                    Espresso.closeSoftKeyboard()
                    EspressoIdlingResource.decrement()

                    val screenShotName = createName(testClassName + "_" + "request", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }

    @Test
    @ScreenshotTest
    fun delete() {
        val intent = Intent(targetContext, PassCodeActivity::class.java).apply {
            action = PassCodeActivity.ACTION_CHECK_WITH_RESULT
        }

        launchActivity<PassCodeActivity>(intent).use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    sut.binding.txt0.clearFocus()
                    Espresso.closeSoftKeyboard()
                    EspressoIdlingResource.decrement()

                    val screenShotName = createName(testClassName + "_" + "delete", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut, screenShotName)
                }
            }
        }
    }
}
