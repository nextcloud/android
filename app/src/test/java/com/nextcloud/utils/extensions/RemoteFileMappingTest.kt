/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.MimeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@Suppress("MagicNumber")
class RemoteFileMappingTest {
    @Test
    fun fileMappingPreservesIdentityPropertiesAndLockMetadata() {
        val remote = RemoteFile("/document.txt").apply {
            mimeType = "text/plain"
            length = 23
            etag = "version"
            remoteId = "remote-id"
            isLocked = true
            lockOwner = "editor"
            lockToken = "token"
        }

        val file = remote.toOCFile()

        assertEquals(remote.remotePath, file.remotePath)
        assertEquals(remote.remotePath, file.decryptedRemotePath)
        assertEquals(remote.length, file.fileLength)
        assertEquals(remote.remoteId, file.remoteId)
        assertEquals(remote.etag, file.etag)
        assertEquals(remote.etag, file.etagOnServer)
        assertTrue(file.isLocked)
        assertEquals(remote.lockOwner, file.lockOwnerId)
        assertEquals(remote.lockToken, file.lockToken)
    }

    @Test
    fun directoryMappingRetainsEncryptionAndDirectorySize() {
        val remote = RemoteFile("/folder/").apply {
            mimeType = MimeType.DIRECTORY
            size = 42
            length = 0
            isEncrypted = true
        }

        val file = remote.toOCFile()

        assertTrue(file.isFolder)
        assertTrue(file.isEncrypted)
        assertEquals(remote.size, file.fileLength)
        assertFalse(file.isLocked)
        assertEquals(file.fileLength, FileStorageUtils.fillOCFile(remote).fileLength)
    }
}
