/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.repository

import android.graphics.Bitmap
import com.ionos.scanbot.entity.ModifiedPicture
import com.ionos.scanbot.entity.Picture
import com.ionos.scanbot.entity.SelectedContour
import com.ionos.scanbot.filter.FilterType
import com.ionos.scanbot.provider.SdkProvider
import com.ionos.scanbot.repository.bitmap.BitmapRepository
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
internal class RepositoryFacade @Inject constructor(
	private val sdkProvider: SdkProvider,
	private val bitmapRepository: BitmapRepository,
	private val pictureRepository: PictureRepository,
) {

	fun isEmpty(): Boolean {
		return pictureRepository.isEmpty()
	}

	fun create(bitmap: Bitmap, contour: SelectedContour): String {
		val origId = bitmapRepository.create(bitmap)
		val id = pictureRepository.create(origId, contour)
		pictureRepository.read(id)?.let { picture -> update(picture, bitmap) }
		return id
	}

	fun create (picture: Picture) = pictureRepository.create(picture)

	fun create (pictures: List<Picture>) = pictureRepository.create(pictures)

	fun read(id: String): Picture? {
		return pictureRepository.read(id)
	}

	fun readAll(): List<Picture> {
		return pictureRepository.readAll()
	}

	fun readOriginalBitmapWithFilters(id: String, filterTypes: Set<FilterType>): Bitmap? {
		return pictureRepository.read(id)?.original?.let { originalPicture ->
			bitmapRepository.read(originalPicture.id)?.let { originalBitmap ->
				originalPicture.applyFilters(filterTypes, originalBitmap)
			}
		}
	}

	fun readOriginalBitmap(id: String) = pictureRepository.read(id)?.original?.id?.let { bitmapRepository.read(it) }

	fun readModifiedBitmap(id: String) = pictureRepository.read(id)?.modified?.id?.let { bitmapRepository.read(it) }

	fun readModifiedFile(id: String) = pictureRepository.read(id)?.modified?.id?.let { bitmapRepository.readFile(it) }

	fun update(picture: Picture) {
		bitmapRepository.read(picture.original.id)?.let { originalBitmap ->
			update(picture, originalBitmap)
		}
	}

	fun update(picture: Picture, originalBitmap: Bitmap) {
		bitmapRepository.delete(picture.modified.id)
		val filterTypes = setOf(FilterType.COLOR, FilterType.CROP, FilterType.ROTATE)
		picture.original.applyFilters(filterTypes, originalBitmap)
			?.let { modifiedBitmap -> bitmapRepository.create(modifiedBitmap) }
			?.let { modifiedPictureId ->
				pictureRepository.update(
					picture.copy(modified = ModifiedPicture(modifiedPictureId)))
			}
	}

	fun delete(id: String) {
		pictureRepository.read(id)?.apply {
			bitmapRepository.delete(original.id)
			bitmapRepository.delete(modified.id)
			pictureRepository.delete(id)
		}
	}

	fun release() {
		bitmapRepository.release()
		pictureRepository.release()
	}
}