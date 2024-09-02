/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.model

sealed class OfflineOperationType {
    data class CreateFolder(val path: String) : OfflineOperationType()
    data class CreateFile(val localPath: String, val remotePath: String, val mimeType: String) : OfflineOperationType()
}
