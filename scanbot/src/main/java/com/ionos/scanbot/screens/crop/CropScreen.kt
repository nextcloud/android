/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.crop

import android.graphics.Bitmap
import android.graphics.PointF
import com.ionos.scanbot.screens.base.BaseScreen

internal interface CropScreen {

	data class State(
		val processing: Boolean = false,
		val buttonType: ButtonType = ButtonType.RESET,
		override val event: Event? = null,
	) : BaseScreen.State<Event>

	enum class ButtonType {
		AUTODETECT,
		RESET,
	}

	sealed interface Event : BaseScreen.Event {
		data class DisplayPictureEvent(val bitmap: Bitmap) : Event
		data class DisplayPolygonEvent(val polygon: List<PointF>) : Event
		data class ShowErrorMessageEvent(val error: Throwable) : Event
		object CloseScreenEvent : Event
	}

	interface ViewModel : BaseScreen.ViewModel<Event, State> {

		fun onStart()

		fun onBackPressed()

		fun onAutoDetectContourClicked()

		fun onResetContourClicked()

		fun onSaveClicked(polygon: List<PointF>)
	}
}