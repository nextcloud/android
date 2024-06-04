/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs.upload

import com.owncloud.android.operations.UploadFileOperation

/**
 * Upload transfer trigger.
 */
enum class UploadTrigger(val value: Int) {

    /**
     * Transfer triggered manually by the user.
     */
    USER(UploadFileOperation.CREATED_BY_USER),

    /**
     * Transfer triggered automatically by taking a photo.
     */
    PHOTO(UploadFileOperation.CREATED_AS_INSTANT_PICTURE),

    /**
     * Transfer triggered automatically by making a video.
     */
    VIDEO(UploadFileOperation.CREATED_AS_INSTANT_VIDEO);

    companion object {
        @JvmStatic
        fun fromValue(value: Int) = when (value) {
            UploadFileOperation.CREATED_BY_USER -> USER
            UploadFileOperation.CREATED_AS_INSTANT_PICTURE -> PHOTO
            UploadFileOperation.CREATED_AS_INSTANT_VIDEO -> VIDEO
            else -> USER
        }
    }
}
