/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.camera.use_case

import android.content.Context
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.ionos.scanbot.util.graphics.getOrientationMatrix
import javax.inject.Inject

internal class ExtractPictureTransformation @Inject constructor(
	private val context: Context,
) {

	operator fun invoke(data: Uri): Matrix? = context
		.contentResolver
		.openInputStream(data)
		?.let { ExifInterface(it) }
		?.getOrientationMatrix()
}