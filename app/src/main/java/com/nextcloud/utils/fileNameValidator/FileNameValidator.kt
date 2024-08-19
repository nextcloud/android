/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.fileNameValidator

import android.text.TextUtils

object FileNameValidator {
    private val reservedWindowsChars = "[<>:\"/\\\\|?*]".toRegex()
    private val reservedWindowsNames = listOf(
        "CON", "PRN", "AUX", "NUL",
        "COM0", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "COM¹", "COM²", "COM³",
        "LPT0", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9",
        "LPT¹", "LPT²", "LPT³"
    )
    private val reservedUnixChars = "[/<>|:&]".toRegex()

    fun isValid(name: String): FileNameValidationResult? {
        if (name.contains(reservedWindowsChars) || name.contains(reservedUnixChars)) {
            return FileNameValidationResult.INVALID_CHARACTER
        }

        if (reservedWindowsNames.contains(name.uppercase())) {
            return FileNameValidationResult.RESERVED_NAME
        }

        if (name.endsWith(" ") || name.endsWith(".")) {
            return FileNameValidationResult.ENDS_WITH_SPACE_OR_PERIOD
        }

        if (TextUtils.isEmpty(name)) {
            return FileNameValidationResult.EMPTY
        }

        return null
    }
}
