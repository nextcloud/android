/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import com.owncloud.android.lib.common.utils.Log_OC
import java.io.File
import java.io.IOException
import java.nio.file.Files

object FileHelper {
    private const val TAG = "FileHelper"

    fun fetchFiles(folder: File?, offset: Int, limit: Int): List<File> {
        val files = mutableListOf<File>()
        if (folder == null || !folder.exists() || !folder.isDirectory) return files

        val dir = folder.toPath()
        var skipped = 0
        try {
            Files.newDirectoryStream(dir).use { stream ->
                for (entry in stream) {
                    if (skipped < offset) {
                        skipped++
                        continue
                    }
                    files.add(entry.toFile())
                    if (files.size >= limit) break
                }
            }
        } catch (e: IOException) {
            Log_OC.d(TAG, "fetchFiles: $e")
        }
        return files
    }
}
