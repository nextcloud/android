/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.camera.use_case

import android.content.Context
import com.ionos.scanbot.R
import io.scanbot.sdk.core.contourdetector.DocumentDetectionStatus
import io.scanbot.sdk.core.contourdetector.DocumentDetectionStatus.*

internal class GetUserGuidanceStatus(private val context: Context) {

	operator fun invoke(status: DocumentDetectionStatus): UserGuidanceStatus {
        return UserGuidanceStatus(statusText(status), status.icon())
	}

    private fun statusText(status: DocumentDetectionStatus): String? = when (status) {
		OK -> context.getString(R.string.scanbot_camera_detection_result_ok)
		OK_BUT_TOO_SMALL -> context.getString(R.string.scanbot_camera_detection_result_ok_but_too_small)
		OK_BUT_BAD_ANGLES -> context.getString(R.string.scanbot_camera_detection_result_ok_but_bad_angles)
		ERROR_NOTHING_DETECTED -> context.getString(R.string.scanbot_camera_detection_result_error_nothing_detected)
		ERROR_TOO_NOISY -> context.getString(R.string.scanbot_camera_detection_result_error_too_noisy)
		ERROR_TOO_DARK -> context.getString(R.string.scanbot_camera_detection_result_error_too_dark)
		else -> null
	}

    private fun DocumentDetectionStatus.icon() : Int? = when (this) {
        OK -> R.drawable.scanbot_ic_dont_move
        OK_BUT_TOO_SMALL -> R.drawable.scanbot_ic_zoom_in
        OK_BUT_BAD_ANGLES -> R.drawable.scanbot_ic_alert
        ERROR_NOTHING_DETECTED -> R.drawable.scanbot_ic_no_file
        ERROR_TOO_NOISY -> R.drawable.scanbot_ic_too_noisy
        ERROR_TOO_DARK -> R.drawable.scanbot_ic_poor_light
        else -> null
    }
}