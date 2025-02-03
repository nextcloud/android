/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.camera.use_case

import android.content.Context
import android.net.Uri
import com.ionos.scanbot.exception.OpenFileException
import javax.inject.Inject

internal class ReadContentToByteArray @Inject constructor(
	private val context: Context,
) {

	operator fun invoke(uri: Uri): ByteArray = context
		.contentResolver
		.openInputStream(uri)
		.let { it ?: throw OpenFileException(uri.path) }
		.buffered()
		.readBytes()
}