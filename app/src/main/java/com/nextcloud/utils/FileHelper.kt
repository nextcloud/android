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
import kotlin.io.path.pathString

@Suppress("NestedBlockDepth")
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

    fun fetchFolders(folder: File?, offset: Int, limit: Int): List<File> {
        val folders = mutableListOf<File>()
        if (folder == null || !folder.exists() || !folder.isDirectory) return folders

        val dir = folder.toPath()
        var skipped = 0
        try {
            Files.newDirectoryStream(dir).use { stream ->
                for (entry in stream) {
                    if (!entry.toFile().isDirectory) continue
                    if (skipped < offset) {
                        skipped++
                        continue
                    }
                    folders.add(entry.toFile())
                    if (folders.size >= limit) break
                }
            }
        } catch (e: IOException) {
            Log_OC.d(TAG, "fetchFolders: $e")
        }
        return folders
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
