/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations

import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedMetadata
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class CreateFolderCollisionTest {
    private val operation = CreateFolderOperation("/private/B/", mock(), mock(), mock())

    @Test
    fun concurrentEncryptedFolderCreationRecognizesFolderNamesIgnoringCase() {
        assertTrue(exists("b"))
    }

    @Test
    fun encryptedFolderCreationDoesNotMatchCiphertextAsLogicalName() {
        assertFalse(exists("ciphertext"))
    }

    private fun exists(name: String): Boolean {
        val decrypted: DecryptedMetadata = mock {
            on { folders } doReturn mutableMapOf("ciphertext" to "B")
            on { files } doReturn mutableMapOf()
        }
        val metadata: DecryptedFolderMetadataFile = mock { on { this.metadata } doReturn decrypted }
        val check = CreateFolderOperation::class.java.getDeclaredMethod(
            "isFileExisting",
            DecryptedFolderMetadataFile::class.java,
            String::class.java
        ).apply { isAccessible = true }
        return check.invoke(operation, metadata, name) as Boolean
    }
}
