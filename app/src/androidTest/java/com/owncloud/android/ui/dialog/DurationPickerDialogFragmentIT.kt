/*
 * Nextcloud Android client application
 *
 * @author Piotr Bator
 * Copyright (C) 2022 Piotr Bator
 * Copyright (C) 2022 Nextcloud GmbH
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
package com.owncloud.android.ui.dialog

import androidx.test.espresso.intent.rule.IntentsTestRule
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.TimeUnit.DAYS
import java.util.concurrent.TimeUnit.HOURS
import java.util.concurrent.TimeUnit.MINUTES

class DurationPickerDialogFragmentIT : AbstractIT() {

    @get:Rule
    val testActivityRule = IntentsTestRule(TestActivity::class.java, true, false)

    @Test
    fun showSyncDelayDurationDialog() {
        val initialDuration = DAYS.toMillis(2) + HOURS.toMillis(8) + MINUTES.toMillis(15)
        val activity = testActivityRule.launchActivity(null)

        val fm = activity.supportFragmentManager
        val ft = fm.beginTransaction()
        ft.addToBackStack(null)

        val dialog = DurationPickerDialogFragment.newInstance(
            initialDuration,
            "Dialog title",
            "Hint message"
        )
        dialog.show(ft, "DURATION_DIALOG")

        waitForIdleSync()
        screenshot(dialog.requireDialog().window!!.decorView)
    }
}
