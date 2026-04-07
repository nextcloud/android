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
            val testFile = File.createTempFile(".test_write_", null, folder)
            testFile.delete()
            true
        } catch (_: Exception) {
            false
        }
    }
}
