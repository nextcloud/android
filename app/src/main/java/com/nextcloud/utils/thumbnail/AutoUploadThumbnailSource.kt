/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.thumbnail

import com.nextcloud.client.database.dao.FileSystemDao
import com.nextcloud.client.database.entity.FilesystemEntity
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.model.ServerFileInterface
import java.io.File

object AutoUploadThumbnailSource {
    fun find(remote: OCFile, accountName: String, filesystem: FileSystemDao): File? {
        // Pending entries use logical paths; successful encrypted uploads store the actual DAV path.
        if (remote.remotePath == remote.decryptedRemotePath) return null
        return filesystem.getHandledByRemotePath(remote.remotePath, accountName).firstNotNullOfOrNull { entry ->
            val local = entry.localPath?.let(::File)
            local?.takeIf { matchesEntry(entry, remote) }
                ?.takeIf { it.isFile && it.canRead() && entry.fileModified == it.lastModified() }
                ?.takeIf { matchesSize(it, remote) }
        }?.also {
            Log_OC.d("AutoUploadThumbnailSource", "Using retained auto-upload source: ${remote.fileName}")
        }
    }

    private fun matchesEntry(entry: FilesystemEntity, remote: ServerFileInterface): Boolean =
        entry.remotePath == remote.remotePath && entry.fileSentForUpload == 1 && entry.fileIsFolder == 0

    private fun matchesSize(local: File, remote: ServerFileInterface): Boolean = remote.fileLength == local.length() ||
        remote.fileLength == local.length() + UploadThumbnailSource.ENCRYPTED_FILE_TAG_BYTES
}
