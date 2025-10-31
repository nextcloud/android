/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.DisplayUtils
import java.io.File

private const val TAG = "FileExtensions"

fun OCFile?.logFileSize(tag: String) {
    val size = DisplayUtils.bytesToHumanReadable(this?.fileLength ?: -1)
    val rawByte = this?.fileLength ?: -1
    Log_OC.d(tag, "onSaveInstanceState: $size, raw byte $rawByte")
}

fun File?.logFileSize(tag: String) {
    val size = DisplayUtils.bytesToHumanReadable(this?.length() ?: -1)
    val rawByte = this?.length() ?: -1
    Log_OC.d(tag, "onSaveInstanceState: $size, raw byte $rawByte")
}

/**
 * Converts a non-null and non-empty [String] path into a [File] object, if it exists.
 *
 * @receiver String path to a file.
 * @return [File] instance if the file exists, or `null` if the path is null, empty, or non-existent.
 */
@Suppress("ReturnCount")
fun String.toFile(): File? {
    if (isNullOrEmpty()) {
        Log_OC.e(TAG, "given path is null or empty")
        return null
    }

    val file = File(this)
    if (!file.exists()) {
        Log_OC.e(TAG, "File does not exist: $this")
        return null
    }

    return file
}
