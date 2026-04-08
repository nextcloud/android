/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.TimeUnit

object FileUtil {
    private const val TAG = "FileUtil"

    @JvmStatic
    fun getCreationTimestamp(file: File): Long? {
        try {
            return Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
                .creationTime()
                .to(TimeUnit.SECONDS)
        } catch (_: IOException) {
            Log_OC.e(
                TAG,
                "failed to read creation timestamp for file: " + file.getName()
            )
            return null
        }
    }

    fun isFolderWritable(folder: File?, lifecycle: Lifecycle, onCompleted: (Boolean) -> Unit) {
        lifecycle.coroutineScope.launch {
            val result = isFolderWritable(folder)
            withContext(Dispatchers.Main) {
                onCompleted(result)
            }
        }
    }

    suspend fun isFolderWritable(folder: File?): Boolean = withContext(Dispatchers.IO) {
        if (folder == null || !folder.isDirectory() || !folder.canWrite()) {
            return@withContext false
        }

        return@withContext try {
            val tempDir = File(folder, ".test_write_dir_${System.currentTimeMillis()}")
            if (!tempDir.mkdir()) return@withContext false

            val tempFile = File(tempDir, "test_file")
            val created = tempFile.createNewFile()

            if (created) tempFile.delete()
            tempDir.delete()

            created
        } catch (_: Exception) {
            false
        }
    }
}
