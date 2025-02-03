/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.util.graphics

import android.graphics.Bitmap
import android.graphics.Matrix

internal fun Bitmap.transform(matrix: Matrix): Bitmap {
	return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

internal fun Bitmap.rotate(degrees: Float): Bitmap {
	val matrix = degrees.toRotateMatrix() ?: return this
	return transform(matrix)
}