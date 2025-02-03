/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ionos.scanbot.filter.color.ColorFilterType
import com.ionos.scanbot.provider.DocumentNameProvider
import com.ionos.scanbot.repository.PictureRepository
import com.ionos.scanbot.repository.RepositoryFacade
import com.ionos.scanbot.screens.gallery.use_case.GetColorFilterIcon
import com.ionos.scanbot.tracking.ScanbotGalleryScreenEventTracker
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class GalleryViewModelFactory @AssistedInject constructor(
	@Assisted private val initialPictureId: String?,
	private val documentNameProvider: DocumentNameProvider,
	private val getColorFilterIcon: GetColorFilterIcon,
	private val repositoryFacade: RepositoryFacade,
	private val pictureRepository: PictureRepository,
	private val eventTracker: ScanbotGalleryScreenEventTracker,
) : ViewModelProvider.Factory {

	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		return create() as T
	}

	private fun create() = GalleryViewModel(
		createInitialState(),
		initialPictureId,
		getColorFilterIcon,
		repositoryFacade,
		pictureRepository,
		eventTracker,
	)

	private fun createInitialState() = GalleryScreen.State(
		title = documentNameProvider.getName(),
		filterIcon = getColorFilterIcon(ColorFilterType.None()),
	)

	@AssistedFactory
	interface Assistant {
		fun create(initialPictureId: String?): GalleryViewModelFactory
	}
}
