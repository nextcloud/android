/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.camera.use_case.import_pictures

import android.net.Uri

internal sealed interface ImportPicturesState {
	val picturesUris: List<Uri>
	val processedItems: List<String>

	data class Processing(
		override val picturesUris: List<Uri>,
		override val processedItems: List<String> = emptyList(),
	) : ImportPicturesState

	data class Finished(
		override val picturesUris: List<Uri>,
		override val processedItems: List<String>,
	) : ImportPicturesState

	data class Canceled(
		override val picturesUris: List<Uri>,
		override val processedItems: List<String>,
	) : ImportPicturesState

	class Error(
		override val picturesUris: List<Uri>,
		override val processedItems: List<String>,
		val error: Throwable,
	) : ImportPicturesState
}
