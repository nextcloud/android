/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2024 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.onboarding

import android.content.res.Resources
import com.nextcloud.client.account.AnonymousUser
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.account.User
import com.nextcloud.client.preferences.AppPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

class OnboardingServiceTest {

    @Mock
    private lateinit var resources: Resources

    @Mock
    private lateinit var preferences: AppPreferences

    @Mock
    private lateinit var currentAccountProvider: CurrentAccountProvider

    @Mock
    private lateinit var user: User

    private lateinit var onboardingService: OnboardingServiceImpl

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        onboardingService = OnboardingServiceImpl(resources, preferences, currentAccountProvider)
    }

    @Test
    fun `first run flag toggles with current current account`() {
        // GIVEN
        //      current account is anonymous
        whenever(currentAccountProvider.user).thenReturn(AnonymousUser("dummy"))

        // THEN
        //      first run flag is true
        assertTrue(onboardingService.isFirstRun)

        // WHEN
        //      current account is set
        whenever(currentAccountProvider.user).thenReturn(user)

        // THEN
        //      first run flag toggles
        assertFalse(onboardingService.isFirstRun)
    }
}
