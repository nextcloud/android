/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.save

import android.net.Uri
import com.ionos.scanbot.controller.ScanbotController
import com.ionos.scanbot.provider.DocumentNameProvider
import com.ionos.scanbot.repository.RepositoryFacade
import com.ionos.scanbot.screens.base.BaseViewModel
import com.ionos.scanbot.screens.save.SaveScreen.Event
import com.ionos.scanbot.screens.save.SaveScreen.Event.CloseScreenEvent
import com.ionos.scanbot.screens.save.SaveScreen.Event.HandleErrorEvent
import com.ionos.scanbot.screens.save.SaveScreen.Event.LaunchUploadTargetPickerEvent
import com.ionos.scanbot.screens.save.SaveScreen.FileType
import com.ionos.scanbot.screens.save.SaveScreen.State
import com.ionos.scanbot.screens.save.SaveScreen.ViewModel
import com.ionos.scanbot.screens.save.use_case.ValidateFilesForUploadSynchronous
import com.ionos.scanbot.screens.save.use_case.save.SaveDocument
import com.ionos.scanbot.tracking.ScanbotSaveScreenEventTracker
import com.ionos.scanbot.upload.target_provider.UploadTarget
import com.ionos.scanbot.util.rx.plusAssign
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal class SaveViewModel @Inject constructor(
	private val saveDocument: SaveDocument,
	private val validateFiles: ValidateFilesForUploadSynchronous,
	private val scanbotController: ScanbotController,
	private val repositoryFacade: RepositoryFacade,
	private val eventTracker: ScanbotSaveScreenEventTracker,
	documentNameProvider: DocumentNameProvider,
) : BaseViewModel<Event, State>(State(documentNameProvider.getName()), eventTracker),
	ViewModel {

	companion object {
		private const val USER_STOP_TYPING_TIMEOUT = 1000L
	}

	private val fileNameChangedSubject = PublishSubject.create<String>()
	private var uploadTarget by scanbotController.uploadTargetRepository::uploadTarget

	init {
		updateState { copy(processing = true) }
		updateTargetPath(uploadTarget)

		subscriptions += fileNameChangedSubject
			.throttleWithTimeout(USER_STOP_TYPING_TIMEOUT, TimeUnit.MILLISECONDS)
			.subscribe { eventTracker.trackFileNameChanged() }
	}

	override fun onEventHandled() {
		updateState { copy(event = null) }
	}

    override fun onBackPressed() {
        eventTracker.trackBackPressed()
        updateState { copy(event = CloseScreenEvent(successResult = false)) }
    }

	override fun onFileNameChanged(baseFileName: String) {
		if (state().baseFileName != baseFileName) {
			fileNameChangedSubject.onNext(baseFileName)
			updateState { copy(baseFileName = baseFileName) }
		}
	}

	override fun onFileTypeChanged(fileType: FileType) {
		if (state().fileType != fileType) {
			when (fileType) {
				FileType.PDF_OCR -> eventTracker.trackPdfOcrFileTypeSelected()
				FileType.PDF -> eventTracker.trackPdfFileTypeSelected()
				FileType.JPG -> eventTracker.trackJpgFileTypeSelected()
				FileType.PNG -> eventTracker.trackPngFileTypeSelected()
			}
			updateState { copy(fileType = fileType) }
		}
	}

	override fun onSaveLocationPathClicked() {
		eventTracker.trackSaveLocationPathClicked()
		updateState { copy(event = LaunchUploadTargetPickerEvent(uploadTarget, state().baseFileName)) }
	}

	override fun onUploadTargetPicked(uploadTarget: UploadTarget) {
		eventTracker.trackSaveLocationPathChanged()
		this.uploadTarget = uploadTarget
		updateState { copy(processing = true) }
		updateTargetPath(uploadTarget)
	}

	override fun onUploadTargetPickerCanceled() {
		eventTracker.trackSaveLocationPathChangeCanceled()
	}

	override fun onSaveClicked() {
		eventTracker.trackSaveClicked()

        runCatching {
            validateFiles(state().baseFileName)
        }
            .onSuccess{ saveDocument() }
            .onFailure(::onError)
	}

	override fun onOverwriteDialogsResult(overwritePaths: List<String>, allowOverwritePaths: List<String>) {
		if (allowOverwritePaths.containsAll(overwritePaths)) {
			saveDocument()
		}
	}

	private fun updateTargetPath(uploadTarget: UploadTarget) {
        onTargetPathUpdated(uploadTarget.uploadPath)
	}

	private fun saveDocument() {
        updateState { copy(processing = true) }

        subscriptions += saveDocument(state().baseFileName, state().fileType)
            .doOnSuccess { repositoryFacade.release() }
            .subscribeOn(Schedulers.io())
			.observeOn(AndroidSchedulers.mainThread())
			.subscribe(::onDocumentSaved, ::onError)
	}

	private fun onTargetPathUpdated(targetPath: String) {
		updateState { copy(processing = false, targetPath = targetPath) }
	}

	private fun onDocumentSaved(uris: List<Uri>) {
		scanbotController.onDocumentSaved(uris)
        updateState { copy(processing = false, event = CloseScreenEvent(successResult = true)) }
	}

	private fun onError(error: Throwable) {
		updateState { copy(processing = false, event = HandleErrorEvent(error)) }
	}
}
