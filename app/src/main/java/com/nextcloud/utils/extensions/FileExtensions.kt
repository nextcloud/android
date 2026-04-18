/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import androidx.exifinterface.media.ExifInterface
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.DisplayUtils
import java.io.File
import java.nio.file.Path

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

fun Path.toLocalPath(): String = toAbsolutePath().toString()

/**
 * Converts a non-null and non-empty [String] path into a [File] object, if it exists.
 *
 * @receiver String path to a file.
 * @return [File] instance if the file exists, or `null` if the path is null, empty, or non-existent.
 */
@Suppress("ReturnCount")
fun String?.toFile(): File? {
    if (isNullOrEmpty()) {
        Log_OC.w(TAG, "given path is null or empty: $this")
        return null
    }

    val file = File(this)
    if (!file.exists()) {
        Log_OC.e(TAG, "File does not exist: $this")
        return null
    }

    return file
}

fun String.getExifSize(): Pair<Int, Int>? = try {
    val exif = ExifInterface(this)
    var w = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
    var h = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)

    val orientation = exif.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    )
    if (orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
        orientation == ExifInterface.ORIENTATION_ROTATE_270
    ) {
        val tmp = w
        w = h
        h = tmp
    }

    Log_OC.d(TAG, "Using exif imageDimension: $w x $h")
    if (w > 0 && h > 0) w to h else null
} catch (_: Exception) {
    null
}

fun String.getBitmapSize(): Pair<Int, Int>? = try {
    val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    android.graphics.BitmapFactory.decodeFile(this, options)
    val w = options.outWidth
    val h = options.outHeight

    Log_OC.d(TAG, "Using bitmap factory imageDimension: $w x $h")
    if (w > 0 && h > 0) w to h else null
} catch (_: Exception) {
    null
}
