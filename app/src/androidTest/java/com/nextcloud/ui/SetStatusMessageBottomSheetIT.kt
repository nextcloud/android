/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.ui

import androidx.annotation.UiThread
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.owncloud.android.AbstractIT
import com.owncloud.android.lib.resources.users.ClearAt
import com.owncloud.android.lib.resources.users.PredefinedStatus
import com.owncloud.android.lib.resources.users.Status
import com.owncloud.android.lib.resources.users.StatusType
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.utils.EspressoIdlingResource
import org.junit.After
import org.junit.Before
import org.junit.Test

class SetStatusMessageBottomSheetIT : AbstractIT() {
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
    }

    @Test
    @UiThread
    fun open() {
        launchActivity<FileDisplayActivity>().use { scenario ->
            scenario.onActivity { activity ->
                onIdleSync {
                    EspressoIdlingResource.increment()
                    val sut = SetStatusMessageBottomSheet(
                        user,
                        Status(StatusType.DND, "Working hard…", "🤖", -1)
                    )
                    sut.show(activity.supportFragmentManager, "")
                    val predefinedStatus: ArrayList<PredefinedStatus> = arrayListOf(
                        PredefinedStatus("meeting", "📅", "In a meeting", ClearAt("period", "3600")),
                        PredefinedStatus("commuting", "🚌", "Commuting", ClearAt("period", "1800")),
                        PredefinedStatus("be-right-back", "⏳", "Be right back", ClearAt("period", "900")),
                        PredefinedStatus("remote-work", "🏡", "Working remotely", ClearAt("end-of", "day")),
                        PredefinedStatus("sick-leave", "🤒", "Out sick", ClearAt("end-of", "day")),
                        PredefinedStatus("vacationing", "🌴", "Vacationing", null)
                    )
                    sut.setPredefinedStatus(predefinedStatus)
                    EspressoIdlingResource.decrement()

                    onView(isRoot()).check(matches(isDisplayed()))
                }
            }
        }
    }
}
