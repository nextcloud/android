/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.thumbnail

import com.nextcloud.client.database.dao.UploadDao
import com.nextcloud.client.database.entity.toUploadEntity
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus
import com.owncloud.android.db.OCUpload
import com.owncloud.android.db.UploadResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@Suppress("MagicNumber")
class UploadThumbnailSourceTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val uploads: UploadDao = mock()

    @Test
    fun everyUploadStateUsesReadableSourceWithoutChangingRemoteDownloadState() {
        val local = localFile()
        val remote = encryptedFile()
        for (status in UploadStatus.entries) {
            val upload = upload(local).apply { whenever(uploadStatus).thenReturn(status) }

            val thumbnail = UploadThumbnailSource.forUpload(upload, remote)

            assertNotSame(remote, thumbnail)
            assertEquals(local.absolutePath, thumbnail.storagePath)
            assertNull(thumbnail.remoteId)
            assertNull(remote.storagePath)
            assertEquals("server-id", remote.remoteId)
        }
    }

    @Test
    fun missingOrDirectorySourceFallsBackToExistingRemoteMetadata() {
        val remote = encryptedFile()
        assertSame(remote, UploadThumbnailSource.forUpload(upload(File(temporaryFolder.root, "missing")), remote))
        assertSame(remote, UploadThumbnailSource.forUpload(upload(temporaryFolder.root), remote))
    }

    @Test
    fun missingSourceAndRemoteProducesAnIconWithoutInventingALocalCopy() {
        val thumbnail = UploadThumbnailSource.forUpload(upload(File(temporaryFolder.root, "missing")), null)
        assertNull(thumbnail.storagePath)
        assertEquals("image/jpeg", thumbnail.mimeType)
    }

    @Test
    fun encryptedFolderUsesTheCompletedUploadsLocalOriginal() {
        val local = localFile()
        val remote = encryptedFile()
        val entity = upload(local).toUploadEntity()
        for (result in listOf(UploadResult.UPLOADED, UploadResult.SKIPPED, UploadResult.SAME_FILE_CONFLICT)) {
            whenever(uploads.getByRemotePath(remote.remotePath, "account-A"))
                .thenReturn(entity.copy(lastResult = result.value))
            assertEquals(local, UploadThumbnailSource.forEncryptedFile(remote, "account-A", uploads))
        }
        assertNull(remote.storagePath)
    }

    @Test
    fun encryptedFolderCanResolveOldLogicalUploadPaths() {
        val local = localFile()
        val remote = encryptedFile()
        val entity = upload(local).toUploadEntity().copy(remotePath = remote.decryptedRemotePath)
        whenever(uploads.getByRemotePath(remote.decryptedRemotePath, "account-A"))
            .thenReturn(entity)

        assertEquals(local, UploadThumbnailSource.forEncryptedFile(remote, "account-A", uploads))
    }

    @Test
    fun anotherAccountsUploadCannotProvideThePreview() {
        val remote = encryptedFile()
        val entity = upload(localFile()).toUploadEntity().copy(accountName = "account-B")
        whenever(uploads.getByRemotePath(remote.remotePath, "account-A"))
            .thenReturn(entity)

        assertNull(UploadThumbnailSource.forEncryptedFile(remote, "account-A", uploads))
        verify(uploads, never()).getByRemotePath(any(), org.mockito.kotlin.eq("account-B"))
    }

    @Test
    fun pendingFailedOrCancelledUploadsCannotStandInForTheEncryptedServerFile() {
        val remote = encryptedFile()
        val entity = upload(localFile()).toUploadEntity()
        for (status in UploadStatus.entries.filter { it != UploadStatus.UPLOAD_SUCCEEDED }) {
            whenever(uploads.getByRemotePath(remote.remotePath, "account-A"))
                .thenReturn(entity.copy(status = status.value))
            assertNull(UploadThumbnailSource.forEncryptedFile(remote, "account-A", uploads))
        }
    }

    @Test
    fun missingFileResultIsNotACompletedPreviewSource() {
        val remote = encryptedFile()
        val entity = upload(localFile()).toUploadEntity().copy(lastResult = UploadResult.FILE_NOT_FOUND.value)
        whenever(uploads.getByRemotePath(remote.remotePath, "account-A"))
            .thenReturn(entity)

        assertNull(UploadThumbnailSource.forEncryptedFile(remote, "account-A", uploads))
    }

    @Test
    fun encryptedTransferSizeIncludesTheGcmAuthenticationTag() {
        val local = localFile()
        val remote = encryptedFile()
        val entity = upload(local).toUploadEntity().copy(fileSize = local.length() + 16)
        whenever(uploads.getByRemotePath(remote.remotePath, "account-A")).thenReturn(entity)

        assertEquals(local, UploadThumbnailSource.forEncryptedFile(remote, "account-A", uploads))

        whenever(uploads.getByRemotePath(remote.remotePath, "account-A"))
            .thenReturn(entity.copy(lastResult = UploadResult.SKIPPED.value))
        assertNull(UploadThumbnailSource.forEncryptedFile(remote, "account-A", uploads))

        whenever(uploads.getByRemotePath(remote.remotePath, "account-A"))
            .thenReturn(entity.copy(uploadEndTimestampLong = local.lastModified() - 1))
        assertNull(UploadThumbnailSource.forEncryptedFile(remote, "account-A", uploads))
    }

    @Test
    fun changedLocalFileIsNotUsedAsTheCloudVersionsPreview() {
        val local = localFile()
        val remote = encryptedFile()
        val entity = upload(local).toUploadEntity()
        whenever(uploads.getByRemotePath(remote.remotePath, "account-A")).thenReturn(entity)
        local.appendText("changed")
        assertNull(UploadThumbnailSource.forEncryptedFile(remote, "account-A", uploads))

        whenever(uploads.getByRemotePath(remote.remotePath, "account-A"))
            .thenReturn(entity.copy(fileSize = local.length(), uploadEndTimestampLong = local.lastModified() - 1))
        assertNull(UploadThumbnailSource.forEncryptedFile(remote, "account-A", uploads))
    }

    @Test
    fun deletedSourceAndUnknownCloudFilesHaveNoLocalFallback() {
        val remote = encryptedFile()
        assertNull(UploadThumbnailSource.forEncryptedFile(remote, "account-A", uploads))
        val local = localFile()
        val entity = upload(local).toUploadEntity()
        whenever(uploads.getByRemotePath(remote.remotePath, "account-A"))
            .thenReturn(entity)
        assertTrue(local.delete())
        assertNull(UploadThumbnailSource.forEncryptedFile(remote, "account-A", uploads))
    }

    @Test
    fun ordinaryCloudPreviewDoesNotConsultUploadHistory() {
        assertNull(UploadThumbnailSource.forEncryptedFile(OCFile("/photo.jpg"), "account-A", uploads))
        verify(uploads, never()).getByRemotePath(any(), any())
    }

    @Test
    fun localCacheIdentityChangesWhenSizeOrModificationTimeChanges() {
        val local = localFile()
        val initial = UploadThumbnailSource.cacheKey(local)
        assertEquals(initial, UploadThumbnailSource.cacheKey(local))
        local.appendText("changed")
        val resized = UploadThumbnailSource.cacheKey(local)
        assertNotEquals(initial, resized)
        assertTrue(local.setLastModified(local.lastModified() + 10_000))
        assertNotEquals(resized, UploadThumbnailSource.cacheKey(local))
    }

    private fun localFile() = temporaryFolder.newFile().apply { writeText("image bytes") }

    private fun encryptedFile() = OCFile("/private/cipher").apply {
        isEncrypted = true
        decryptedRemotePath = "/private/photo.jpg"
        mimeType = "image/jpeg"
        remoteId = "server-id"
    }

    private fun upload(local: File): OCUpload = mock<OCUpload>().apply {
        whenever(localPath).thenReturn(local.absolutePath)
        whenever(remotePath).thenReturn("/private/cipher")
        whenever(accountName).thenReturn("account-A")
        whenever(mimeType).thenReturn("image/jpeg")
        whenever(uploadStatus).thenReturn(UploadStatus.UPLOAD_SUCCEEDED)
        whenever(lastResult).thenReturn(UploadResult.UPLOADED)
        whenever(fileSize).thenReturn(local.length())
        whenever(uploadEndTimestamp).thenReturn(System.currentTimeMillis())
    }
}
