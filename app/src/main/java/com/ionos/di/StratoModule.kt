/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.di

import com.ionos.analycis.AnalyticsManager
import com.ionos.privacy.DataProtectionActivity
import com.ionos.analycis.FirebaseAnalyticsManager
import com.ionos.privacy.PrivacySettingsActivity
import com.ionos.scanbot.di.NCScanbotModule
import dagger.Binds
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module(includes = [NCScanbotModule::class])
abstract class StratoModule {

    @ContributesAndroidInjector
    abstract fun dataProtectionActivity(): DataProtectionActivity

    @ContributesAndroidInjector
    abstract fun privacySettingsActivity(): PrivacySettingsActivity

    @Binds
    abstract fun analyticsManager(firebaseAnalyticsManager: FirebaseAnalyticsManager): AnalyticsManager
}
