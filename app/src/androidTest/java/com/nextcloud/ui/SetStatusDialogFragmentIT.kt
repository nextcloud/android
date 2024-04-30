/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
