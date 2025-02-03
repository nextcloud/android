/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.save.use_case.save

import android.net.Uri
import com.ionos.scanbot.screens.save.SaveScreen.FileType
import com.ionos.scanbot.entity.ImageFormat
import io.reactivex.Single
import javax.inject.Inject

internal class SaveDocument @Inject constructor(
	private val savePdf: SavePdf,
	private val saveImages: SaveImages,
) {

	operator fun invoke(baseFileName: String, fileType: FileType): Single<List<Uri>> = when (fileType) {
		FileType.PDF_OCR -> savePdf(baseFileName, withOcr = true)
		FileType.PDF -> savePdf(baseFileName, withOcr = false)
		FileType.JPG -> saveImages(baseFileName, ImageFormat.JPEG)
		FileType.PNG -> saveImages(baseFileName, ImageFormat.PNG)
	}
}
