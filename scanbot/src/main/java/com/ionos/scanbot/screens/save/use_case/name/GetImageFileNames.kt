/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.save.use_case.name

import com.ionos.scanbot.repository.RepositoryFacade
import com.ionos.scanbot.entity.ImageFormat
import javax.inject.Inject

internal class GetImageFileNames @Inject constructor(
	val repositoryFacade: RepositoryFacade,
) {

	operator fun invoke(baseFileName: String, imageFormat: ImageFormat, count: Int): List<String> {
		return List(count) { index ->
			val extension = imageFormat.extension
			val normalizedBaseFileName = normalizeBaseFileName(baseFileName, extension)
			if (count > 1) {
				"$normalizedBaseFileName${index + 1}.$extension"
			} else {
				"$normalizedBaseFileName.$extension"
			}
		}
	}

	private fun normalizeBaseFileName(baseFileName: String, extension: String): String = when {
		baseFileName.endsWith(".$extension") -> removeExtension(baseFileName, extension)
		else -> baseFileName
	}

	private fun removeExtension(baseFileName: String, extension: String): String {
		return baseFileName.substring(0, baseFileName.lastIndexOf(".$extension"))
	}
}
