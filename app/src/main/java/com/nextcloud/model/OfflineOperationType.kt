/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.model

sealed class OfflineOperationType {
    abstract val type: String

    data class CreateFolder(override val type: String, var path: String) : OfflineOperationType()

    data class CreateFile(
        override val type: String,
        val localPath: String,
        var remotePath: String,
        var mimeType: String
    ) : OfflineOperationType()

    data class RenameFile(
        override val type: String,
        var ocFileId: Long,
        val newName: String
    ) : OfflineOperationType()

    data class RemoveFile(override val type: String, var path: String) : OfflineOperationType()
}

enum class OfflineOperationRawType {
    CreateFolder,
    CreateFile,
    RenameFile,
    RemoveFile
}
