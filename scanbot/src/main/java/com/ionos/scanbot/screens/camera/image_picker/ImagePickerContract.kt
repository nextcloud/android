/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.camera.image_picker

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia

class ImagePickerContract(private val maxItems: Int) : ActivityResultContract<Array<String>, List<Uri>>() {
	private val pickMultipleVisualMedia = PickMultipleVisualMedia(maxItems)

	override fun createIntent(context: Context, mimeTypes: Array<String>): Intent {
		val request = PickVisualMediaRequest(PickVisualMedia.ImageOnly)
		return pickMultipleVisualMedia.createIntent(context, request).apply {
			putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
			if (action == PickVisualMedia.ACTION_SYSTEM_FALLBACK_PICK_IMAGES) {
				putExtra(PickVisualMedia.EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_MAX, maxItems)
			}
		}
	}

	override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
		return pickMultipleVisualMedia.parseResult(resultCode, intent)
	}
}
