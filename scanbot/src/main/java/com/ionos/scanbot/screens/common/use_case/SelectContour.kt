/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.common.use_case

import android.graphics.Bitmap
import com.ionos.scanbot.entity.SelectedContour
import io.scanbot.sdk.core.contourdetector.ContourDetector
import io.scanbot.sdk.core.contourdetector.DetectionResult
import io.scanbot.sdk.core.contourdetector.DocumentDetectionStatus.*
import javax.inject.Inject

internal class SelectContour @Inject constructor() {

	operator fun invoke(detector: ContourDetector, image: Bitmap): SelectedContour {
		return detector.detect(image)?.toSelectedContour() ?: SelectedContour.DEFAULT
	}

	private fun DetectionResult.toSelectedContour(): SelectedContour? = when (status) {
		OK, OK_BUT_BAD_ANGLES, OK_BUT_TOO_SMALL, OK_BUT_BAD_ASPECT_RATIO -> SelectedContour(polygonF)
		else -> null
	}
}
