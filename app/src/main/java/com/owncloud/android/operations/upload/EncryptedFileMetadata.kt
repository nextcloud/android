/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.upload

import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFileV1
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.utils.MimeType

class EncryptedFileMetadata(
    val remoteName: String,
    val filename: String,
    val mimeType: String,
    val key: String,
    val nonce: String
) {
    companion object {
        @JvmStatic
        fun find(metadata: Any?, filename: String): EncryptedFileMetadata? = when (metadata) {
            is DecryptedFolderMetadataFileV1 -> metadata.files.entries.firstOrNull {
                it.value.encrypted.filename.equals(filename, ignoreCase = true) &&
                    it.value.encrypted.mimetype != MimeType.WEBDAV_FOLDER
            }?.let { (name, file) ->
                EncryptedFileMetadata(
                    name,
                    file.encrypted.filename,
                    file.encrypted.mimetype,
                    file.encrypted.key,
                    file.initializationVector
                )
            }

            is DecryptedFolderMetadataFile -> metadata.metadata.files.entries.firstOrNull {
                it.value.filename.equals(filename, ignoreCase = true)
            }?.let { (name, file) ->
                EncryptedFileMetadata(name, file.filename, file.mimetype, file.key, file.nonce)
            }

            else -> null
        }
    }
}
