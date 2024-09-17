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
import com.owncloud.android.lib.resources.status.OCCapability
import java.util.regex.Pattern

object AutoRename {
    private const val REPLACEMENT = "_"

    fun rename(filename: String, capability: OCCapability): String {
        capability.run {
            forbiddenFilenameCharactersJson?.let {
                val forbiddenFilenameCharacters = capability.forbiddenFilenameCharacters()

                forbiddenFilenameCharacters.forEach {
                    if (filename.lowercase().contains(it)) {
                        val result = filename.replace(it, REPLACEMENT)
                        return if (shouldRemoveNonPrintableUnicodeCharacters()) {
                            removeNonPrintableUnicodeCharacters(result)
                        } else {
                            result
                        }
                    }
                }
            }

            forbiddenFilenameExtensionJson?.let {
                for (forbiddenExtension in forbiddenFilenameExtension()) {
                    val result = if (forbiddenExtension == StringConstants.SPACE &&
                        filename.endsWith(forbiddenExtension, ignoreCase = true)) {
                        filename.trimEnd()
                    } else if (forbiddenExtension == StringConstants.SPACE &&
                        filename.startsWith(forbiddenExtension, ignoreCase = true)) {
                        filename.trimStart()
                    } else if (filename.endsWith(forbiddenExtension, ignoreCase = true) ||
                        filename.startsWith(forbiddenExtension, ignoreCase = true)) {
                        filename.replace(forbiddenExtension, REPLACEMENT)
                    } else {
                        filename
                    }

                    return if (shouldRemoveNonPrintableUnicodeCharacters()) {
                        removeNonPrintableUnicodeCharacters(result)
                    } else {
                        result
                    }
                }
            }
        }

        return if (capability.shouldRemoveNonPrintableUnicodeCharacters()) {
            removeNonPrintableUnicodeCharacters(filename)
        } else {
            filename
        }
    }

    fun removeNonPrintableUnicodeCharacters(filename: String): String {
        val regex = "\\p{C}"
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(filename)
        return matcher.replaceAll("")
    }
}
