/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.ui

import android.Manifest
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.rule.GrantPermissionRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.lib.resources.users.Status
import com.owncloud.android.lib.resources.users.StatusType
import com.owncloud.android.ui.activity.FileDisplayActivity
import org.junit.Rule
import org.junit.Test

class SetOnlineStatusBottomSheetIT : AbstractIT() {

    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.POST_NOTIFICATIONS
    )

    @Test
    fun open() {
        launchActivity<FileDisplayActivity>().use { scenario ->
            onView(isRoot()).check(matches(isDisplayed()))

            onView(withId(R.id.clearStatusAfterSpinner))
                .check(matches(isDisplayed()))

            scenario.onActivity { activity ->
                val sut = SetOnlineStatusBottomSheet(
                    Status(StatusType.DND, "Working hard…", "🤖", -1)
                )
                sut.show(activity.supportFragmentManager, "")
            }
        }
    }
}
