/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.owncloud.android.db.OCUpload

fun List<OCUpload>.getUploadIds(): LongArray = map { it.uploadId }.toLongArray()

fun Array<OCUpload>.getUploadIds(): LongArray = map { it.uploadId }.toLongArray()

fun List<OCUpload>.sortedByUploadOrder(): List<OCUpload> = sortedWith(
    compareBy<OCUpload> { it.fixedUploadStatus }
        .thenByDescending { it.isFixedUploadingNow }
        .thenByDescending { it.fixedUploadEndTimeStamp }
        .thenBy { it.fixedUploadId }
)
