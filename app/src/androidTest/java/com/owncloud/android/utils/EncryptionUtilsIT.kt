/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import com.owncloud.android.EncryptionIT
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.e2e.v1.decrypted.Data
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFile
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFileV1
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedMetadata
import com.owncloud.android.lib.resources.e2ee.CsrHelper
import com.owncloud.android.operations.RefreshFolderOperation
import org.junit.Assert.assertEquals
import org.junit.Test

class EncryptionUtilsIT : EncryptionIT() {
    @Throws(
        java.security.NoSuchAlgorithmException::class,
        java.io.IOException::class,
        org.bouncycastle.operator.OperatorCreationException::class
    )
    @Test
    fun saveAndRestorePublicKey() {
        val arbitraryDataProvider = ArbitraryDataProviderImpl(targetContext)
        val keyPair = EncryptionUtils.generateKeyPair()
        val e2eUser = "e2e-user"
        val key = CsrHelper().generateCsrPemEncodedString(keyPair, e2eUser)

        EncryptionUtils.savePublicKey(user, key, e2eUser, arbitraryDataProvider)

        assertEquals(key, EncryptionUtils.getPublicKey(user, e2eUser, arbitraryDataProvider))
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateFileNameForEncryptedFileV1() {
        val folder = testFolder()

        val decryptedFilename = "image.png"
        val mockEncryptedFilename = "encrypted_file_name.png"

        val decryptedMetadata = DecryptedMetadata()
        val filesData = DecryptedFile().apply {
            encrypted = Data().apply {
                filename = decryptedFilename
            }
        }
        val files = mapOf(mockEncryptedFilename to filesData)
        val metadata = DecryptedFolderMetadataFileV1(decryptedMetadata, files)

        RefreshFolderOperation.updateFileNameForEncryptedFileV1(storageManager, metadata, folder)
        assertEquals(folder.decryptedRemotePath.contains("null"), false)
    }
}
