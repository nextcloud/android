/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity

import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.facebook.testing.screenshot.internal.TestNameDetector
import com.owncloud.android.AbstractIT
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Test

class ReceiveExternalFilesActivityIT : AbstractIT() {

    @Test
    @ScreenshotTest
    fun open() {
        // Screenshot name must be constructed outside of the scenario, otherwise it will not be reliably detected
        val screenShotName = TestNameDetector.getTestClass() + "_" + TestNameDetector.getTestName()
        launchActivity<ReceiveExternalFilesActivity>().use { scenario ->
            onView(isRoot()).check(matches(isDisplayed()))

            scenario.onActivity { sut ->
                screenshotViaName(sut, screenShotName)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun openMultiAccount() {
        val secondAccount = createAccount("secondtest@https://nextcloud.localhost")
        open()
        removeAccount(secondAccount)
    }
}
