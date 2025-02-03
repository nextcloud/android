/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.util.graphics

import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface

internal fun ExifInterface.getOrientationMatrix(): Matrix? {
	val matrix = Matrix()
	val orientation = getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)

	return with(matrix) {
		when (orientation) {
			ExifInterface.ORIENTATION_ROTATE_90 -> rotate(90f)
			ExifInterface.ORIENTATION_ROTATE_180 -> rotate(180f)
			ExifInterface.ORIENTATION_ROTATE_270 -> rotate(270f)
			ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flipHorizontal()
			ExifInterface.ORIENTATION_FLIP_VERTICAL -> flipVertical()
			ExifInterface.ORIENTATION_TRANSPOSE -> flipHorizontal().rotate(270f)
			ExifInterface.ORIENTATION_TRANSVERSE -> flipHorizontal().rotate(90f)
			else -> null
		}
	}
}
