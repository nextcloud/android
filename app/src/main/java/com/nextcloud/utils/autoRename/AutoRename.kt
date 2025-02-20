/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.autoRename

import com.nextcloud.utils.extensions.StringConstants
import com.nextcloud.utils.extensions.forbiddenFilenameCharacters
import com.nextcloud.utils.extensions.forbiddenFilenameExtensions
import com.nextcloud.utils.extensions.shouldRemoveNonPrintableUnicodeCharactersAndConvertToUTF8
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.status.NextcloudVersion
import com.owncloud.android.lib.resources.status.OCCapability
import org.apache.commons.io.FilenameUtils
import java.util.regex.Pattern

object AutoRename {
    private const val REPLACEMENT = "_"

    @Suppress("NestedBlockDepth")
    fun rename(filename: String, capability: OCCapability, isFolderPath: Boolean = false): String {
        if (!capability.version.isNewerOrEqual(NextcloudVersion.nextcloud_30)) {
            return filename
        }

        val pathSegments = filename.split(OCFile.PATH_SEPARATOR).toMutableList()

        capability.run {
            if (forbiddenFilenameCharactersJson != null) {
                var forbiddenFilenameCharacters = capability.forbiddenFilenameCharacters()

                if (isFolderPath) {
                    forbiddenFilenameCharacters = forbiddenFilenameCharacters.filter { it != OCFile.PATH_SEPARATOR }
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

            if (forbiddenFilenameExtensionJson != null) {
                val forbiddenFilenameExtensions = forbiddenFilenameExtensions()

                forbiddenFilenameExtensions.find { it == StringConstants.SPACE }?.let {
                    pathSegments.replaceAll { segment ->
                        segment.trim()
                    }
                }

                forbiddenFilenameExtensions.find { it == StringConstants.DOT }?.let { forbiddenExtension ->
                    pathSegments.replaceAll { segment ->
                        replaceDots(forbiddenExtension, segment)
                    }
                }

                forbiddenFilenameExtensions
                    .filter { it != StringConstants.SPACE && it != StringConstants.DOT }
                    .forEach { forbiddenExtension ->
                        pathSegments.replaceAll { segment ->
                            replaceFileExtensions(forbiddenExtension, segment)
                        }
                    }
            }
        }

        val filenameWithExtension = pathSegments.joinToString(OCFile.PATH_SEPARATOR)
        val result = if (isFolderPath) filenameWithExtension else lowercaseFileExtension(filenameWithExtension)

        return if (capability.shouldRemoveNonPrintableUnicodeCharactersAndConvertToUTF8()) {
            val utf8Result = convertToUTF8(result)
            removeNonPrintableUnicodeCharacters(utf8Result)
        } else {
            result
        }
    }

    private fun lowercaseFileExtension(filename: String): String {
        val extension = FilenameUtils.getExtension(filename).lowercase()
        val fileNameWithoutExtension = FilenameUtils.removeExtension(filename)
        return fileNameWithoutExtension + extension
    }

    private fun replaceDots(forbiddenExtension: String, segment: String): String {
        return if (isSegmentContainsForbiddenExtension(forbiddenExtension, segment)) {
            segment.replaceFirst(forbiddenExtension, REPLACEMENT)
        } else {
            segment
        }
    }

    private fun replaceFileExtensions(forbiddenExtension: String, segment: String): String {
        return if (isSegmentContainsForbiddenExtension(forbiddenExtension, segment)) {
            val newExtension = forbiddenExtension.replace(StringConstants.DOT, REPLACEMENT, ignoreCase = true)
            segment.replace(forbiddenExtension, newExtension.lowercase(), ignoreCase = true)
        } else {
            segment
        }
    }

    private fun isSegmentContainsForbiddenExtension(forbiddenExtension: String, segment: String): Boolean {
        return segment.endsWith(forbiddenExtension, ignoreCase = true) ||
            segment.startsWith(forbiddenExtension, ignoreCase = true)
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
