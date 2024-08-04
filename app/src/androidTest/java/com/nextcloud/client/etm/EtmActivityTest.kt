/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.etm

import android.app.Activity
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import com.owncloud.android.AbstractIT
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Rule
import org.junit.Test

class EtmActivityTest : AbstractIT() {
    @get:Rule
    var activityRule = IntentsTestRule(EtmActivity::class.java, true, false)

    @Test
    @ScreenshotTest
    fun overview() {
        val sut: Activity = activityRule.launchActivity(null)

        waitForIdleSync()

        screenshot(sut)
    }

    @Test
    @ScreenshotTest
    fun accounts() {
        val sut: EtmActivity = activityRule.launchActivity(null)

        UiThreadStatement.runOnUiThread { sut.vm.onPageSelected(1) }

        screenshot(sut)
    }
}
