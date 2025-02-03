/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.camera.use_case

import android.graphics.Bitmap
import com.ionos.scanbot.provider.ContourDetectorProvider
import com.ionos.scanbot.repository.RepositoryFacade
import com.ionos.scanbot.screens.common.use_case.SelectContour
import javax.inject.Inject

internal class AddPictureToRepository @Inject constructor(
	private val selectContour: SelectContour,
	private val contourDetectorProvider: ContourDetectorProvider,
	private val repositoryFacade: RepositoryFacade,
) {

	operator fun invoke(picture: Bitmap): String {
		val contourDetector = contourDetectorProvider.get()
		val selectedContour = selectContour(contourDetector, picture)
		return repositoryFacade.create(picture, selectedContour)
	}
}
