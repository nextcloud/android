/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.utils

import android.os.Environment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.UUID
import java.util.concurrent.TimeUnit

object FileUtil {
    private const val TAG = "FileUtil"
    private const val WRITE_PROBE_PREFIX = ".nc_write_probe_"
    private const val WRITE_PROBE_FILE_NAME = "probe"

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
        if (folder == null) {
            Log_OC.w(TAG, "isFolderWritable: folder is null")
            return@withContext false
        }

        val directory = try {
            folder.toPath()
        } catch (e: InvalidPathException) {
            Log_OC.w(TAG, "isFolderWritable: invalid path: ${folder.path}: $e")
            return@withContext false
        }

        return@withContext try {
            isUsableDirectory(directory) && canCreateAndDeleteIn(directory)
        } catch (e: Exception) {
            Log_OC.e(TAG, "isFolderWritable: $e")
            false
        }
    }

    private fun isUsableDirectory(directory: Path): Boolean {
        if (!Files.exists(directory)) {
            Log_OC.w(TAG, "isFolderWritable: existence cannot be verified: $directory")
            return false
        }

        if (!Files.isDirectory(directory)) {
            Log_OC.w(TAG, "isFolderWritable: not a directory: $directory")
            return false
        }

        val storageState = Environment.getExternalStorageState(directory.toFile())
        if (storageState != Environment.MEDIA_MOUNTED && storageState != Environment.MEDIA_UNKNOWN) {
            Log_OC.w(TAG, "isFolderWritable: storage is $storageState: $directory")
            return false
        }

        if (!Files.isWritable(directory)) {
            Log_OC.w(TAG, "isFolderWritable: no write access: $directory")
            return false
        }

        Log_OC.i(TAG, "isFolderWritable: usable directory")

        return true
    }

    private fun canCreateAndDeleteIn(directory: Path): Boolean {
        val probeDirectory = directory.resolve("$WRITE_PROBE_PREFIX${UUID.randomUUID()}")
        val probeFile = probeDirectory.resolve(WRITE_PROBE_FILE_NAME)

        val writable =
            runCatchingLogged("create directory", probeDirectory) { Files.createDirectory(probeDirectory) } &&
                runCatchingLogged("create file", probeFile) { Files.createFile(probeFile) } &&
                runCatchingLogged("delete file", probeFile) { Files.delete(probeFile) } &&
                runCatchingLogged("delete directory", probeDirectory) { Files.delete(probeDirectory) }

        if (writable) {
            Log_OC.d(TAG, "isFolderWritable: writable: $directory")
        } else {
            removeLeftovers(probeFile, probeDirectory)
        }

        Log_OC.i(TAG, "isFolderWritable: writable: $writable")

        return writable
    }

    private fun removeLeftovers(vararg paths: Path) {
        paths.forEach { path ->
            runCatchingLogged("delete if exists", path) {
                Files.deleteIfExists(path)
            }
        }
    }

    private inline fun runCatchingLogged(step: String, target: Path, action: () -> Unit): Boolean = try {
        action()
        true
    } catch (e: Exception) {
        Log_OC.e(TAG, "isFolderWritable: cannot $step $target: $e")
        false
    }
}
