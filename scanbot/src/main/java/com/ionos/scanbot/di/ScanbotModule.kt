/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.di

import android.content.Context
import com.ionos.scanbot.BuildConfig
import com.ionos.scanbot.availability.Availability
import com.ionos.scanbot.availability.ScanbotFeatureAvailability
import com.ionos.scanbot.availability.ScanbotLicenseAvailability
import com.ionos.scanbot.controller.ScanbotController
import com.ionos.scanbot.controller.ScanbotControllerImpl
import com.ionos.scanbot.di.qualifiers.Scanbot
import com.ionos.scanbot.di.qualifiers.ScanbotLicense
import com.ionos.scanbot.di.qualifiers.ScanbotLicenseKey
import com.ionos.scanbot.di.qualifiers.ScanbotLicenseKeyUrl
import com.ionos.scanbot.initializer.ScanbotInitializer
import com.ionos.scanbot.initializer.ScanbotInitializerImpl
import com.ionos.scanbot.license.KeyStore
import com.ionos.scanbot.license.PreferencesKeyStore
import com.ionos.scanbot.license.oath.security.algorithm.AesGcmEncryptionAlgorithm
import com.ionos.scanbot.license.oath.security.algorithm.AlgorithmParameterSpecFactoryImpl
import com.ionos.scanbot.license.oath.security.key.AesGcmKeyRepository
import com.ionos.scanbot.license.oath.settings.SecureEncryptor
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(includes = [ScanbotTracking::class])
abstract class ScanbotModule {

    companion object {
        @Singleton
        @Provides
        fun provideSecureEncryptor(context: Context): SecureEncryptor {
            return SecureEncryptor(
                AesGcmEncryptionAlgorithm(
                    AesGcmKeyRepository(context).getKey(),
                    AlgorithmParameterSpecFactoryImpl()
                )
            )
        }

        @Provides
        @ScanbotLicenseKey
        fun provideScanbotLicenseKey(): String {
            return BuildConfig.SCANBOT_LICENSE_KEY
        }

        @Provides
        @ScanbotLicenseKeyUrl
        fun provideScanbotLicenseKeyUrl(): String {
            return BuildConfig.SCANBOT_LICENSE_KEY_URL
        }
    }

    @Binds
    @Scanbot
    abstract fun bindScanbotFeatureAvailability(availability: ScanbotFeatureAvailability): Availability

    @Binds
    @ScanbotLicense
    abstract fun bindLicenseAvailability(availability: ScanbotLicenseAvailability): Availability

    @Binds
    abstract fun bindKeyStore(keyStore: PreferencesKeyStore): KeyStore

    @Binds
    abstract fun bindInitializer(initializer: ScanbotInitializerImpl): ScanbotInitializer

    @Binds
    abstract fun bindScanbotController(controller: ScanbotControllerImpl): ScanbotController

}