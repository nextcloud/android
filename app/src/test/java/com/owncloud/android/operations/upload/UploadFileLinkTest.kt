/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.upload

import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus
import com.owncloud.android.db.OCUpload
import com.owncloud.android.db.UploadResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

class UploadFileLinkTest {
    private val storage: FileDataStorageManager = mock()

    @Test
    fun identicalEncryptedConflictBecomesCompletedWithPhysicalRemoteLink() {
        verifyCompletion(encrypted = true)
    }

    @Test
    fun identicalOrdinaryConflictUsesTheSameCompletionFlow() {
        verifyCompletion(encrypted = false)
    }

    private fun verifyCompletion(encrypted: Boolean) {
        val uploads: UploadsStorageManager = mock()
        val localPath = File.separator + "photo.jpg"
        val upload = OCUpload(localPath, "/private/photo.jpg", "account-A").apply {
            uploadId = 42
            uploadStatus = UploadStatus.UPLOAD_FAILED
            lastResult = UploadResult.SYNC_CONFLICT
        }
        val remote = OCFile(if (encrypted) "/private/cipher" else "/private/photo.jpg").apply {
            isEncrypted = encrypted
            decryptedRemotePath = "/private/photo.jpg"
            fileLength = 3
        }
        whenever(storage.getFileByEncryptedRemotePath(remote.remotePath)).thenReturn(remote)

        UploadFileLink.completeIdentical(upload, remote, uploads)

        verify(uploads).updateUpload(
            argThat {
                uploadId == 42L && accountName == "account-A" && this.localPath == localPath &&
                    remotePath == remote.remotePath && fileSize == 3L &&
                    uploadStatus == UploadStatus.UPLOAD_SUCCEEDED && lastResult == UploadResult.SAME_FILE_CONFLICT
            }
        )
        assertSame(remote, UploadFileLink.findUploadedFile(storage, upload.remotePath))
        assertNull(remote.storagePath)
    }

    @Test
    fun oldCompletedPlaintextLinkResolvesToEncryptedFile() {
        val remote = OCFile("/private/cipher").apply { isEncrypted = true }
        whenever(storage.getFileByDecryptedRemotePath("/private/photo.jpg")).thenReturn(remote)

        assertSame(remote, UploadFileLink.findUploadedFile(storage, "/private/photo.jpg"))
    }

    @Test
    fun physicalPathTakesPrecedenceOverDecryptedNameCollision() {
        val remote = OCFile("/private/cipher")
        whenever(storage.getFileByEncryptedRemotePath(remote.remotePath)).thenReturn(remote)

        assertSame(remote, UploadFileLink.findUploadedFile(storage, remote.remotePath))
        verify(storage, never()).getFileByDecryptedRemotePath(any())
    }

    @Test
    fun decryptedFallbackCannotSelectAnUnencryptedFile() {
        whenever(storage.getFileByDecryptedRemotePath("/private/photo.jpg"))
            .thenReturn(OCFile("/other/photo.jpg"))

        assertNull(UploadFileLink.findUploadedFile(storage, "/private/photo.jpg"))
    }

    @Test
    fun missingRemoteLinkRemainsAnError() {
        assertEquals(null, UploadFileLink.findUploadedFile(storage, "/private/missing"))
    }

    @Test
    fun encryptedListUsesDecryptedNameAndParentEvenAfterRemoteRename() {
        val upload = OCUpload(File.separator + "original.jpg", "/cipher-folder/cipher", "account-A")
        val remote = OCFile(upload.remotePath).apply {
            isEncrypted = true
            decryptedRemotePath = "/private/renamed.jpg"
        }

        assertEquals("/private/renamed.jpg", UploadFileLink.displayPath(storage, upload, remote))
    }

    @Test
    fun rawCiphertextPropertiesUseSourceNameAndDecryptedParentForDisplay() {
        val upload = OCUpload(File.separator + "photo.jpg", "/cipher-folder/cipher", "account-A")
        val parent = OCFile("/cipher-folder/").apply {
            isEncrypted = true
            decryptedRemotePath = "/private/"
        }
        whenever(storage.getFileByEncryptedRemotePath(parent.remotePath)).thenReturn(parent)

        assertEquals("/private/photo.jpg", UploadFileLink.displayPath(storage, upload, OCFile(upload.remotePath)))
    }

    @Test
    fun ordinaryUploadDisplaysRemoteRenameInsteadOfSourceName() {
        val upload = OCUpload(File.separator + "original.jpg", "/photos/renamed.jpg", "account-A")

        assertEquals(upload.remotePath, UploadFileLink.displayPath(storage, upload, null))
    }
}
