/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.fileNameValidator

import android.content.Context
import android.text.TextUtils
import com.nextcloud.utils.extensions.removeFileExtension
import com.owncloud.android.R
import com.owncloud.android.lib.resources.status.OCCapability

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
    fun isValid(
        name: String,
        capability: OCCapability,
        context: Context,
        fileNames: MutableSet<String>? = null
    ): String? {
        if (TextUtils.isEmpty(name)) {
            return context.getString(R.string.filename_empty)
        }

        if (isFileNameAlreadyExist(name, fileNames ?: mutableSetOf())) {
            return context.getString(R.string.file_already_exists)
        }

        if (name.endsWith(" ") || name.endsWith(".")) {
            return context.getString(R.string.file_name_validator_error_ends_with_space_period)
        }

        checkInvalidCharacters(name, capability, context)?.let {
            return it
        }

        if (capability.forbiddenFilenames.isTrue &&
            (reservedWindowsNames.contains(name.uppercase()) || reservedWindowsNames.contains(name.removeFileExtension().uppercase()))
        ) {
            return context.getString(R.string.file_name_validator_error_reserved_names)
        }

        if (capability.forbiddenFilenameExtension.isTrue) {
            // TODO add logic
        }

        return null
    }

    fun checkPath(folderPath: String, filePaths: List<String>, capability: OCCapability, context: Context): Boolean {
        val folderPaths = folderPath.split("/", "\\")

        for (item in folderPaths) {
            if (isValid(item, capability, context) != null) {
                return false
            }
        }

        for (item in filePaths) {
            if (isValid(item, capability, context) != null) {
                return false
            }
        }

        return true
    }

    private fun checkInvalidCharacters(name: String, capability: OCCapability, context: Context): String? {
        if (capability.forbiddenFilenameCharacters.isFalse) return null

        val invalidCharacter = name.find {
            val input = it.toString()
            input.matches(reservedWindowsChars) || input.matches(reservedUnixChars)
        }

        if (invalidCharacter == null) return null

        return context.getString(R.string.file_name_validator_error_invalid_character, invalidCharacter)
    }

    fun isFileHidden(name: String): Boolean = !TextUtils.isEmpty(name) && name[0] == '.'

    fun isFileNameAlreadyExist(name: String, fileNames: MutableSet<String>): Boolean = fileNames.contains(name)
}
