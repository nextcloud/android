/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import com.nextcloud.utils.extensions.StringConstants
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.TimeUnit

object FileUtil {
    /**
     * returns the file name of a given path.
     *
     * @param filePath (absolute) file path
     * @return the filename including its file extension, `empty String` for invalid input values
     */
    fun getFilenameFromPathString(filePath: String?): String = if (!filePath.isNullOrBlank()) {
        val file = File(filePath)
        if (file.isFile()) {
            file.getName()
        } else {
            ""
        }
    } else {
        ""
    }

    @JvmStatic
    fun getCreationTimestamp(file: File): Long? {
        try {
            return Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
                .creationTime()
                .to(TimeUnit.SECONDS)
        } catch (e: IOException) {
            Log_OC.e(
                UploadFileRemoteOperation::class.java.getSimpleName(),
                "Failed to read creation timestamp for file: " + file.getName()
            )
            return null
        }
    }

    /**
     * Returns remote path variants (lowercase and uppercase extension) for the given path.
     *
     * Example:
     * ```
     * If you pass "/TesTFolder/abc.JPG", it will return:
     * "/TesTFolder/abc.jpg" and "/TesTFolder/abc.JPG"
     * ```
     */
    fun getRemotePathVariants(path: String): Pair<String, String> {
        val lastDot = path.lastIndexOf(StringConstants.DOT)
        if (lastDot == -1 || lastDot == path.length - 1) {
            return Pair(path, path)
        }

        val base = path.substring(0, lastDot)
        val ext = path.substring(lastDot + 1)

        val lower = "$base.${ext.lowercase()}"
        val upper = "$base.${ext.uppercase()}"

        return Pair(lower, upper)
    }
}
