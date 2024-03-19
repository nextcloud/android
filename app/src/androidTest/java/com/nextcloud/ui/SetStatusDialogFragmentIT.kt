/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
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

            shortSleep()

            activity.runOnUiThread { sut.setPredefinedStatus(predefinedStatus) }

            longSleep()
        }
    }
}
