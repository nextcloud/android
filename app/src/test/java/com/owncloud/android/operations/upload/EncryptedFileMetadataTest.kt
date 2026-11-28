/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.upload

import com.owncloud.android.datamodel.e2e.v1.decrypted.Data
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFileV1
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedMetadata
import com.owncloud.android.utils.MimeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFile as DecryptedFileV1

class EncryptedFileMetadataTest {
    @Test
    fun v1ResolvesCiphertextIdentityAndPlaintextAttributes() {
        val metadata = DecryptedFolderMetadataFileV1().apply {
            setFiles(
                mapOf(
                    "cipher" to DecryptedFileV1().apply {
                        encrypted = Data().apply {
                            filename = "photo.jpg"
                            mimetype = "image/jpeg"
                            key = "key"
                        }
                        initializationVector = "nonce"
                    }
                )
            )
        }

        val file = requireNotNull(EncryptedFileMetadata.find(metadata, "PHOTO.jpg"))
        assertEquals("cipher", file.remoteName)
        assertEquals("photo.jpg", file.filename)
        assertEquals("image/jpeg", file.mimeType)
        assertEquals("key", file.key)
        assertEquals("nonce", file.nonce)
        assertNull(EncryptedFileMetadata.find(metadata, "other.jpg"))
        metadata.files["cipher"]!!.encrypted.mimetype = MimeType.WEBDAV_FOLDER
        assertNull(EncryptedFileMetadata.find(metadata, "photo.jpg"))
    }

    @Test
    fun v2ResolvesFilesAndDoesNotTreatFolderAsFile() {
        val metadata = DecryptedFolderMetadataFile(
            metadata = DecryptedMetadata(
                files = mutableMapOf("cipher" to DecryptedFile("photo.jpg", "image/jpeg", "nonce", "tag", "key")),
                folders = mutableMapOf("cipher-folder" to "photos"),
                metadataKey = ByteArray(0)
            ),
            version = "2.0"
        )

        val file = requireNotNull(EncryptedFileMetadata.find(metadata, "photo.jpg"))
        assertEquals("cipher", file.remoteName)
        assertEquals("image/jpeg", file.mimeType)
        assertEquals("key", file.key)
        assertEquals("nonce", file.nonce)
        assertNull(EncryptedFileMetadata.find(metadata, "photos"))
        assertNull(EncryptedFileMetadata.find(null, "photo.jpg"))
    }
}
