/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.providers

import androidx.annotation.UiThread
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractOnServerIT
import org.junit.Test

class UsersAndGroupsSearchProviderIT : AbstractOnServerIT() {
    @Test
    @UiThread
    fun searchUser() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { sut ->
                onIdleSync {
                    onView(isRoot()).check(matches(isDisplayed()))
                }
            }
        }
    }
}
