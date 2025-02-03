/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.save.use_case.name

import com.ionos.scanbot.screens.save.SaveScreen.FileType
import com.ionos.scanbot.entity.ImageFormat
import javax.inject.Inject

internal class GetFileNames @Inject constructor(
	private val getPdfFileName: GetPdfFileName,
	private val getImageFileNames: GetImageFileNames,
) {

	operator fun invoke(baseFileName: String, fileType: FileType, count: Int): List<String> {
		return when (fileType) {
			FileType.PDF_OCR -> listOf(getPdfFileName(baseFileName))
			FileType.PDF -> listOf(getPdfFileName(baseFileName))
			FileType.JPG -> getImageFileNames(baseFileName, ImageFormat.JPEG, count)
			FileType.PNG -> getImageFileNames(baseFileName, ImageFormat.PNG, count)
		}
	}
}
