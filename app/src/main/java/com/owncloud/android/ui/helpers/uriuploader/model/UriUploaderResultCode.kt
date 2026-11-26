/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.helpers.uriuploader.model

enum class UriUploaderResultCode {
    OK,
    ERROR_UNKNOWN,
    ERROR_NO_FILE_TO_UPLOAD,
    ERROR_READ_PERMISSION_NOT_GRANTED,
    ERROR_SENSITIVE_PATH
}
