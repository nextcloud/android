/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.camera.use_case.import_pictures

import android.net.Uri
import com.ionos.scanbot.exception.ImportPictureException
import com.ionos.scanbot.screens.camera.use_case.AddPictureToRepository
import com.ionos.scanbot.screens.camera.use_case.ExtractPictureTransformation
import com.ionos.scanbot.screens.camera.use_case.ReadContentToByteArray
import com.ionos.scanbot.repository.bitmap.BitmapDecoder
import io.reactivex.Observable
import javax.inject.Inject
import com.ionos.scanbot.screens.camera.use_case.import_pictures.ImportPicturesInputItem as InputItem
import com.ionos.scanbot.screens.camera.use_case.import_pictures.ImportPicturesState as State

internal class ImportPictures @Inject constructor(
	private val bitmapDecoder: BitmapDecoder,
	private val addPictureToRepository: AddPictureToRepository,
	private val readContentToByteArray: ReadContentToByteArray,
	private val extractPictureTransformation: ExtractPictureTransformation,
) {

	operator fun invoke(uris: List<Uri>, cancellation: Observable<Boolean>): Observable<State> {
		return Observable
			.fromIterable(uris)
			.withLatestFrom(cancellation.startWith(false), ::toInputItem)
			.scan(State.Processing(uris) as State, ::processInputItem)
			.distinctUntilChanged()
	}

	private fun toInputItem(uri: Uri, cancellation: Boolean): InputItem {
		return if (cancellation) InputItem.CancellationSignal else InputItem.Picture(uri)
	}

	private fun processInputItem(state: State, inputItem: InputItem): State = when {
		state is State.Error || state is State.Canceled -> state
		inputItem is InputItem.CancellationSignal -> state.cancel()
		inputItem is InputItem.Picture -> importPicture(state, inputItem.uri)
		else -> throw IllegalStateException()
	}

	private fun importPicture(state: State, uri: Uri): State = try {
		val byteArray = readContentToByteArray(uri)
		val transformation = extractPictureTransformation(uri)
		val bitmap = bitmapDecoder.decodeSampledBitmap(byteArray, transformation)
		val processedItem = addPictureToRepository(bitmap)
		state.progress(processedItem)
	} catch (e: Exception) {
		state.error(ImportPictureException(uri, e))
	}

	private fun State.progress(processedItem: String): State {
		val processedItems = processedItems + processedItem
		val isLastItem = processedItems.size == picturesUris.size
		return when {
			isLastItem -> State.Finished(picturesUris, processedItems)
			else -> State.Processing(picturesUris, processedItems)
		}
	}

	private fun State.cancel(): State {
		return State.Canceled(picturesUris, processedItems)
	}

	private fun State.error(error: Throwable): State {
		return State.Error(picturesUris, processedItems, error)
	}
}
