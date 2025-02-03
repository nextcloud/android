/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.provider

import io.scanbot.sdk.ScanbotSDK
import io.scanbot.sdk.core.contourdetector.ContourDetector
import javax.inject.Inject

internal class ContourDetectorProvider(private val sdk: ScanbotSDK) {
	private val contourDetector by lazy { sdk.initContourDetector() }

	@Inject
	constructor(sdkProvider: SdkProvider) : this(sdkProvider.get())

	fun get(): ContourDetector = contourDetector

	private fun ScanbotSDK.initContourDetector() = createContourDetector().apply {
		val paramsProvider = ContourDetectorParamsProvider()
		setAcceptedAngleScore(paramsProvider.acceptedAngleScore)
		setAcceptedSizeScore(paramsProvider.acceptedSizeScore)
	}
}
