/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.repository.bitmap

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import com.ionos.scanbot.exception.DecodeBitmapException
import com.ionos.scanbot.util.graphics.toRotateMatrix
import com.ionos.scanbot.util.graphics.transform
import java.io.File
import javax.inject.Inject

internal class BitmapDecoder @Inject constructor() {

	companion object {
		private const val IMAGE_TARGET_WIDTH = 1080
		private const val IMAGE_TARGET_HEIGHT = 1080
	}

	private var targetWidth = IMAGE_TARGET_WIDTH
	private var targetHeight = IMAGE_TARGET_HEIGHT

	fun setTargetWidth(targetWidth: Int): BitmapDecoder = apply {
		this.targetWidth = targetWidth
	}

	fun setTargetHeight(targetHeight: Int): BitmapDecoder = apply {
		this.targetHeight = targetHeight
	}

	fun decodeSampledBitmap(byteArray: ByteArray, transformation: Matrix? = null): Bitmap {
		return decodeSampledBitmap(ByteArraySource(byteArray), transformation)
	}

	fun decodeSampledBitmap(file: File, transformation: Matrix? = null): Bitmap {
		return decodeSampledBitmap(FileSource(file), transformation)
	}

	fun decodeSampledBitmap(byteArray: ByteArray, rotation: Int): Bitmap {
		return decodeSampledBitmap(ByteArraySource(byteArray), rotation.toRotateMatrix())
	}

	fun decodeSampledBitmap(file: File, rotation: Int): Bitmap {
		return decodeSampledBitmap(FileSource(file), rotation.toRotateMatrix())
	}

	private fun decodeSampledBitmap(source: Source, transformation: Matrix?): Bitmap {
		val options = BitmapFactory.Options()
		options.inPreferredConfig = Bitmap.Config.RGB_565
		options.inSampleSize = getInSampleSize(source)
		options.inJustDecodeBounds = false
		val bitmap = source.decode(options) ?: throw DecodeBitmapException()
		return if (transformation != null) {
			bitmap.transform(transformation)
		} else {
			bitmap
		}
	}

	private fun getInSampleSize(source: Source): Int = with(BitmapFactory.Options()) {
		inJustDecodeBounds = true
		source.decode(this)
		return calculateInSampleSize(outWidth, outHeight, targetWidth, targetHeight)
	}

	private fun calculateInSampleSize(width: Int, height: Int, targetWidth: Int, targetHeight: Int): Int {
		var inSampleSize = 1
		if (height > targetHeight || width > targetWidth) {
			val halfHeight = height / 2
			val halfWidth = width / 2
			while (halfHeight / inSampleSize >= targetHeight && halfWidth / inSampleSize >= targetWidth) {
				inSampleSize *= 2
			}
		}
		return inSampleSize
	}

	private interface Source {
		fun decode(options: BitmapFactory.Options): Bitmap?
	}

	private class FileSource(private val file: File) : Source {
		override fun decode(options: BitmapFactory.Options): Bitmap? {
			return BitmapFactory.decodeFile(file.path, options)
		}
	}

	private class ByteArraySource(private val byteArray: ByteArray) : Source {
		override fun decode(options: BitmapFactory.Options): Bitmap? {
			return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size, options)
		}
	}
}