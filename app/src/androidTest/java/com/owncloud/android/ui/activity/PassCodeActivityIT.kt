/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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
