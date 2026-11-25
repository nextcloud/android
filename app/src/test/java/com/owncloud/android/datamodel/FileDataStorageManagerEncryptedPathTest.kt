/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.datamodel

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class FileDataStorageManagerEncryptedPathTest {
    private val storage: FileDataStorageManager = mock()

    @Before
    fun setUp() {
        whenever(storage.retrieveRemotePathConsideringEncryption(any())).thenCallRealMethod()
    }

    @Test
    fun ciphertextIdentityTakesPriorityOverDecryptedPath() {
        val file = OCFile("/private/ciphertext").apply {
            isEncrypted = true
            decryptedRemotePath = "/private/photo.jpg"
        }
        whenever(storage.getFileByEncryptedRemotePath(file.remotePath)).thenReturn(file)

        assertEquals(file.remotePath, storage.retrieveRemotePathConsideringEncryption(file))
        verify(storage, never()).getEncryptedRemotePath(any())
    }

    @Test
    fun logicalUploadResolvesCiphertextFromDecryptedPath() {
        val file = OCFile("/private/photo.jpg").apply {
            isEncrypted = true
            decryptedRemotePath = "/private/photo.jpg"
        }
        whenever(storage.getEncryptedRemotePath("/private/photo.jpg")).thenReturn("/private/ciphertext")

        assertEquals("/private/ciphertext", storage.retrieveRemotePathConsideringEncryption(file))
    }

    @Test
    fun unencryptedFileKeepsItsRemotePath() {
        val file = OCFile("/photo.jpg")

        assertEquals(file.remotePath, storage.retrieveRemotePathConsideringEncryption(file))
        verify(storage, never()).getFileByEncryptedRemotePath(any())
    }
}
