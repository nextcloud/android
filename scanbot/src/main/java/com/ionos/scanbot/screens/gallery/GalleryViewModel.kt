/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.gallery

import com.ionos.scanbot.entity.Picture
import com.ionos.scanbot.filter.color.ColorFilterType
import com.ionos.scanbot.repository.PictureRepository
import com.ionos.scanbot.repository.RepositoryFacade
import com.ionos.scanbot.screens.base.BaseViewModel
import com.ionos.scanbot.screens.common.use_case.open_screen.OpenScreenIntent.OpenCameraScreenIntent
import com.ionos.scanbot.screens.common.use_case.open_screen.OpenScreenIntent.OpenCropScreenIntent
import com.ionos.scanbot.screens.common.use_case.open_screen.OpenScreenIntent.OpenFilterScreenIntent
import com.ionos.scanbot.screens.common.use_case.open_screen.OpenScreenIntent.OpenRearrangeScreenIntent
import com.ionos.scanbot.screens.common.use_case.open_screen.OpenScreenIntent.OpenSaveScreenIntent
import com.ionos.scanbot.screens.gallery.GalleryScreen.ColorFilterIcon
import com.ionos.scanbot.screens.gallery.GalleryScreen.Event
import com.ionos.scanbot.screens.gallery.GalleryScreen.Event.DisplayPicturesEvent
import com.ionos.scanbot.screens.gallery.GalleryScreen.Event.OpenScreenEvent
import com.ionos.scanbot.screens.gallery.GalleryScreen.Event.PerformExitEvent
import com.ionos.scanbot.screens.gallery.GalleryScreen.Event.ShowErrorMessageEvent
import com.ionos.scanbot.screens.gallery.GalleryScreen.Event.ShowExitDialogEvent
import com.ionos.scanbot.screens.gallery.GalleryScreen.PageInfo
import com.ionos.scanbot.screens.gallery.GalleryScreen.State
import com.ionos.scanbot.screens.gallery.GalleryScreen.ViewModel
import com.ionos.scanbot.screens.gallery.use_case.GetColorFilterIcon
import com.ionos.scanbot.tracking.ScanbotGalleryScreenEventTracker
import com.ionos.scanbot.util.rx.plusAssign
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

