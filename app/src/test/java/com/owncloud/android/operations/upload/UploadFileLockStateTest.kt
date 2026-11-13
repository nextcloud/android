/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.upload

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.files.model.FileLockType
import com.owncloud.android.lib.resources.files.model.RemoteFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

@Suppress("MagicNumber")
class UploadFileLockStateTest {
    private val resolver = UploadFileResolver(mock())

    @Test
    fun uploadedFileUsesCurrentRemoteLockForTheSameAndReplacedIdentity() {
        for (remoteId in listOf("old-id", "new-id")) {
            val file = previouslyLockedFile()
            val remoteFile = RemoteFile("/document.docx").apply {
                this.remoteId = remoteId
                isLocked = true
                lockType = FileLockType.fromValue(0)
                lockOwner = "current-owner"
                lockOwnerDisplayName = "Current owner"
                lockOwnerEditor = "current-editor"
                lockTimestamp = 200
                lockTimeout = 600
                lockToken = "current-token"
            }

            resolver.updateOCFile(file, remoteFile)

            assertEquals(remoteId, file.remoteId)
            assertTrue(file.isLocked)
            assertEquals(remoteFile.lockType, file.lockType)
            assertEquals("current-owner", file.lockOwnerId)
            assertEquals("Current owner", file.lockOwnerDisplayName)
            assertEquals("current-editor", file.lockOwnerEditor)
            assertEquals(200L, file.lockTimestamp)
            assertEquals(600L, file.lockTimeout)
            assertEquals("current-token", file.lockToken)
            assertEquals(7L, file.fileId)
            assertEquals(3L, file.parentId)
            assertEquals("/local/document.docx", file.storagePath)
        }
    }

    @Test
    fun unlockedRemoteFileClearsEveryCachedLockFieldForTheSameAndReplacedIdentity() {
        for (remoteId in listOf("old-id", "new-id")) {
            val file = previouslyLockedFile()
            val remoteFile = RemoteFile("/document.docx").apply {
                this.remoteId = remoteId
            }

            resolver.updateOCFile(file, remoteFile)

            assertEquals(remoteId, file.remoteId)
            assertFalse(file.isLocked)
            assertNull(file.lockType)
            assertNull(file.lockOwnerId)
            assertNull(file.lockOwnerDisplayName)
            assertNull(file.lockOwnerEditor)
            assertEquals(0L, file.lockTimestamp)
            assertEquals(0L, file.lockTimeout)
            assertNull(file.lockToken)
        }
    }

    private fun previouslyLockedFile() = OCFile("/document.docx").apply {
        fileId = 7
        parentId = 3
        storagePath = "/local/document.docx"
        remoteId = "old-id"
        isLocked = true
        lockType = FileLockType.fromValue(2)
        lockOwnerId = "old-owner"
        lockOwnerDisplayName = "Old owner"
        lockOwnerEditor = "old-editor"
        lockTimestamp = 100
        lockTimeout = 300
        lockToken = "old-token"
    }
}
