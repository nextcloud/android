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
package com.owncloud.android.providers

import androidx.test.espresso.intent.rule.IntentsTestRule
import com.nextcloud.client.TestActivity
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
