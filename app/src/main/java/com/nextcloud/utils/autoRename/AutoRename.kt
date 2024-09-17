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
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.status.OCCapability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

object AutoRename {
    private const val REPLACEMENT = "_"
    private const val TAG = "AutoRename"
    private val scope = CoroutineScope(Dispatchers.IO)

    fun rename(filename: String, capability: OCCapability): String {
        capability.run {
            forbiddenFilenameCharactersJson?.let {
                val forbiddenFilenameCharacters = capability.forbiddenFilenameCharacters()

                forbiddenFilenameCharacters.forEach {
                    if (filename.lowercase().contains(it)) {
                        return filename.replace(it, REPLACEMENT)
                    }
                }
            }

            forbiddenFilenameExtensionJson?.let {
                for (forbiddenExtension in forbiddenFilenameExtension()) {
                    return if (forbiddenExtension == StringConstants.SPACE &&
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
                }
            }
        }

        return filename
    }

    fun renameFiles(paths: Array<String>, capability: OCCapability, onComplete: (Array<String>) -> Unit) {
        scope.launch {
           val result = paths.map { path ->
                val file = File(path)

                if (file.exists()) {
                    val newFilename = rename(file.name, capability)
                    val newFile = File(file.parentFile, newFilename)

                    if (file.renameTo(newFile)) {
                        try {
                            newFile.path
                        } catch (e: IOException) {
                            Log_OC.e(TAG, "Exception caught during renameFiles(): $e")
                            path
                        }
                    } else {
                        path
                    }
                } else {
                    path
                }
            }.toTypedArray()

            launch(Dispatchers.Main) {
                onComplete(result)
            }
        }
    }
}
