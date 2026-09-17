/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.autoUpload

import com.owncloud.android.R

enum class AutoUploadRequestResult {
    STARTED,
    ALREADY_RUNNING,
    NO_ENABLED_FOLDER;

    val messageId: Int
        get() = when (this) {
            STARTED -> R.string.auto_upload_sync_now_started
            ALREADY_RUNNING -> R.string.auto_upload_sync_now_running
            NO_ENABLED_FOLDER -> R.string.auto_upload_sync_now_no_folder
        }
}
