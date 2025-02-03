/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.entity

import com.ionos.scanbot.filter.color.ColorFilter
import com.ionos.scanbot.filter.color.ColorFilterType
import com.ionos.scanbot.filter.crop.CropFilter
import com.ionos.scanbot.filter.rotate.RotateFilter


 internal data class Picture(
	val id: String,
	val original: OriginalPicture,
	val modified: ModifiedPicture,
) {

	fun makeCopy(contour: SelectedContour) = copy(
		original = this.original.copy(cropFilter = CropFilter(contour)),
	)

	fun makeCopy(colorFilterType: ColorFilterType) = copy(
		original = this.original.copy(colorFilter = ColorFilter(colorFilterType)),
	)

	fun makeCopy(rotateDegrees: Float) = copy(
		original = this.original.copy(rotateFilter = RotateFilter(degrees = rotateDegrees)),
	)
}