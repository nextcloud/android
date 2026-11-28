/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.thumbnail

import com.nextcloud.client.database.dao.FileSystemDao
import com.nextcloud.client.database.dao.UploadDao
import com.nextcloud.client.database.entity.FilesystemEntity
import com.owncloud.android.datamodel.OCFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@Suppress("MagicNumber")
class AutoUploadThumbnailSourceTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val filesystem: FileSystemDao = mock()
    private val uploads: UploadDao = mock()

    @Test
    fun clearedUploadHistoryCanUseTheRetainedMappingForTheSameAccountAndPhysicalPath() {
        val local = temporaryFolder.newFile().apply { writeText("image bytes") }
        val remote = encryptedFile(local)
        whenever(filesystem.getHandledByRemotePath(remote.remotePath, "account-A"))
            .thenReturn(listOf(entry(local, remote)))

        assertEquals(local, UploadThumbnailSource.forEncryptedFile(remote, "account-A", uploads, filesystem))
        verify(filesystem).getHandledByRemotePath("/private/cipher", "account-A")
        assertNull(remote.storagePath)

        assertNull(UploadThumbnailSource.forEncryptedFile(remote, "account-B", uploads, filesystem))
    }

    @Test
    fun changedOrDeletedLocalSourceCannotRepresentTheCloudFile() {
        val local = temporaryFolder.newFile().apply { writeText("image bytes") }
        val remote = encryptedFile(local)
        whenever(filesystem.getHandledByRemotePath(remote.remotePath, "account-A"))
            .thenReturn(listOf(entry(local, remote)))
        assertTrue(local.setLastModified(local.lastModified() + 10_000))
        assertNull(UploadThumbnailSource.forEncryptedFile(remote, "account-A", uploads, filesystem))
        assertTrue(local.delete())
        assertNull(UploadThumbnailSource.forEncryptedFile(remote, "account-A", uploads, filesystem))
    }

    @Test
    fun pendingEntriesAndDifferentPhysicalPathsCannotProvideTheThumbnail() {
        val local = temporaryFolder.newFile().apply { writeText("image bytes") }
        val remote = encryptedFile(local)
        for (invalid in listOf(
            entry(local, remote).copy(fileSentForUpload = 0),
            entry(local, remote).copy(remotePath = remote.decryptedRemotePath),
            entry(local, remote).copy(fileIsFolder = 1)
        )) {
            whenever(filesystem.getHandledByRemotePath(remote.remotePath, "account-A"))
                .thenReturn(listOf(invalid))
            assertNull(UploadThumbnailSource.forEncryptedFile(remote, "account-A", uploads, filesystem))
        }
    }

    @Test
    fun differentCloudSizeIsRejectedButPlaintextAndCiphertextLengthsAreSupported() {
        val local = temporaryFolder.newFile().apply { writeText("image bytes") }
        val remote = encryptedFile(local)
        whenever(filesystem.getHandledByRemotePath(remote.remotePath, "account-A"))
            .thenReturn(listOf(entry(local, remote)))
        remote.fileLength = local.length()
        assertEquals(local, UploadThumbnailSource.forEncryptedFile(remote, "account-A", uploads, filesystem))
        remote.fileLength = local.length() + 1
        assertNull(UploadThumbnailSource.forEncryptedFile(remote, "account-A", uploads, filesystem))
    }

    private fun encryptedFile(local: File) = OCFile("/private/cipher").apply {
        isEncrypted = true
        decryptedRemotePath = "/private/photo.jpg"
        mimeType = "image/jpeg"
        fileLength = local.length() + 16
        remoteId = "server-id"
    }

    private fun entry(local: File, remote: OCFile) = FilesystemEntity(
        id = 1,
        localPath = local.absolutePath,
        remotePath = remote.remotePath,
        fileIsFolder = 0,
        fileFoundRecently = System.currentTimeMillis(),
        fileSentForUpload = 1,
        syncedFolderId = "3",
        crc32 = null,
        fileModified = local.lastModified()
    )
}
