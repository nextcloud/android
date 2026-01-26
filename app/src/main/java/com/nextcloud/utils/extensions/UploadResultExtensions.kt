/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.owncloud.android.db.UploadResult

fun UploadResult.isNonRetryable(): Boolean = when (this) {
    UploadResult.FILE_NOT_FOUND,
    UploadResult.FILE_ERROR,
    UploadResult.FOLDER_ERROR,
    UploadResult.CANNOT_CREATE_FILE,
    UploadResult.SYNC_CONFLICT,
    UploadResult.LOCAL_STORAGE_NOT_COPIED,
    UploadResult.VIRUS_DETECTED,
    UploadResult.QUOTA_EXCEEDED,
    UploadResult.SAME_FILE_CONFLICT,
    UploadResult.PRIVILEGES_ERROR,
    UploadResult.CREDENTIAL_ERROR,
    UploadResult.UNKNOWN,

    // user's choice
    UploadResult.CANCELLED -> true

    // everything else may succeed after retry
    else -> false
}
