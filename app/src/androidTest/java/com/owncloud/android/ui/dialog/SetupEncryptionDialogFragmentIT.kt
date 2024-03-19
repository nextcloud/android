/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.dialog

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.utils.ScreenshotTest
import org.junit.After
import org.junit.Rule
import org.junit.Test

class SetupEncryptionDialogFragmentIT : AbstractIT() {
    private lateinit var scenario: ActivityScenario<TestActivity>
    val intent = Intent(ApplicationProvider.getApplicationContext(), TestActivity::class.java)

    @get:Rule
    val activityRule = ActivityScenarioRule<TestActivity>(intent)

    @After
    fun cleanup() {
        scenario.close()
    }

    @Test
    @ScreenshotTest
    fun showMnemonic() {
        scenario = activityRule.scenario
        scenario.onActivity { activity ->
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

            shortSleep()

            runOnUiThread {
                sut.setMnemonic(keyWords)
                sut.showMnemonicInfo()
            }

            onIdleSync {
                screenshot(sut.requireDialog().window!!.decorView)
            }
        }
    }

    @Test
    @ScreenshotTest
    fun error() {
        scenario = activityRule.scenario
        scenario.onActivity { activity ->
            val sut = SetupEncryptionDialogFragment.newInstance(user, 0)

            sut.show(activity.supportFragmentManager, "1")

            shortSleep()

            runOnUiThread {
                sut.errorSavingKeys()
            }

            shortSleep()
            onIdleSync {
                screenshot(sut.requireDialog().window!!.decorView)
            }
        }
    }
}
