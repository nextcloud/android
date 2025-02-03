/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.camera

import android.net.Uri
import com.ionos.scanbot.screens.base.BaseScreen
import com.ionos.scanbot.screens.common.use_case.open_screen.OpenScreenIntent

internal interface CameraScreen {

	data class State(
		val flashEnabled: Boolean = false,
		val automaticCaptureEnabled: Boolean = true,
		val processing: Processing = Processing.None,
		override val event: Event? = null,
	) : BaseScreen.State<Event>

	sealed interface Processing {
		data class Import(val progress: Int) : Processing
		object Decoding : Processing
		object None : Processing
	}

	sealed interface Event : BaseScreen.Event {
		object ShowExitDialogEvent : Event
		object CloseScreenEvent : Event
		data class OpenScreenEvent(val intent: OpenScreenIntent) : Event
		data class ShowMessageEvent(val message: String) : Event
		object ShowNoFreeSpaceAlertEvent : Event
		object TakePictureEvent : Event
		object LaunchImagePickerEvent : Event
	}

	interface ViewModel : BaseScreen.ViewModel<Event, State> {

		fun onCreate()

		fun onCancelClicked()

		fun onBackPressed()

		fun onExitConfirmed()

		fun onExitDenied()

		fun onAutomaticCaptureToggled()

		fun onFlashToggled()

		fun onTakePictureClicked()

		fun onImportClicked()

		fun onCancelImportClicked()

		fun onPictureReceived(pictureBytes: ByteArray, pictureOrientation: Int)

		fun onPicturesUrisReceived(picturesUris: List<Uri>)
	}
}