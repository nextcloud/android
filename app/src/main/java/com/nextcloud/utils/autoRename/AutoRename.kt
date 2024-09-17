/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.autoRename

import com.nextcloud.utils.extensions.forbiddenFilenameCharacters
import com.nextcloud.utils.extensions.forbiddenFilenameExtension
import com.owncloud.android.lib.resources.status.OCCapability

object AutoRename {
    private const val REPLACEMENT = "_"

    fun rename(filename: String, capability: OCCapability): String {
        val result = filename.lowercase()

        capability.run {
            forbiddenFilenameCharactersJson?.let {
                val forbiddenFilenameCharacters = capability.forbiddenFilenameCharacters()

                forbiddenFilenameCharacters.forEach {
                    if (result.contains(it)) {
                        return result.replace(it, REPLACEMENT)
                    }
                }
            }

            forbiddenFilenameExtensionJson?.let {
                for (forbiddenExtension in forbiddenFilenameExtension()) {
                    if (filename.endsWith(forbiddenExtension, ignoreCase = true) ||
                        filename.startsWith(forbiddenExtension, ignoreCase = true)) {
                        return filename.replace(forbiddenExtension, REPLACEMENT)
                    }
                }
            }
        }

        return result
    }
}
