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
import com.nextcloud.utils.extensions.shouldRemoveNonPrintableUnicodeCharactersAndConvertToUTF8
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.status.NextcloudVersion
import com.owncloud.android.lib.resources.status.OCCapability
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
            forbiddenFilenameCharactersJson?.let {
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

            forbiddenFilenameExtensionJson?.let {
                forbiddenFilenameExtension().forEach { forbiddenExtension ->
                    pathSegments.replaceAll { segment ->
                        var modifiedSegment = segment
                        if (forbiddenExtension == StringConstants.SPACE) {
                            modifiedSegment = modifiedSegment.trim()
                        }

                        if (modifiedSegment.endsWith(forbiddenExtension, ignoreCase = true) ||
                            modifiedSegment.startsWith(forbiddenExtension, ignoreCase = true)
                        ) {
                            modifiedSegment = modifiedSegment.replace(forbiddenExtension, REPLACEMENT)
                        }

                        modifiedSegment
                    }
                }
            }
        }

        val result = pathSegments.joinToString(OCFile.PATH_SEPARATOR)
        return if (capability.shouldRemoveNonPrintableUnicodeCharactersAndConvertToUTF8()) {
            val utf8Result = convertToUTF8(result)
            removeNonPrintableUnicodeCharacters(utf8Result)
        } else {
            result
        }
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
