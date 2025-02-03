/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.controller

import android.os.Bundle
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ionos.scanbot.upload.target_provider.UploadTarget
import com.ionos.scanbot.entity.Picture
import com.ionos.scanbot.filter.color.ColorFilterType
import com.ionos.scanbot.util.gson.RuntimeTypeAdapterFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

internal class StateManager {

	companion object {
		private const val UPLOAD_TARGET_STATE_KEY = "UPLOAD_TARGET_STATE_KEY"
		private const val PICTURES_STATE_KEY = "PICTURES_STATE_KEY"
	}

	private val gson: Gson = createGson()

	private fun createGson(): Gson {
		val typeAdapter = RuntimeTypeAdapterFactory
			.of(ColorFilterType::class.java)
			.registerSubtype(ColorFilterType.MagicColor::class.java)
			.registerSubtype(ColorFilterType.Color::class.java)
			.registerSubtype(ColorFilterType.None::class.java)
			.registerSubtype(ColorFilterType.BlackWhite::class.java)
			.registerSubtype(ColorFilterType.MagicText::class.java)
			.registerSubtype(ColorFilterType.Grayscale::class.java)

		return GsonBuilder()
			.registerTypeAdapterFactory(typeAdapter)
			.create()
	}

	fun saveUploadTarget(uploadTarget: UploadTarget, state: Bundle) {
		state.putSerializable(UPLOAD_TARGET_STATE_KEY, uploadTarget)
	}

	fun restoreUploadTarget(state: Bundle, onRestore: (UploadTarget) -> Unit) {
		state
			.getSerializable(UPLOAD_TARGET_STATE_KEY)
			?.let { it as? UploadTarget }
			?.let { onRestore(it) }
	}

	fun savePictures(pictures: List<Picture>, state: Bundle) {
		val compressedPictures = compressPictures(pictures)
		state.putByteArray(PICTURES_STATE_KEY, compressedPictures)
	}

	fun restorePictures(state: Bundle, onRestore: (List<Picture>) -> Unit) {
		state
			.getByteArray(PICTURES_STATE_KEY)
			?.let { decompressPictures(it) }
			?.let { onRestore(it.toList()) }
	}

	private fun compressPictures(pictures: List<Picture>): ByteArray = try {
		val byteArrayOutputStream = ByteArrayOutputStream()
		val gzipOutputStream = GZIPOutputStream(byteArrayOutputStream)
		val writer = OutputStreamWriter(gzipOutputStream, Charsets.UTF_8)
		writer.use { gson.toJson(pictures, it) }
		byteArrayOutputStream.toByteArray()
	} catch (e: Exception) {
		ByteArray(0)
	}

	private fun decompressPictures(byteArray: ByteArray): Array<Picture> = try {
		val gzipInputStream = GZIPInputStream(ByteArrayInputStream(byteArray))
		val reader = InputStreamReader(gzipInputStream, Charsets.UTF_8)
		reader.use { gson.fromJson(it, Array<Picture>::class.java) }
	} catch (e: Exception) {
		emptyArray()
	}
}
