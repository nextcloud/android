/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import com.owncloud.android.AbstractIT
import com.owncloud.android.utils.ScreenshotTest
import org.junit.Test

class PassCodeActivityIT : AbstractIT() {
    @Test
    @ScreenshotTest
    fun check() {
        val intent = Intent(
            targetContext,
            PassCodeActivity::class.java
        ).apply {
            action = PassCodeActivity.ACTION_CHECK
        }

        val sutScenario = ActivityScenario.launch<PassCodeActivity>(intent)
        sutScenario.onActivity { sut ->
            onIdleSync {
                sut.runOnUiThread { sut.binding.txt0.clearFocus() }
                Espresso.closeSoftKeyboard()
                shortSleep()
                onIdleSync {
                    screenshot(sut)
                }
            }
        }
    }

    @Test
    @ScreenshotTest
    fun request() {
        val intent = Intent(
            targetContext,
            PassCodeActivity::class.java
        ).apply {
            action = PassCodeActivity.ACTION_REQUEST_WITH_RESULT
        }

        val sutScenario = ActivityScenario.launch<PassCodeActivity>(intent)
        sutScenario.onActivity { sut ->
            onIdleSync {
                sut.runOnUiThread { sut.binding.txt0.clearFocus() }
                Espresso.closeSoftKeyboard()
                shortSleep()

                onIdleSync {
                    screenshot(sut)
                }
            }
        }
    }

    @Test
    @ScreenshotTest
    fun delete() {
        val intent = Intent(
            targetContext,
            PassCodeActivity::class.java
        ).apply {
            action = PassCodeActivity.ACTION_CHECK_WITH_RESULT
        }

        val sutScenario = ActivityScenario.launch<PassCodeActivity>(intent)
        sutScenario.onActivity { sut ->
            onIdleSync {
                sut.runOnUiThread { sut.binding.txt0.clearFocus() }
                Espresso.closeSoftKeyboard()
                shortSleep()
                onIdleSync {
                    screenshot(sut)
                }
            }
        }
    }
}
