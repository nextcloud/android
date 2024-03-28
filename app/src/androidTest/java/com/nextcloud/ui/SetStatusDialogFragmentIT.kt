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

package com.nextcloud.ui

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.lib.resources.users.ClearAt
import com.owncloud.android.lib.resources.users.PredefinedStatus
import com.owncloud.android.lib.resources.users.Status
import com.owncloud.android.lib.resources.users.StatusType
import com.owncloud.android.ui.activity.FileDisplayActivity
import org.junit.After
import org.junit.Rule
import org.junit.Test

class SetStatusDialogFragmentIT : AbstractIT() {
    private lateinit var scenario: ActivityScenario<FileDisplayActivity>
    val intent = Intent(ApplicationProvider.getApplicationContext(), FileDisplayActivity::class.java)

    @get:Rule
    val activityRule = ActivityScenarioRule<FileDisplayActivity>(intent)

    @After
    fun cleanup() {
        scenario.close()
    }

    @Test
    fun open() {
        val sut = SetStatusDialogFragment.newInstance(user, Status(StatusType.DND, "Working hard…", "🤖", -1))
        scenario = activityRule.scenario
        scenario.onActivity { activity ->
            sut.show(activity.supportFragmentManager, "")

            val predefinedStatus: ArrayList<PredefinedStatus> = arrayListOf(
                PredefinedStatus("meeting", "📅", "In a meeting", ClearAt("period", "3600")),
                PredefinedStatus("commuting", "🚌", "Commuting", ClearAt("period", "1800")),
                PredefinedStatus("remote-work", "🏡", "Working remotely", ClearAt("end-of", "day")),
                PredefinedStatus("sick-leave", "🤒", "Out sick", ClearAt("end-of", "day")),
                PredefinedStatus("vacationing", "🌴", "Vacationing", null)
            )

            onIdleSync {
                shortSleep()

                activity.runOnUiThread { sut.setPredefinedStatus(predefinedStatus) }

                longSleep()
            }
        }
    }
}
