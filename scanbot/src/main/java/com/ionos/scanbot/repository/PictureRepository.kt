/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.repository

import com.ionos.scanbot.entity.ModifiedPicture
import com.ionos.scanbot.entity.OriginalPicture
import com.ionos.scanbot.entity.Picture
import com.ionos.scanbot.entity.SelectedContour
import com.ionos.scanbot.filter.color.ColorFilter
import com.ionos.scanbot.filter.color.ColorFilterType
import com.ionos.scanbot.filter.crop.CropFilter
import com.ionos.scanbot.filter.rotate.RotateFilter
import com.ionos.scanbot.util.collections.OrderedHashMap
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
internal class PictureRepository @Inject constructor() {
	private val items = OrderedHashMap<String, Picture>()

	fun isEmpty(): Boolean {
		return items.isEmpty()
	}

	fun create(originalBitmapRepoId: String, contour: SelectedContour): String {
		val picture = Picture(
			UUID.randomUUID().toString(),
			OriginalPicture(
				originalBitmapRepoId,
				CropFilter(contour),
				ColorFilter(ColorFilterType.None()),
				RotateFilter(0f),
			),
			ModifiedPicture(""),
		)
		return create(picture)
	}

	fun create(pictures: List<Picture>) = pictures.forEach {
		create(it)
	}

	fun create(picture: Picture): String {
		items[picture.id] = picture
		return picture.id
	}

	fun read(id: String): Picture? {
		return items[id]
	}

	fun readAll(): List<Picture> = items.values.toList()

	fun swap(firstId: String, secondId: String) {
		items.swapOrder(firstId, secondId)
	}

	fun update(picture: Picture) {
		items[picture.id]?.let { oldPicture ->
			val updatedPicture = oldPicture.copy(
				original = picture.original,
				modified = picture.modified,
			)
			items.put(picture.id, updatedPicture)
		}
	}

	fun delete(id: String) {
		items.remove(id)
	}

	fun release() {
		items.clear()
	}
}
