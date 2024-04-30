/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
