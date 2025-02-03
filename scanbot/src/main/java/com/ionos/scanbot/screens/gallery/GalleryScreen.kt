/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.gallery

import androidx.annotation.DrawableRes
import com.ionos.scanbot.entity.Picture
import com.ionos.scanbot.screens.base.BaseScreen
import com.ionos.scanbot.screens.common.use_case.open_screen.OpenScreenIntent

internal interface GalleryScreen {

	data class State(
		val title: String,
		val filterIcon: ColorFilterIcon,
		val pageInfo: PageInfo? = null,
		val processing: Boolean = false,
		override val event: Event? = null,
	) : BaseScreen.State<Event>

	data class ColorFilterIcon(
		@DrawableRes val imageRes: Int,
		@DrawableRes val backgroundRes: Int,
	)

	data class PageInfo(
		val index: Int,
		val total: Int,
	)

	sealed interface Event : BaseScreen.Event {
		data class DisplayPicturesEvent(val pictures: List<Picture>) : Event
		data class OpenScreenEvent(val intent: OpenScreenIntent) : Event
		object PerformExitEvent : Event
		object ShowExitDialogEvent : Event
		data class ShowErrorMessageEvent(val error: Throwable) : Event
	}

	interface ViewModel : BaseScreen.ViewModel<Event, State> {

		fun onStart()

        fun onSuccessSaveScreenResult()

		fun onPageSelected(pageIndex: Int)

		fun onBackPressed()

		fun onExitConfirmed()

		fun onExitDenied()

		fun onSaveButtonClicked()

		fun onAddButtonClicked()

		fun onCropButtonClicked()

		fun onFilterButtonClicked()

		fun onRotateButtonClicked()

		fun onRearrangeButtonClicked()

		fun onDeleteButtonClicked()
	}
}