internal class GalleryViewModel(
	initialState: State,
	private var initialPictureId: String?,
	private val getColorFilterIcon: GetColorFilterIcon,
	private val repositoryFacade: RepositoryFacade,
	private val pictureRepository: PictureRepository,
	private val eventTracker: ScanbotGalleryScreenEventTracker,
) : BaseViewModel<Event, State>(initialState, eventTracker),
	ViewModel {

	private var pictures: List<Picture> = emptyList()
	private var currentPictureIndex: Int = 0

	override fun onStart() {
		subscriptions += loadPictures()
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(::oPicturesLoaded, ::onError)
	}

	override fun onEventHandled() {
		updateState { copy(event = null) }
	}

    override fun onSuccessSaveScreenResult() {
        updateState { copy(event = PerformExitEvent) }
    }

	override fun onPageSelected(pageIndex: Int) {
		if (currentPictureIndex < pageIndex) {
			eventTracker.trackSwipeNext()
		} else if (currentPictureIndex > pageIndex) {
			eventTracker.trackSwipeBack()
		}
		currentPictureIndex = pageIndex
		val pageInfo = PageInfo(pageIndex, pictures.size)
		val filterIcon = getPictureColorFilterIcon(pageIndex)
		updateState { copy(pageInfo = pageInfo, filterIcon = filterIcon) }
	}

	override fun onBackPressed() {
		eventTracker.trackBackPressed()
		updateState { copy(event = ShowExitDialogEvent) }
	}

	override fun onExitConfirmed() {
		eventTracker.trackExitConfirmed()
		subscriptions += Completable
			.fromCallable { repositoryFacade.release() }
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(::onRepositoryReleased, ::onError)
	}

	override fun onExitDenied() {
		eventTracker.trackExitDenied()
	}

    override fun onSaveButtonClicked() {
        eventTracker.trackSaveClicked()
        updateState { copy(event = OpenScreenEvent(OpenSaveScreenIntent(closeCurrent = false))) }
    }

	override fun onAddButtonClicked() {
		eventTracker.trackAddPictureClicked()
		updateState { copy(event = OpenScreenEvent(OpenCameraScreenIntent(closeCurrent = true))) }
	}

	override fun onCropButtonClicked() {
		eventTracker.trackCropClicked()
		val picture = pictures.getOrNull(currentPictureIndex) ?: return
		val intent = OpenCropScreenIntent(picture.id, closeCurrent = false)
		updateState { copy(event = OpenScreenEvent(intent)) }
	}

	override fun onFilterButtonClicked() {
		eventTracker.trackFilterClicked()
		val picture = pictures.getOrNull(currentPictureIndex) ?: return
		val filterType = picture.original.getColorFilter().colorFilterType
		val intent = OpenFilterScreenIntent(picture.id, filterType, closeCurrent = false)
		updateState { copy(event = OpenScreenEvent(intent)) }
	}

	override fun onRotateButtonClicked() {
		eventTracker.trackRotateClicked()

		val picture = pictures.getOrNull(currentPictureIndex) ?: return
		updateState { copy(processing = true) }

		subscriptions += rotatePicture(picture)
			.andThen(loadPictures())
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(::oPicturesLoaded, ::onError)
	}

	override fun onRearrangeButtonClicked() {
		eventTracker.trackRearrangeClicked()
		updateState { copy(event = OpenScreenEvent(OpenRearrangeScreenIntent(closeCurrent = false))) }
	}

	override fun onDeleteButtonClicked() {
		eventTracker.trackDeleteClicked()

		val picture = pictures.getOrNull(currentPictureIndex) ?: return
		updateState { copy(processing = true) }

		subscriptions += Completable
			.fromCallable { repositoryFacade.delete(picture.id) }
			.andThen(loadPictures())
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
            .subscribe(::onPictureDeleted, ::onError)
	}

    private fun onPictureDeleted(remainPictures: List<Picture>) = if (remainPictures.isNotEmpty()) {
        oPicturesLoaded(remainPictures)
    } else {
        updateState { copy(processing = false, event = OpenScreenEvent(OpenCameraScreenIntent(closeCurrent = true))) }
    }

    private fun oPicturesLoaded(pictures: List<Picture>) {
        this.pictures = pictures
        this.initialPictureId?.let { currentPictureIndex = pictures.indexOr0(it) }
        this.initialPictureId = null

        val pageInfo = PageInfo(currentPictureIndex, pictures.size)
        val filterIcon = getPictureColorFilterIcon(currentPictureIndex)
        val event = DisplayPicturesEvent(pictures)

        updateState { copy(pageInfo = pageInfo, filterIcon = filterIcon, processing = false, event = event) }
    }

	private fun onRepositoryReleased() {
		updateState { copy(processing = false, event = PerformExitEvent) }
	}

	private fun onError(error: Throwable) {
		updateState { copy(event = ShowErrorMessageEvent(error)) }
	}

	private fun loadPictures(): Single<List<Picture>> = Single.fromCallable {
		pictureRepository.readAll()
	}

	private fun rotatePicture(picture: Picture): Completable = Completable.fromCallable {
		val rotateDegrees = 90f + picture.original.getRotateFilter().degrees
		repositoryFacade.update(picture.makeCopy(rotateDegrees = rotateDegrees))
	}

	private fun getPictureColorFilterIcon(pictureIndex: Int): ColorFilterIcon {
		val colorFilter = pictures.getOrNull(pictureIndex)?.original?.getColorFilter()
		val colorFilterType = colorFilter?.colorFilterType ?: ColorFilterType.None()
		return getColorFilterIcon(colorFilterType)
	}

	private fun List<Picture>.indexOr0(pictureId: String): Int {
		val index = indexOfFirst { it.id == pictureId }
		return if (index >= 0) index else 0
	}
}
