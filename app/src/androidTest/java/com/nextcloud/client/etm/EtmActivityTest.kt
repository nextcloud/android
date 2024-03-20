/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.etm

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import com.owncloud.android.AbstractIT
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class EtmActivityTest : AbstractIT() {
    private lateinit var scenario: ActivityScenario<EtmActivity>
    val intent = Intent(ApplicationProvider.getApplicationContext(), EtmActivity::class.java)

    @get:Rule
    val activityRule = ActivityScenarioRule<EtmActivity>(intent)

    @After
    fun cleanup() {
        scenario.close()
    }

    @Test
    @ScreenshotTest
    fun overview() {
        scenario = activityRule.scenario
        scenario.onActivity { sut ->
            onIdleSync {
                screenshot(sut)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun accounts() {
        scenario = activityRule.scenario
        scenario.onActivity { sut ->
            onIdleSync {
                UiThreadStatement.runOnUiThread { sut.vm.onPageSelected(1) }
                screenshot(sut)
            }
        }
    }
}
