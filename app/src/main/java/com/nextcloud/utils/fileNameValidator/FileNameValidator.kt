/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.fileNameValidator

import android.content.Context
import android.text.TextUtils
import com.owncloud.android.R

object FileNameValidator {
    private val reservedWindowsChars = "[<>:\"/\\\\|?*]".toRegex()
    private val reservedUnixChars = "[/<>|:&]".toRegex()
    private val reservedWindowsNames = listOf(
        "CON", "PRN", "AUX", "NUL",
        "COM0", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "COM¹", "COM²", "COM³",
        "LPT0", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9",
        "LPT¹", "LPT²", "LPT³"
    )

    @Suppress("ReturnCount")
    fun isValid(name: String, context: Context, fileNames: MutableSet<String>? = null): String? {
        val invalidCharacter = name.find {
            it.toString().matches(reservedWindowsChars) || it.toString().matches(reservedUnixChars)
        }
        if (invalidCharacter != null) {
            return context.getString(R.string.file_name_validator_error_invalid_character, invalidCharacter)
        }

        if (reservedWindowsNames.contains(name.uppercase())) {
            return context.getString(R.string.file_name_validator_error_reserved_names)
        }

        if (name.endsWith(" ") || name.endsWith(".")) {
            return context.getString(R.string.file_name_validator_error_ends_with_space_period)
        }

        if (TextUtils.isEmpty(name)) {
            return context.getString(R.string.filename_empty)
        }

        if (isFileNameAlreadyExist(name, fileNames ?: mutableSetOf())) {
            return context.getString(R.string.file_already_exists)
        }

        return null
    }

    fun isFileHidden(name: String): Boolean = !TextUtils.isEmpty(name) && name[0] == '.'

    fun isFileNameAlreadyExist(name: String, fileNames: MutableSet<String>): Boolean = fileNames.contains(name)
}
