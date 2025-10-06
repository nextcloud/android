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
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.io.path.pathString

@Suppress("NestedBlockDepth")
object FileHelper {
    private const val TAG = "FileHelper"

    fun listDirectoryEntries(
        directory: File?,
        startIndex: Int,
        maxItems: Int,
        fetchFolders: Boolean
    ): List<File> {
        if (directory == null || !directory.exists() || !directory.isDirectory) return emptyList()

        return try {
            val allEntries = Files.list(directory.toPath())
                .map { it.toFile() }
                .collect(Collectors.toList())

            val result = mutableListOf<File>()
            var skipped = 0

            for (file in allEntries) {
                val shouldInclude = if (fetchFolders) file.isDirectory else true

                if (shouldInclude) {
                    if (skipped < startIndex) {
                        skipped++
                        continue
                    }
                    result.add(file)
                    if (result.size >= maxItems) break
                }
            }

            result
        } catch (e: IOException) {
            Log_OC.d(TAG, "listDirectoryEntries: $e")
            emptyList()
        }
    }

    fun listFilesRecursive(files: Collection<File>): List<String> {
        val result = mutableListOf<String>()

        for (file in files) {
            try {
                collectFilesRecursively(file.toPath(), result)
            } catch (e: IOException) {
                Log_OC.e(TAG, "Error collecting files recursively from: ${file.absolutePath}", e)
            }
        }

        return result
    }

    private fun collectFilesRecursively(path: Path, result: MutableList<String>) {
        if (Files.isDirectory(path)) {
            try {
                Files.newDirectoryStream(path).use { stream ->
                    for (entry in stream) {
                        collectFilesRecursively(entry, result)
                    }
                }
            } catch (e: IOException) {
                Log_OC.e(TAG, "Error reading directory: ${path.pathString}", e)
            }
        } else {
            result.add(path.pathString)
        }
    }
}
