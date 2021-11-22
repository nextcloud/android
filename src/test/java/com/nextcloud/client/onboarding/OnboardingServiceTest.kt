/* Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.onboarding

import android.accounts.Account
import android.content.res.Resources
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.preferences.AppPreferences
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class OnboardingServiceTest {

    @Mock
    private lateinit var resources: Resources

    @Mock
    private lateinit var preferences: AppPreferences

    @Mock
    private lateinit var currentAccountProvider: CurrentAccountProvider

    @Mock
    private lateinit var account: Account

    private lateinit var onboardingService: OnboardingServiceImpl

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        onboardingService = OnboardingServiceImpl(resources, preferences, currentAccountProvider)
    }

    @Test
    fun `first run flag toggles with current current account`() {
        // GIVEN
        //      current account is not set
        //      first run flag is true
        assertTrue(onboardingService.isFirstRun)

        // WHEN
        //      current account is set
        whenever(currentAccountProvider.currentAccount).thenReturn(account)

        // THEN
        //      first run flag toggles
        assertFalse(onboardingService.isFirstRun)
    }
}
