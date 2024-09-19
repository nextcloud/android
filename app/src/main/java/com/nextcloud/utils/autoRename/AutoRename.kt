/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.autoRename

import com.nextcloud.utils.extensions.StringConstants
import com.nextcloud.utils.extensions.forbiddenFilenameCharacters
import com.nextcloud.utils.extensions.forbiddenFilenameExtension
import com.nextcloud.utils.extensions.shouldRemoveNonPrintableUnicodeCharacters
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.status.OCCapability
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.regex.Pattern

object AutoRename {
    private const val REPLACEMENT = "_"

    fun rename(filename: String, capability: OCCapability, isFolderPath: Boolean = false): String {
        val pathSegments = filename.split(OCFile.PATH_SEPARATOR).toMutableList()

        capability.run {
            forbiddenFilenameCharactersJson?.let {
                var forbiddenFilenameCharacters = capability.forbiddenFilenameCharacters()
                if (isFolderPath) {
                    forbiddenFilenameCharacters = forbiddenFilenameCharacters.minus(OCFile.PATH_SEPARATOR)
                }

                pathSegments.replaceAll { segment ->
                    var modifiedSegment = segment
                    forbiddenFilenameCharacters.forEach { forbiddenChar ->
                        if (modifiedSegment.contains(forbiddenChar)) {
                            modifiedSegment = modifiedSegment.replace(forbiddenChar, REPLACEMENT)
                        }
                    }
                    modifiedSegment
                }
            }

            forbiddenFilenameExtensionJson?.let {
                forbiddenFilenameExtension().any { forbiddenExtension ->
                    pathSegments.replaceAll { segment ->
                        var modifiedSegment = segment
                        if (forbiddenExtension == StringConstants.SPACE) {
                            modifiedSegment = modifiedSegment.trimStart().trimEnd()
                        }

                        if (modifiedSegment.endsWith(forbiddenExtension, ignoreCase = true) ||
                            modifiedSegment.startsWith(forbiddenExtension, ignoreCase = true)
                        ) {
                            modifiedSegment = modifiedSegment.replace(forbiddenExtension, REPLACEMENT)
                        }

                        modifiedSegment
                    }
                    false
                }
            }
        }

        val result = pathSegments.joinToString(OCFile.PATH_SEPARATOR)

        return if (capability.shouldRemoveNonPrintableUnicodeCharacters()) {
            val utf8Result = convertToUTF8(result)
            removeNonPrintableUnicodeCharacters(utf8Result)
        } else {
            result
        }
    }

    fun renameFile(file: File, capability: OCCapability): File {
        if (!file.exists()) {
            return file
        }

        val newFilename = rename(file.name, capability)
        val newFile = File(file.parentFile, newFilename)
        FileUtils.moveFile(file, newFile)
        return newFile
    }

    private fun convertToUTF8(filename: String): String {
        return String(filename.toByteArray(), Charsets.UTF_8)
    }

    private fun removeNonPrintableUnicodeCharacters(filename: String): String {
        val regex = "\\p{C}"
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(filename)
        return matcher.replaceAll("")
    }
}
