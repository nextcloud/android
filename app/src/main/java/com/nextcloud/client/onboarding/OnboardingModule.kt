/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.onboarding

import android.content.res.Resources
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.preferences.AppPreferences
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class OnboardingModule {

    @Provides
    @Singleton
    internal fun onboardingService(
        resources: Resources,
        preferences: AppPreferences,
        accountProvider: CurrentAccountProvider
    ): OnboardingService = OnboardingServiceImpl(resources, preferences, accountProvider)
}
