/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.fileNameValidator

import android.content.Context
import android.text.TextUtils
import com.nextcloud.utils.extensions.StringConstants
import com.nextcloud.utils.extensions.forbiddenFilenameBaseNames
import com.nextcloud.utils.extensions.forbiddenFilenameCharacters
import com.nextcloud.utils.extensions.forbiddenFilenameExtension
import com.nextcloud.utils.extensions.forbiddenFilenames
import com.nextcloud.utils.extensions.removeFileExtension
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.status.OCCapability

object FileNameValidator {

    /**
     * Checks the validity of a file name.
     *
     * @param filename The name of the file to validate.
     * @param capability The capabilities affecting the validation criteria
     * such as forbiddenFilenames, forbiddenCharacters.
     * @param context The context used for retrieving error messages.
     * @param existedFileNames Set of existing file names to avoid duplicates.
     * @return An error message if the filename is invalid, null otherwise.
     */
    @Suppress("ReturnCount")
    fun checkFileName(
        filename: String,
        capability: OCCapability,
        context: Context,
        existedFileNames: MutableSet<String>? = null
    ): String? {
        if (TextUtils.isEmpty(filename)) {
            return context.getString(R.string.filename_empty)
        }

        existedFileNames?.let {
            if (isFileNameAlreadyExist(filename, existedFileNames)) {
                return context.getString(R.string.file_already_exists)
            }
        }

        if (filename.endsWith(StringConstants.SPACE) || filename.endsWith(StringConstants.DOT)) {
            return context.getString(R.string.file_name_validator_error_ends_with_space_period)
        }

        checkInvalidCharacters(filename, capability, context)?.let {
            return it
        }

        capability.forbiddenFilenameBaseNames().let {
            val forbiddenFilenameBaseNames = capability.forbiddenFilenameBaseNames().map { it.lowercase() }

            if (forbiddenFilenameBaseNames.contains(filename.lowercase()) || forbiddenFilenameBaseNames.contains(
                    filename.removeFileExtension().lowercase()
                )
            ) {
                return context.getString(
                    R.string.file_name_validator_error_reserved_names,
                    filename.substringBefore(StringConstants.DOT)
                )
            }
        }

        capability.forbiddenFilenamesJson?.let {
            val forbiddenFilenames = capability.forbiddenFilenames().map { it.lowercase() }

            if (forbiddenFilenames.contains(filename.uppercase()) || forbiddenFilenames.contains(
                    filename.removeFileExtension().uppercase()
                )
            ) {
                return context.getString(
                    R.string.file_name_validator_error_reserved_names,
                    filename.substringBefore(StringConstants.DOT)
                )
            }
        }

        capability.forbiddenFilenameExtensionJson?.let {
            val forbiddenFilenameExtension = capability.forbiddenFilenameExtension()

            if (forbiddenFilenameExtension.any { filename.endsWith(it, ignoreCase = true) }) {
                return context.getString(
                    R.string.file_name_validator_error_forbidden_file_extensions,
                    filename.substringAfter(StringConstants.DOT)
                )
            }
        }

        return null
    }

    /**
     * Checks the validity of file paths wanted to move or copied inside the folder.
     *
     * @param folderPath Target folder to be used for move or copy.
     * @param filePaths The list of file paths to move or copy to folderPath.
     * @param capability The capabilities affecting the validation criteria.
     * @param context The context used for retrieving error messages.
     * @return True if folder path and file paths are valid, false otherwise.
     */
    fun checkFolderAndFilePaths(
        folderPath: String,
        filePaths: List<String>,
        capability: OCCapability,
        context: Context
    ): Boolean {
        return checkFolderPath(folderPath, capability, context) && checkFilePaths(filePaths, capability, context)
    }

    fun checkParentRemotePaths(filePaths: List<OCFile>, capability: OCCapability, context: Context): Boolean {
        return filePaths.all {
            if (it.parentRemotePath != StringConstants.SLASH) {
                val parentFolderName = it.parentRemotePath.replace(StringConstants.SLASH, "")
                checkFileName(parentFolderName, capability, context) == null
            } else {
                true
            }
        }
    }

    private fun checkFilePaths(filePaths: List<String>, capability: OCCapability, context: Context): Boolean {
        return filePaths.all { checkFileName(it, capability, context) == null }
    }

    fun checkFolderPath(folderPath: String, capability: OCCapability, context: Context): Boolean {
        return folderPath.split("[/\\\\]".toRegex())
            .none { it.isNotEmpty() && checkFileName(it, capability, context) != null }
    }

    @Suppress("ReturnCount")
    private fun checkInvalidCharacters(name: String, capability: OCCapability, context: Context): String? {
        capability.forbiddenFilenameCharactersJson?.let {
            val forbiddenFilenameCharacters = capability.forbiddenFilenameCharacters()

            val invalidCharacter = forbiddenFilenameCharacters.firstOrNull { name.contains(it) }

            if (invalidCharacter == null) return null

            return context.getString(R.string.file_name_validator_error_invalid_character, invalidCharacter)
        }

        return null
    }

    fun isFileHidden(name: String): Boolean = !TextUtils.isEmpty(name) && name[0] == '.'

    fun isFileNameAlreadyExist(name: String, fileNames: MutableSet<String>): Boolean = fileNames.contains(name)
}
