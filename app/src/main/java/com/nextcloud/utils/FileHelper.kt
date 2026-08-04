/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.DirectoryIteratorException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.pathString

@Suppress("NestedBlockDepth")
object FileHelper {
    private const val TAG = "FileHelper"

    suspend fun forEachDirectoryPage(
        directory: File?,
        pageSize: Int,
        fetchFolders: Boolean,
        onPage: suspend (List<File>) -> Unit
    ) {
        if (directory == null || !directory.isDirectory || pageSize <= 0) {
            return
        }

        try {
            withContext(Dispatchers.IO) {
                Files.newDirectoryStream(directory.toPath())
            }.use { entries ->
                var page = ArrayList<File>(pageSize)

                for (entry in entries) {
                    val file = entry.toFile()
                    if (file.isDirectory != fetchFolders) {
                        continue
                    }

                    page.add(file)
                    if (page.size < pageSize) {
                        continue
                    }

                    onPage(page)
                    page = ArrayList(pageSize)
                }

                if (page.isNotEmpty()) {
                    onPage(page)
                }
            }
        } catch (e: IOException) {
            Log_OC.d(TAG, "forEachDirectoryPage failed: $e")
        } catch (e: DirectoryIteratorException) {
            Log_OC.d(TAG, "forEachDirectoryPage failed: $e")
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
