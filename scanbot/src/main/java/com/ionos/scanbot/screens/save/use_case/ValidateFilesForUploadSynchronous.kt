/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.save.use_case

import com.ionos.scanbot.exception.InvalidFileNameException
import com.ionos.scanbot.util.FileUtils
import javax.inject.Inject

internal class ValidateFilesForUploadSynchronous @Inject constructor(
) {

    @Throws(InvalidFileNameException::class)
    operator fun invoke(baseFileName: String) {
        if (!FileUtils.isValidName(baseFileName))
            throw InvalidFileNameException(baseFileName)
    }

}
