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
import com.nextcloud.utils.extensions.forbiddenFilenameExtensions
import com.nextcloud.utils.extensions.forbiddenFilenames
import com.nextcloud.utils.extensions.removeFileExtension
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.status.NextcloudVersion
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
    @Suppress("ReturnCount", "NestedBlockDepth")
    fun checkFileName(
        filename: String,
        capability: OCCapability,
        context: Context,
        existedFileNames: Set<String>? = null
    ): String? {
        if (filename.isBlank()) {
            return context.getString(R.string.filename_empty)
        }

        existedFileNames?.let {
            if (isFileNameAlreadyExist(filename, existedFileNames)) {
                return context.getString(R.string.file_already_exists)
            }
        }

        if (!capability.version.isNewerOrEqual(NextcloudVersion.nextcloud_30)) {
            return null
        }

        checkInvalidCharacters(filename, capability, context)?.let { return it }

        val filenameVariants = setOf(filename.lowercase(), filename.removeFileExtension().lowercase())

        with(capability) {
            forbiddenFilenameBaseNamesJson?.let {
                forbiddenFilenameBaseNames().find { it.lowercase() in filenameVariants }?.let { forbiddenBaseFilename ->
                    return context.getString(R.string.file_name_validator_error_reserved_names, forbiddenBaseFilename)
                }
            }

            forbiddenFilenamesJson?.let {
                forbiddenFilenames().find { it.lowercase() in filenameVariants }?.let { forbiddenFilename ->
                    return context.getString(R.string.file_name_validator_error_reserved_names, forbiddenFilename)
                }
            }

            forbiddenFilenameExtensionJson?.let {
                forbiddenFilenameExtensions().find { extension ->
                    when {
                        extension == StringConstants.SPACE ->
                            filename.startsWith(extension, ignoreCase = true) ||
                                filename.endsWith(extension, ignoreCase = true)

                        else -> filename.endsWith(extension, ignoreCase = true)
                    }
                }?.let { forbiddenExtension ->
                    return if (forbiddenExtension == StringConstants.SPACE) {
                        context.getString(R.string.file_name_validator_error_forbidden_space_character_extensions)
                    } else {
                        context.getString(
                            R.string.file_name_validator_error_forbidden_file_extensions,
                            forbiddenExtension
                        )
                    }
                }
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
    ): Boolean = checkFolderPath(folderPath, capability, context) && checkFilePaths(filePaths, capability, context)

    fun checkParentRemotePaths(filePaths: List<OCFile>, capability: OCCapability, context: Context): Boolean =
        filePaths.all {
            if (it.parentRemotePath != StringConstants.SLASH) {
                val parentFolderName = it.parentRemotePath.replace(StringConstants.SLASH, "")
                checkFileName(parentFolderName, capability, context) == null
            } else {
                true
            }
        }

    private fun checkFilePaths(filePaths: List<String>, capability: OCCapability, context: Context): Boolean =
        filePaths.all {
            checkFileName(it, capability, context) == null
        }

    fun checkFolderPath(folderPath: String, capability: OCCapability, context: Context): Boolean =
        folderPath.split("[/\\\\]".toRegex())
            .none { it.isNotEmpty() && checkFileName(it, capability, context) != null }

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

    fun isFileNameAlreadyExist(name: String, fileNames: Set<String>): Boolean = fileNames.contains(name)
}
