/*
 * Nextcloud Android client application
 *
 *  @author Álvaro Brey
 *  Copyright (C) 2023 Álvaro Brey
 *  Copyright (C) 2023 Nextcloud GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.nextcloud.test

import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import dagger.android.AndroidInjector
import io.mockk.every
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class InjectionTestActivityTest {

    @get:Rule
    val injectionOverrideRule =
        InjectionOverrideRule(
            mapOf(
                InjectionTestActivity::class.java to AndroidInjector<InjectionTestActivity> { activity ->
                    val appPreferencesMock = mockk<AppPreferences>()
                    every { appPreferencesMock.lastUploadPath } returns INJECTED_STRING
                    activity.appPreferences = appPreferencesMock
                }
            )
        )

    @Test
    fun testInjectionOverride() {
        launchActivity<InjectionTestActivity>().use { _ ->
            onView(withId(R.id.text)).check(matches(withText(INJECTED_STRING)))
        }
    }

    companion object {
        private const val INJECTED_STRING = "injected string"
    }
}
