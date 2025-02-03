/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.crop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ionos.scanbot.provider.ContourDetectorProvider
import com.ionos.scanbot.repository.RepositoryFacade
import com.ionos.scanbot.screens.common.use_case.SelectContour
import com.ionos.scanbot.tracking.ScanbotCropScreenEventTracker
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class CropViewModelFactory @AssistedInject constructor(
	@Assisted private val pictureId: String,
	private val selectContour: SelectContour,
	private val contourDetectorProvider: ContourDetectorProvider,
	private val repositoryFacade: RepositoryFacade,
	private val eventTracker: ScanbotCropScreenEventTracker,
) : ViewModelProvider.Factory {

	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		return create() as T
	}

	private fun create() = CropViewModel(
		pictureId,
		selectContour,
		contourDetectorProvider,
		repositoryFacade,
		eventTracker,
	)

	@AssistedFactory
	interface Assistant {
		fun create(pictureId: String): CropViewModelFactory
	}
}
