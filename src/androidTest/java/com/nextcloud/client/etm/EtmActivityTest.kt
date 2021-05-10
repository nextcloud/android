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
    fun preferences() {
        val sut: EtmActivity = activityRule.launchActivity(null)
        val preferences = sut.getSharedPreferences()

        preferences.edit().apply {
            clear()
            putString("TEST", "ONLY_DUMMY_VALUES")
            putBoolean("autoUploadEntriesSplitOut", true)
            putString("darkMode", "SYSTEM")
            putString("lock", "none")
            commit()
        }

        UiThreadStatement.runOnUiThread { sut.vm.onPageSelected(0) }

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
