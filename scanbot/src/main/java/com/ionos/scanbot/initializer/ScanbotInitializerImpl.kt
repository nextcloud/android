/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.initializer

import com.ionos.scanbot.di.qualifiers.ScanbotLicenseKey
import com.ionos.scanbot.license.LicenseKeyStore
import com.ionos.scanbot.license.LoadScanbotLicense
import com.ionos.scanbot.provider.SdkProvider
import com.ionos.scanbot.util.logger.Logger
import io.scanbot.sdk.ScanbotSDKInitializer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User: Dima Muravyov
 * Date: 07.02.2020
 */
@Singleton
class ScanbotInitializerImpl @Inject internal constructor(
    private val licenseKeyStore: LicenseKeyStore,
    private val sdkProvider: SdkProvider,
    private val tryToInitScanbotSdk: TryToInitScanbotSdk,
    private val loadScanbotLicense: LoadScanbotLicense,
    private val loggerImpl: Logger,
	@ScanbotLicenseKey private val defaultLicenseKey: String,
) : ScanbotInitializer {

    companion object{
        var logger: Logger? = null
            private set
    }

	override fun initialize() {
        logger = loggerImpl

        tryToInitScanbotSdk(defaultLicenseKey)

        if (isSdkInitRequired()) {
			licenseKeyStore.getLicenseKey()
                ?.let {  tryToInitScanbotSdk(it) }

			if (isSdkInitRequired()) {
                loadScanbotLicense()
			}
		}
	}

	override fun isInitialized(): Boolean = ScanbotSDKInitializer.isInitialized()

	override fun isLicenseValid(): Boolean = sdkProvider.get().licenseInfo.isValid

	private fun isSdkInitRequired() = !isInitialized() || !isLicenseValid()

}