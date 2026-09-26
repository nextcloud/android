/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.worker

import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.FileStorageUtils
import java.io.File

@Suppress("ReturnCount")
object WorkerFilesPayload {
    private const val TAG = "WorkerFilesPayload"
    private const val FILE_PREFIX = "worker_files_payload_"
    private const val FILE_SUFFIX = ".tmp"
    private const val SEPARATOR = ","

    fun write(files: List<OCFile>): String? {
        val context = MainApp.getAppContext() ?: return null
        if (files.isEmpty()) return null

        val dir = File(FileStorageUtils.getAppTempDirectoryPath(context)).also {
            if (!it.exists() && !it.mkdirs()) {
                Log_OC.e(TAG, "Failed to create temp directory: ${it.absolutePath}")
                return null
            }
        }

        val file = File(dir, "$FILE_PREFIX${System.currentTimeMillis()}$FILE_SUFFIX")
        return runCatching {
            file.writeText(files.joinToString(SEPARATOR) { it.fileId.toString() })
            file.absolutePath
        }.onFailure {
            Log_OC.e(TAG, "Failed to write payload file", it)
        }.getOrNull()
    }

    fun read(path: String?): List<Long> {
        if (path.isNullOrBlank()) return listOf()

        val file = File(path)
        if (!file.exists()) {
            Log_OC.e(TAG, "Payload file not found: $path")
            return listOf()
        }

        val ids = runCatching {
            file.readText()
                .split(SEPARATOR)
                .mapNotNull { it.toLongOrNull() }
        }.onFailure {
            Log_OC.e(TAG, "Failed to read payload file", it)
        }.getOrNull() ?: return listOf()

        return ids
    }

    fun cleanup(path: String?) {
        if (path.isNullOrBlank()) return
        val deleted = File(path).delete()
        if (!deleted) Log_OC.w(TAG, "Failed to delete payload file: $path")
    }
}
