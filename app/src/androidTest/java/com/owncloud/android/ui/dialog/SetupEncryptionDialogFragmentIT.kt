/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.dialog

import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.ui.dialog.setupEncryption.SetupEncryptionDialogFragment
import com.owncloud.android.utils.EspressoIdlingResource
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class SetupEncryptionDialogFragmentIT : AbstractIT() {
    private val testClassName = "com.owncloud.android.ui.dialog.SetupEncryptionDialogFragmentIT"

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
    fun showMnemonic() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    val sut = SetupEncryptionDialogFragment.newInstance(user, 0)

                    sut.show(activity.supportFragmentManager, "1")

                    val keyWords = arrayListOf(
                        "ability",
                        "able",
                        "about",
                        "above",
                        "absent",
                        "absorb",
                        "abstract",
                        "absurd",
                        "abuse",
                        "access",
                        "accident",
                        "account",
                        "accuse"
                    )
                    sut.setMnemonic(keyWords)
                    sut.showMnemonicInfo()
                    EspressoIdlingResource.decrement()

                    val screenShotName = createName(testClassName + "_" + "showMnemonic", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut.requireDialog().window?.decorView, screenShotName)
                }
            }
        }
    }

    @Test
    @ScreenshotTest
    fun error() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
                onIdleSync {
                    EspressoIdlingResource.increment()

                    val sut = SetupEncryptionDialogFragment.newInstance(user, 0)
                    sut.show(activity.supportFragmentManager, "1")
                    sut.errorSavingKeys()

                    EspressoIdlingResource.decrement()

                    val screenShotName = createName(testClassName + "_" + "error", "")
                    onView(isRoot()).check(matches(isDisplayed()))
                    screenshotViaName(sut.requireDialog().window?.decorView, screenShotName)
                }
            }
        }
    }
}
