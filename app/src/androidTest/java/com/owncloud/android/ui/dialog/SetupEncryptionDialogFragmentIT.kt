/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.dialog

import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.ui.dialog.setupEncryption.SetupEncryptionDialogFragment
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Rule
import org.junit.Test

class SetupEncryptionDialogFragmentIT : AbstractIT() {
    @get:Rule
    val testActivityRule = IntentsTestRule(TestActivity::class.java, true, false)

    @Test
    @ScreenshotTest
    fun showMnemonic() {
        val activity = testActivityRule.launchActivity(null)

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

        waitForIdleSync()

        screenshot(sut.requireDialog().window!!.decorView)
    }

    @Test
    @ScreenshotTest
    fun error() {
        val activity = testActivityRule.launchActivity(null)

        val sut = SetupEncryptionDialogFragment.newInstance(user, 0)

        sut.show(activity.supportFragmentManager, "1")

        shortSleep()

        runOnUiThread {
            sut.errorSavingKeys()
        }

        shortSleep()
        waitForIdleSync()

        screenshot(sut.requireDialog().window!!.decorView)
    }
}
