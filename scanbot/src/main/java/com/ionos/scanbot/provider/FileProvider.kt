/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.provider

import android.content.Context
import java.io.File
import javax.inject.Inject

 class FileProvider @Inject constructor(context: Context) {

	companion object {
		const val SDK_FILES_DIRECTORY_NAME = "scanbot-sdk"
		const val SDK_TEMP_FILES_DIRECTORY_NAME = "snapping_documents"
		const val CACHE_DIRECTORY_NAME = "scanbot_cache_directory"
		const val IMAGE_RESULTS_DIRECTORY_NAME = "scanbotImageResults"
		const val TEMP_OPERATIONS_DIRECTORY_NAME = "scanbot_temp_operations_directory"
	}

	val sdkFilesDirectory: File = File(context.filesDir, SDK_FILES_DIRECTORY_NAME)

	val sdkTempFilesDirectory: File = File(sdkFilesDirectory, SDK_TEMP_FILES_DIRECTORY_NAME)

	val cacheDirectory: File = context.getDir(CACHE_DIRECTORY_NAME, Context.MODE_PRIVATE)

	val imageResultsDirectory: File = context.getDir(IMAGE_RESULTS_DIRECTORY_NAME, Context.MODE_PRIVATE)

	val tempOperationsDirectory: File = context.getDir(TEMP_OPERATIONS_DIRECTORY_NAME, Context.MODE_PRIVATE)
}
