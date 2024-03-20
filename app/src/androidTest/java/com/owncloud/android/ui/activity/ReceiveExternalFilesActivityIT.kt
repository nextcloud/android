/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class ReceiveExternalFilesActivityIT : AbstractIT() {
    private lateinit var scenario: ActivityScenario<ReceiveExternalFilesActivity>
    val intent = Intent(ApplicationProvider.getApplicationContext(), ReceiveExternalFilesActivity::class.java)

    @get:Rule
    val activityRule = ActivityScenarioRule<ReceiveExternalFilesActivity>(intent)

    @After
    fun cleanup() {
        scenario.close()
    }

    @Test
    @ScreenshotTest
    fun open() {
        scenario = activityRule.scenario
        scenario.onActivity { sut ->
            onIdleSync {
                screenshot(sut)
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
