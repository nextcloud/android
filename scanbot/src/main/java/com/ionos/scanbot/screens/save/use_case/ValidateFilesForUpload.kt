/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.save.use_case

import com.ionos.scanbot.exception.InvalidFileNameException
import com.ionos.scanbot.util.FileUtils
import io.reactivex.Completable
import javax.inject.Inject

internal class ValidateFilesForUpload @Inject constructor(
) {

	operator fun invoke(baseFileName: String): Completable {
		return validateBaseFileName(baseFileName)
	}

	private fun validateBaseFileName(baseFileName: String) = Completable.fromCallable {
		if (!FileUtils.isValidName(baseFileName)) {
			throw InvalidFileNameException(baseFileName)
		}
	}
}
