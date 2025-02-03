/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.crop

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import com.ionos.scanbot.entity.Picture
import com.ionos.scanbot.entity.SelectedContour
import com.ionos.scanbot.exception.PictureNotFoundException
import com.ionos.scanbot.exception.ReadPictureBitmapException
import com.ionos.scanbot.filter.FilterType
import com.ionos.scanbot.provider.ContourDetectorProvider
import com.ionos.scanbot.repository.RepositoryFacade
import com.ionos.scanbot.screens.base.BaseViewModel
import com.ionos.scanbot.screens.common.use_case.SelectContour
import com.ionos.scanbot.screens.crop.CropScreen.ButtonType
import com.ionos.scanbot.screens.crop.CropScreen.Event
import com.ionos.scanbot.screens.crop.CropScreen.Event.CloseScreenEvent
import com.ionos.scanbot.screens.crop.CropScreen.Event.DisplayPictureEvent
import com.ionos.scanbot.screens.crop.CropScreen.Event.DisplayPolygonEvent
import com.ionos.scanbot.screens.crop.CropScreen.Event.ShowErrorMessageEvent
import com.ionos.scanbot.screens.crop.CropScreen.State
import com.ionos.scanbot.screens.crop.CropScreen.ViewModel
import com.ionos.scanbot.tracking.ScanbotCropScreenEventTracker
import com.ionos.scanbot.util.graphics.mapPoints
import com.ionos.scanbot.util.rx.plusAssign
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

internal class CropViewModel(
	private val pictureId: String,
	private val selectContour: SelectContour,
	private val contourDetectorProvider: ContourDetectorProvider,
	private val repositoryFacade: RepositoryFacade,
	private val eventTracker: ScanbotCropScreenEventTracker,
) : BaseViewModel<Event, State>(State(), eventTracker),
	ViewModel {

	private val contourDetector by lazy { contourDetectorProvider.get() }

	init {
		subscriptions += getPicture()
			.map { createInitialState(it) }
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(::updateState, ::onError)
	}

	private fun createInitialState(picture: Picture): State {
		val contour = picture.original.getCropFilter().contour
		val rotation = picture.original.getRotateFilter().degrees
		val polygon = getRotatedPolygon(contour, rotation)
		val isDefaultContour = contour == SelectedContour.DEFAULT
		val buttonType = if (isDefaultContour) ButtonType.AUTODETECT else ButtonType.RESET
		return State(buttonType = buttonType, event = DisplayPolygonEvent(polygon))
	}

	override fun onEventHandled() {
		updateState { copy(event = null) }
	}

	override fun onStart() {
		updateState { copy(processing = true) }

		subscriptions += getOriginalBitmap(setOf(FilterType.COLOR, FilterType.ROTATE))
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(::onPictureLoaded, ::onError)
	}

	private fun onPictureLoaded(bitmap: Bitmap) {
		updateState { copy(processing = false, event = DisplayPictureEvent(bitmap)) }
	}

	override fun onBackPressed() {
		eventTracker.trackBackPressed()
		updateState { copy(event = CloseScreenEvent) }
	}

	override fun onAutoDetectContourClicked() {
		eventTracker.trackDetectDocumentClicked()

		updateState { copy(processing = true) }

		subscriptions += getOriginalBitmap()
			.map { selectContour(contourDetector, it) }
			.zipWith(getPictureRotation(), ::getRotatedPolygon)
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(::onContourDetected, ::onError)
	}

	private fun onContourDetected(polygon: List<PointF>) = updateState {
		copy(processing = false, buttonType = ButtonType.RESET, event = DisplayPolygonEvent(polygon))
	}

	override fun onResetContourClicked() {
		eventTracker.trackResetBordersClicked()

		updateState { copy(processing = true) }

		subscriptions += getPictureRotation()
			.map { getRotatedPolygon(SelectedContour.DEFAULT, it) }
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(::onContourReset, ::onError)
	}

	private fun onContourReset(polygon: List<PointF>) = updateState {
		copy(processing = false, buttonType = ButtonType.AUTODETECT, event = DisplayPolygonEvent(polygon))
	}

	override fun onSaveClicked(polygon: List<PointF>) {
		eventTracker.trackSaveClicked()

		updateState { copy(processing = true) }

		subscriptions += updatePicture(polygon)
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(::onSaved, ::onError)
	}

	private fun onSaved() {
		updateState { copy(processing = false, event = CloseScreenEvent) }
	}

	private fun onError(error: Throwable) {
		updateState { copy(processing = false, event = ShowErrorMessageEvent(error)) }
	}

	private fun getPictureRotation(): Single<Float> = getPicture()
		.map { it.original.getRotateFilter().degrees }

	private fun getPicture(): Single<Picture> = Single.fromCallable {
		repositoryFacade.read(pictureId) ?: throw PictureNotFoundException(pictureId)
	}

	private fun getOriginalBitmap(): Single<Bitmap> = Single.fromCallable {
		repositoryFacade.readOriginalBitmap(pictureId) ?: throw ReadPictureBitmapException(pictureId)
	}

	private fun getOriginalBitmap(filterTypes: Set<FilterType>): Single<Bitmap> = Single.fromCallable {
		val bitmap = repositoryFacade.readOriginalBitmapWithFilters(pictureId, filterTypes)
		bitmap ?: throw ReadPictureBitmapException(pictureId)
	}

	private fun updatePicture(polygon: List<PointF>): Completable = Completable.fromCallable {
		val picture = repositoryFacade.read(pictureId) ?: throw PictureNotFoundException(pictureId)
		val rotation = picture.original.getRotateFilter().degrees
		val normalizedPolygon = polygon.rotate(-rotation)
		val contour = SelectedContour(normalizedPolygon)
		repositoryFacade.update(picture.makeCopy(contour))
	}

	private fun getRotatedPolygon(contour: SelectedContour, rotation: Float): List<PointF> {
		return contour.normalizedPolygon.rotate(rotation)
	}

	private fun List<PointF>.rotate(rotation: Float): List<PointF> = with(Matrix()) {
		setRotate(rotation, 0.5F, 0.5F)
		mapPoints(this@rotate)
	}
}