/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.filter

import android.graphics.Bitmap
import com.ionos.scanbot.exception.PictureNotFoundException
import com.ionos.scanbot.exception.ReadPictureBitmapException
import com.ionos.scanbot.filter.FilterType
import com.ionos.scanbot.filter.color.ColorFilter
import com.ionos.scanbot.filter.color.ColorFilterType
import com.ionos.scanbot.repository.RepositoryFacade
import com.ionos.scanbot.screens.base.BaseViewModel
import com.ionos.scanbot.screens.filter.FilterScreen.Event
import com.ionos.scanbot.screens.filter.FilterScreen.Event.CloseScreenEvent
import com.ionos.scanbot.screens.filter.FilterScreen.Event.ShowErrorEvent
import com.ionos.scanbot.screens.filter.FilterScreen.State
import com.ionos.scanbot.screens.filter.FilterScreen.ViewModel
import com.ionos.scanbot.tracking.ScanbotFilterScreenEventTracker
import com.ionos.scanbot.util.rx.plusAssign
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

internal class FilterViewModel(
	initialState: State,
	private val repositoryFacade: RepositoryFacade,
	private val eventTracker: ScanbotFilterScreenEventTracker,
) : BaseViewModel<Event, State>(initialState, eventTracker),
	ViewModel {

	init {
		subscriptions += loadImage(state().filterType)
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(::onSuccessfulInit, ::onLoadImageError)
	}

	private fun onSuccessfulInit(image: Bitmap) {
		updateState { copy(image = image, showApplyToAll = shouldShowApplyToAllButton()) }
	}

	override fun onEventHandled() {
		updateState { copy(event = null) }
	}

	override fun onBackPressed() {
		eventTracker.trackBackPressed()
		updateState { copy(event = CloseScreenEvent) }
	}

	override fun onSaveClicked() {
		eventTracker.trackSaveClicked()

		updateState { copy(processing = true) }

		subscriptions += applyFilter(state().pictureId, state().filterType)
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(::onFilterApplied, ::onApplyFilterError)
	}

	override fun onApplyForAllClicked() {
		eventTracker.trackApplyForAllClicked()

		updateState { copy(processing = true) }

		subscriptions += applyForAll(state().filterType)
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(::onFilterApplied, ::onApplyFilterError)
	}

	override fun onFilterTypeChanged(filterType: ColorFilterType) {
		when (filterType) {
			is ColorFilterType.MagicColor -> eventTracker.trackMagicColorFilterApplied()
			is ColorFilterType.MagicText -> eventTracker.trackMagicTextFilterApplied()
			is ColorFilterType.Color -> eventTracker.trackColorFilterApplied()
			is ColorFilterType.Grayscale -> eventTracker.trackGrayscaleFilterApplied()
			is ColorFilterType.BlackWhite -> eventTracker.trackBlackAndWhiteFilterApplied()
			is ColorFilterType.None -> eventTracker.trackFilterReset()
		}
		onFilterChanged(filterType)
	}

	override fun onBrightnessChanged(filterType: ColorFilterType) {
		if (filterType.isChanged(brightness = true)) {
			eventTracker.trackBrightnessChanged()
		} else {
			eventTracker.trackBrightnessReset()
		}
		onFilterChanged(filterType)
	}

	override fun onSharpnessChanged(filterType: ColorFilterType) {
		if (filterType.isChanged(sharpness = true)) {
			eventTracker.trackSharpnessChanged()
		} else {
			eventTracker.trackSharpnessReset()
		}
		onFilterChanged(filterType)
	}

	override fun onContrastChanged(filterType: ColorFilterType) {
		if (filterType.isChanged(contrast = true)) {
			eventTracker.trackContrastChanged()
		} else {
			eventTracker.trackContrastReset()
		}
		onFilterChanged(filterType)
	}

	private fun onFilterChanged(filterType: ColorFilterType) {
		updateState { copy(filterType = filterType) }

		subscriptions += loadImage(state().filterType)
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(::onImageLoaded, ::onLoadImageError)
	}

	private fun onImageLoaded(image: Bitmap) {
		updateState { copy(image = image) }
	}

	private fun onLoadImageError(error: Throwable) {
		updateState { copy(event = ShowErrorEvent(error)) }
	}

	private fun onFilterApplied() {
		updateState { copy(processing = false, event = CloseScreenEvent) }
	}

	private fun onApplyFilterError(error: Throwable) {
		updateState { copy(processing = false, event = ShowErrorEvent(error)) }
	}

	private fun loadImage(colorFilterType: ColorFilterType): Single<Bitmap> = Single.fromCallable {
		val pictureId = state().pictureId
		val filterTypes = setOf(FilterType.CROP, FilterType.ROTATE)

		repositoryFacade.readOriginalBitmapWithFilters(pictureId, filterTypes)
			?.let { ColorFilter(colorFilterType).apply(it) }
			?: throw ReadPictureBitmapException(pictureId)
	}

	private fun applyFilter(pictureId: String, filterType: ColorFilterType): Completable {
		return Completable.fromCallable {
			val picture = repositoryFacade.read(pictureId) ?: throw PictureNotFoundException(pictureId)
			repositoryFacade.update(picture.makeCopy(colorFilterType = filterType))
		}
	}

	private fun applyForAll(filterType: ColorFilterType): Completable = Completable.fromCallable {
		val pictures = repositoryFacade.readAll()
		pictures.forEach { picture ->
			repositoryFacade.update(picture.makeCopy(colorFilterType = filterType))
		}
	}

	private fun shouldShowApplyToAllButton(): Boolean = repositoryFacade.readAll().size > 1
}