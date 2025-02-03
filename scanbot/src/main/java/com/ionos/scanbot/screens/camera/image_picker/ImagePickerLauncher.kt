/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.camera.image_picker

import android.net.Uri
import androidx.activity.ComponentActivity

private const val MAX_PICTURE_COUNT = 10

private val mimeTypes = arrayOf(
	"image/bmp",
	"image/gif",
	"image/jpeg",
	"image/png",
	"image/webp",
	"image/x-ms-bmp",
)

internal class ImagePickerLauncher(
	activity: ComponentActivity,
	private val onPicturesUrisReceived: (List<Uri>) -> Unit,
) {
	private val activityResultLauncher = activity.registerForActivityResult(
		ImagePickerContract(MAX_PICTURE_COUNT),
		this::onActivityResult,
	)

	fun launch() {
		activityResultLauncher.launch(mimeTypes)
	}

	private fun onActivityResult(uris: List<Uri>) {
		if (uris.isNotEmpty()) {
			onPicturesUrisReceived(uris)
		}
	}
}
