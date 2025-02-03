/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.save.use_case.save

import android.graphics.Bitmap
import android.net.Uri
import com.ionos.scanbot.entity.Picture
import com.ionos.scanbot.provider.FileProvider
import com.ionos.scanbot.repository.RepositoryFacade
import com.ionos.scanbot.screens.save.use_case.name.GetImageFileNames
import com.ionos.scanbot.repository.bitmap.BitmapSaver
import com.ionos.scanbot.entity.ImageFormat
import com.ionos.scanbot.exception.ReadPictureBitmapException
import com.ionos.scanbot.exception.SaveDocumentException
import io.reactivex.Observable
import io.reactivex.Single
import java.io.File
import javax.inject.Inject

internal class SaveImages @Inject constructor(
	private val bitmapSaver: BitmapSaver,
	private val repositoryFacade: RepositoryFacade,
	private val getImageFileNames: GetImageFileNames,
) {

	operator fun invoke(baseFileName: String, imageFormat: ImageFormat): Single<List<Uri>> {
		bitmapSaver.setDirectoryName(FileProvider.IMAGE_RESULTS_DIRECTORY_NAME)
		bitmapSaver.setImageFormat(imageFormat)
		return Single
			.fromCallable { repositoryFacade.readAll() }
			.flatMap { it.save(baseFileName, imageFormat) }
			.onErrorResumeNext { Single.error(SaveDocumentException(it)) }
	}

	private fun List<Picture>.save(baseFileName: String, imageFormat: ImageFormat): Single<List<Uri>> {
		val fileNames = getImageFileNames(baseFileName, imageFormat, size)
		return Observable
			.range(0, size)
			.flatMapSingle { index -> this[index].save(fileNames[index]) }
			.toList()
	}

	private fun Picture.save(fileName: String): Single<Uri> = readBitmap(this)
		.flatMap { saveBitmap(it, fileName) }
		.map { Uri.fromFile(it) }

	private fun readBitmap(picture: Picture): Single<Bitmap> = Single.fromCallable {
		repositoryFacade.readModifiedBitmap(picture.id) ?: throw ReadPictureBitmapException(picture.id)
	}

	private fun saveBitmap(bitmap: Bitmap, fileName: String): Single<File> = Single.fromCallable {
		bitmapSaver.setFileName(fileName)
		bitmapSaver.save(bitmap)
		bitmapSaver.loadFile()
	}
}
