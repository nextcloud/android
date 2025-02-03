/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.availability

import com.ionos.scanbot.initializer.ScanbotInitializer
import javax.inject.Inject

/**
 * User: Alex Kucherenko
 * Date: 13.06.2019
 */
class ScanbotLicenseAvailability @Inject internal constructor(
	private val initializer: ScanbotInitializer,
) : Availability {

	override fun available(): Boolean = initializer.isInitialized() && initializer.isLicenseValid()
}