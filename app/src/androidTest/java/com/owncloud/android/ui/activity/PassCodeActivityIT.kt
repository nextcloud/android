/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity

import android.content.Intent
import androidx.test.espresso.Espresso
import androidx.test.espresso.intent.rule.IntentsTestRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Rule
import org.junit.Test

class PassCodeActivityIT : AbstractIT() {
    @get:Rule
    var activityRule = IntentsTestRule(PassCodeActivity::class.java, true, false)

    @Test
    @ScreenshotTest
    fun check() {
        val sut = activityRule.launchActivity(Intent(PassCodeActivity.ACTION_CHECK))

        waitForIdleSync()

        sut.runOnUiThread { sut.binding.txt0.clearFocus() }
        Espresso.closeSoftKeyboard()
        shortSleep()
        waitForIdleSync()

        screenshot(sut)
    }

    @Test
    @ScreenshotTest
    fun request() {
        val sut = activityRule.launchActivity(Intent(PassCodeActivity.ACTION_REQUEST_WITH_RESULT))

        waitForIdleSync()

        sut.runOnUiThread { sut.binding.txt0.clearFocus() }
        Espresso.closeSoftKeyboard()
        shortSleep()
        waitForIdleSync()

        screenshot(sut)
    }

    @Test
    @ScreenshotTest
    fun delete() {
        val sut = activityRule.launchActivity(Intent(PassCodeActivity.ACTION_CHECK_WITH_RESULT))

        waitForIdleSync()

        sut.runOnUiThread { sut.binding.txt0.clearFocus() }
        Espresso.closeSoftKeyboard()
        shortSleep()
        waitForIdleSync()

        screenshot(sut)
    }
}
