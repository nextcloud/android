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

import androidx.test.espresso.intent.rule.IntentsTestRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.lib.resources.users.ClearAt
import com.owncloud.android.lib.resources.users.PredefinedStatus
import com.owncloud.android.lib.resources.users.Status
import com.owncloud.android.lib.resources.users.StatusType
import com.owncloud.android.ui.activity.FileDisplayActivity
import org.junit.Rule
import org.junit.Test

class SetStatusDialogFragmentIT : AbstractIT() {
    @get:Rule
    var activityRule = IntentsTestRule(FileDisplayActivity::class.java, true, false)

    @Test
    fun open() {
        val sut = SetStatusDialogFragment.newInstance(user, Status(StatusType.DND, "Working hard…", "🤖", -1))
        val activity = activityRule.launchActivity(null)

        sut.show(activity.supportFragmentManager, "")

        val predefinedStatus: ArrayList<PredefinedStatus> = arrayListOf(
            PredefinedStatus("meeting", "📅", "In a meeting", ClearAt("period", "3600")),
            PredefinedStatus("commuting", "🚌", "Commuting", ClearAt("period", "1800")),
            PredefinedStatus("remote-work", "🏡", "Working remotely", ClearAt("end-of", "day")),
            PredefinedStatus("sick-leave", "🤒", "Out sick", ClearAt("end-of", "day")),
            PredefinedStatus("vacationing", "🌴", "Vacationing", null)
        )

        shortSleep()

        activity.runOnUiThread { sut.setPredefinedStatus(predefinedStatus) }

        longSleep()
    }
}
