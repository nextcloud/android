/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.filter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ionos.scanbot.repository.RepositoryFacade
import com.ionos.scanbot.screens.filter.FilterScreen.State
import com.ionos.scanbot.tracking.ScanbotFilterScreenEventTracker
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

internal class FilterViewModelFactory @AssistedInject constructor(
	@Assisted private val initialState: State,
	private val repositoryFacade: RepositoryFacade,
	private val eventTracker: ScanbotFilterScreenEventTracker,
) : ViewModelProvider.Factory {

	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		return create() as T
	}

	private fun create(): FilterViewModel = FilterViewModel(
		initialState,
		repositoryFacade,
		eventTracker,
	)

	@AssistedFactory
	interface Assistant {
		fun create(initialState: State): FilterViewModelFactory
	}
}