/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.camera.use_case

import android.content.Context
import android.net.Uri
import com.ionos.scanbot.exception.NoFreeLocalSpaceException
import com.ionos.scanbot.util.FileUtils
import com.ionos.scanbot.R
import com.ionos.scanbot.exception.ImportPictureException
import com.ionos.scanbot.exception.NoCameraPermissionException
import javax.inject.Inject

internal class GetCameraScreenErrorMessage @Inject constructor(private val context: Context) {

	operator fun invoke(error: Throwable): String = when (error) {
		is ImportPictureException -> context.getImportPictureErrorMessage(error)
		is NoCameraPermissionException -> context.getString(R.string.scanbot_no_camera_permissions_granted)
		else -> context.getString(R.string.scanbot_fail)
	}

	private fun Context.getImportPictureErrorMessage(error: ImportPictureException) = when {
		error.cause is NoFreeLocalSpaceException -> getString(R.string.scanbot_no_free_space_message)
		else -> getFailedImageMessage(error.pictureUri)
	}

	private fun Context.getFailedImageMessage(failedImageUri: Uri): String {
		val fileName = FileUtils.extractFileName(failedImageUri.path)
		val decodedFileName = Uri.decode(fileName)
		return getString(R.string.scanbot_file_import_failed, decodedFileName)
	}
}