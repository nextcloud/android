/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.providers

import androidx.test.espresso.intent.rule.IntentsTestRule
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractOnServerIT
import org.junit.Rule
import org.junit.Test

class UsersAndGroupsSearchProviderIT : AbstractOnServerIT() {
    @get:Rule
    val testActivityRule = IntentsTestRule(TestActivity::class.java, true, false)

    @Test
    fun searchUser() {
        val activity = testActivityRule.launchActivity(null)

        shortSleep()

        activity.runOnUiThread {
            // fragment.search("Admin")
        }

        longSleep()
    }
}
