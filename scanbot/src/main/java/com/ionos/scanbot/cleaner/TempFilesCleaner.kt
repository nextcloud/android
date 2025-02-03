/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.cleaner

import com.ionos.scanbot.provider.FileProvider
import com.ionos.scanbot.util.io.removeRecursive
import javax.inject.Inject

internal class TempFilesCleaner @Inject constructor(
	private val fileProvider: FileProvider,
) {

	fun clean(): Unit = with(fileProvider) {
		sdkTempFilesDirectory.removeRecursive()
		cacheDirectory.removeRecursive()
		imageResultsDirectory.removeRecursive()
		tempOperationsDirectory.removeRecursive()
	}
}
