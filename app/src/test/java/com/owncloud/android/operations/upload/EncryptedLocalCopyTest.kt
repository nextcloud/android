/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.upload

import com.owncloud.android.datamodel.OCFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class EncryptedLocalCopyTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun repairingRawCiphertextRecordDeletesOnlyItsDefaultAccountCopy() {
        val root = temporaryFolder.newFolder()
        val raw = File(root, "cipher").apply { writeText("ciphertext") }
        val previous = OCFile("/cipher").apply { storagePath = raw.absolutePath }
        val refreshed = OCFile("/cipher").apply { isEncrypted = true }

        EncryptedLocalCopy.discardRawCopy(previous, refreshed, root)

        assertFalse(raw.exists())
        assertNull(refreshed.storagePath)
    }

    @Test
    fun repairingRecordPreservesExternalSourceAndClearsUnverifiedAssociation() {
        val root = temporaryFolder.newFolder()
        val source = temporaryFolder.newFile().apply { writeText("source") }
        val previous = OCFile("/cipher").apply { storagePath = source.absolutePath }
        val refreshed = OCFile("/cipher").apply {
            isEncrypted = true
            storagePath = source.absolutePath
        }

        EncryptedLocalCopy.discardRawCopy(previous, refreshed, root)

        assertTrue(source.exists())
        assertNull(refreshed.storagePath)
    }

    @Test
    fun validEncryptedLocalCopyIsPreserved() {
        val root = temporaryFolder.newFolder()
        val local = File(root, "photo.jpg").apply { writeText("plaintext") }
        val previous = OCFile("/cipher").apply { isEncrypted = true }
        val refreshed = OCFile("/cipher").apply {
            isEncrypted = true
            storagePath = local.absolutePath
        }

        EncryptedLocalCopy.discardRawCopy(previous, refreshed, root)

        assertTrue(local.exists())
        assertTrue(refreshed.storagePath == local.absolutePath)
    }
}
