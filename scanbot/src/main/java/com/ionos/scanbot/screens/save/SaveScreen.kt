/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.save

import com.ionos.scanbot.upload.target_provider.UploadTarget
import com.ionos.scanbot.screens.base.BaseScreen

internal interface SaveScreen {

	data class State(
		val baseFileName: String,
		val fileType: FileType = FileType.PDF_OCR,
		val targetPath: String = "",
		val processing: Boolean = false,
		override val event: Event? = null,
	) : BaseScreen.State<Event>

	enum class FileType { PDF_OCR, PDF, JPG, PNG }

	sealed interface Event : BaseScreen.Event {
		data class LaunchUploadTargetPickerEvent(val initialTarget: UploadTarget, val fileName: String) : Event
		data class HandleErrorEvent(val error: Throwable) : Event
        data class CloseScreenEvent(val successResult: Boolean) : Event
	}

	interface ViewModel : BaseScreen.ViewModel<Event, State> {

		fun onBackPressed()

		fun onFileNameChanged(baseFileName: String)

		fun onFileTypeChanged(fileType: FileType)

		fun onSaveLocationPathClicked()

		fun onUploadTargetPicked(uploadTarget: UploadTarget)

		fun onUploadTargetPickerCanceled()

		fun onSaveClicked()

		fun onOverwriteDialogsResult(overwritePaths: List<String>, allowOverwritePaths: List<String>)
	}
}
