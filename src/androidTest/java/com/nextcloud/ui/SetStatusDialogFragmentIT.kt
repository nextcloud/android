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
import com.owncloud.android.ui.activity.FileDisplayActivity
import org.junit.Rule
import org.junit.Test

class SetStatusDialogFragmentIT : AbstractIT() {
    @get:Rule
    var activityRule = IntentsTestRule(FileDisplayActivity::class.java, true, false)

    @Test
    fun open() {
        val sut = SetStatusDialogFragment.newInstance(user)
        val activity = activityRule.launchActivity(null)

        sut.show(activity.supportFragmentManager, "")

        longSleep()
    }
}
