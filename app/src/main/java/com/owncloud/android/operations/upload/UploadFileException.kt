/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.upload

sealed class UploadFileException(message: String) : Exception(message) {
    class EmptyOrNullFilePath: UploadFileException("Empty or null file path")
    class MissingPermission : UploadFileException("Missing storage permission")
}
