/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.rearrange

import com.ionos.scanbot.entity.Picture
import com.ionos.scanbot.repository.PictureRepository
import com.ionos.scanbot.screens.base.BaseViewModel
import com.ionos.scanbot.screens.rearrange.RearrangeScreen.Event
import com.ionos.scanbot.screens.rearrange.RearrangeScreen.Event.CloseScreenEvent
import com.ionos.scanbot.screens.rearrange.RearrangeScreen.Event.ShowErrorMessageEvent
import com.ionos.scanbot.screens.rearrange.RearrangeScreen.Event.ShowItemsEvent
import com.ionos.scanbot.screens.rearrange.RearrangeScreen.State
import com.ionos.scanbot.screens.rearrange.RearrangeScreen.ViewModel
import com.ionos.scanbot.screens.rearrange.recycler.RearrangeItem
import com.ionos.scanbot.tracking.ScanbotRearrangeScreenEventTracker
import com.ionos.scanbot.util.rx.plusAssign
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

internal class RearrangeViewModel @Inject constructor(
	private val pictureRepository: PictureRepository,
    private val eventTracker: ScanbotRearrangeScreenEventTracker,
) : BaseViewModel<Event, State>(State(), eventTracker),
	ViewModel {

	private var pictures: List<Picture> = emptyList()

	override fun onStart() {
		subscriptions += loadPictures()
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(::onPicturesLoaded, ::onError)
	}

	override fun onEventHandled() {
		updateState { copy(event = null) }
	}

	override fun onBackPressed() {
		eventTracker.trackBackPressed()
		updateState { copy(event = CloseScreenEvent) }
	}

	override fun onPictureDragged(sourcePosition: Int, targetPosition: Int) {
		eventTracker.trackPictureDragged()

		updateState { copy(processing = true) }

		subscriptions += swapPictures(sourcePosition, targetPosition)
			.andThen(loadPictures())
			.subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(::onPicturesLoaded, ::onError)
	}

	private fun loadPictures(): Single<List<Picture>> = Single.fromCallable {
		pictureRepository.readAll()
	}

	private fun swapPictures(sourcePosition: Int, targetPosition: Int) = Completable.fromCallable {
		pictureRepository.swap(pictures[sourcePosition].id, pictures[targetPosition].id)
	}

	private fun onPicturesLoaded(pictures: List<Picture>) {
		this.pictures = pictures
		val items = pictures.mapIndexed { index, picture ->
			RearrangeItem(picture, sequenceNumber = index + 1)
		}
		updateState { copy(event = ShowItemsEvent(items), processing = false) }
	}

	private fun onError(error: Throwable) {
		updateState { copy(event = ShowErrorMessageEvent(error), processing = false) }
	}
}
