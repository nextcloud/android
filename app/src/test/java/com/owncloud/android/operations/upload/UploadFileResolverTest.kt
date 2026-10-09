/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.upload

import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.datamodel.e2e.v1.decrypted.Data
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFileV1
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedMetadata
import com.owncloud.android.db.OCUpload
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.utils.MimeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFile as DecryptedFileV1

@Suppress("MagicNumber")
class UploadFileResolverTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val uploadsStorageManager: UploadsStorageManager = mock()
    private val fileDataStorageManager: FileDataStorageManager = mock()
    private val operation: UploadFileOperation = mock()
    private lateinit var resolver: UploadFileResolver

    @Before
    fun setUp() {
        whenever(operation.uploadsStorageManager).thenReturn(uploadsStorageManager)
        whenever(operation.storageManager).thenReturn(fileDataStorageManager)
        whenever(operation.ocUploadId).thenReturn(UPLOAD_ID)
        resolver = UploadFileResolver(operation)
    }

    @Test
    fun obtainNewOCFileToUploadUsesLocalFileSizeAndModificationDate() {
        val localFile = temporaryFolder.newFile("photo.jpg").apply {
            writeBytes(ByteArray(42))
            setLastModified(1_700_000_000_000)
        }

        val file = UploadFileResolver.obtainNewOCFileToUpload("/photo.jpg", localFile.absolutePath, "image/jpeg")

        assertEquals("/photo.jpg", file.remotePath)
        assertEquals(localFile.absolutePath, file.storagePath)
        assertEquals(42L, file.fileLength)
        assertEquals(localFile.lastModified(), file.lastSyncDateForData)
        assertEquals(0L, file.lastSyncDateForProperties)
        assertEquals("image/jpeg", file.mimeType)
    }

    @Test
    fun obtainNewOCFileToUploadWithoutLocalPathKeepsDefaults() {
        val file = UploadFileResolver.obtainNewOCFileToUpload("/photo.jpg", null, "image/jpeg")

        assertNull(file.storagePath)
        assertEquals(0L, file.fileLength)
        assertEquals(0L, file.lastSyncDateForData)
        assertEquals("image/jpeg", file.mimeType)
    }

    @Test
    fun collidedFileNamesOfV1MetadataAreEncryptedFilenames() {
        val files = FILE_NAMES.associateWith { name ->
            DecryptedFileV1().apply { encrypted = Data().apply { filename = name } }
        }
        val metadata = DecryptedFolderMetadataFileV1().apply { setFiles(files) }

        assertEquals(FILE_NAMES, resolver.getCollidedFileNames(metadata))
    }

    @Test
    fun collidedFileNamesOfV2MetadataAreFilenames() {
        val files = FILE_NAMES.associateWith { name ->
            DecryptedFile(filename = name, mimetype = "text/plain", nonce = "", authenticationTag = "", key = "")
        }.toMutableMap()
        val metadata = DecryptedFolderMetadataFile(
            metadata = DecryptedMetadata(files = files, metadataKey = ByteArray(0)),
            version = "2.0"
        )

        assertEquals(FILE_NAMES, resolver.getCollidedFileNames(metadata))
    }

    @Test
    fun collidedFileNamesOfUnknownMetadataAreEmpty() {
        assertTrue(resolver.getCollidedFileNames(null).isEmpty())
        assertTrue(resolver.getCollidedFileNames("metadata").isEmpty())
    }

    @Test
    fun updateSizeStoresSizeOfExistingUpload() {
        val upload: OCUpload = mock()
        whenever(uploadsStorageManager.getUploadById(UPLOAD_ID)).thenReturn(upload)

        resolver.updateSize(1024)

        verify(upload).fileSize = 1024
        verify(uploadsStorageManager).updateUpload(upload)
    }

    @Test
    fun updateSizeIgnoresMissingUpload() {
        whenever(uploadsStorageManager.getUploadById(UPLOAD_ID)).thenReturn(null)

        resolver.updateSize(1024)

        verify(uploadsStorageManager, never()).updateUpload(any())
    }

    @Test
    fun createNewOCFileCopiesPropertiesToNewRemotePath() {
        val original = OCFile("/photo.jpg").apply {
            creationTimestamp = 1
            fileLength = 2
            mimeType = "image/jpeg"
            modificationTimestamp = 3
            modificationTimestampAtLastSyncForData = 4
            etag = "etag"
            lastSyncDateForProperties = 5
            lastSyncDateForData = 6
            storagePath = "/storage/photo.jpg"
            parentId = 7
        }

        val renamed = resolver.createNewOCFile(original, "/photo (2).jpg")

        assertEquals("/photo (2).jpg", renamed.remotePath)
        assertEquals(1L, renamed.creationTimestamp)
        assertEquals(2L, renamed.fileLength)
        assertEquals("image/jpeg", renamed.mimeType)
        assertEquals(3L, renamed.modificationTimestamp)
        assertEquals(4L, renamed.modificationTimestampAtLastSyncForData)
        assertEquals("etag", renamed.etag)
        assertEquals(5L, renamed.lastSyncDateForProperties)
        assertEquals(6L, renamed.lastSyncDateForData)
        assertEquals("/storage/photo.jpg", renamed.storagePath)
        assertEquals(7L, renamed.parentId)
    }

    @Test
    fun createLocalFolderUsesExistingParent() {
        val parent = OCFile("/a/").apply { fileId = 10 }
        whenever(fileDataStorageManager.getFileByPath("/a/")).thenReturn(parent)

        val folder = resolver.createLocalFolder("/a/b/")

        assertNotNull(folder)
        assertEquals("/a/b/", folder?.remotePath)
        assertEquals(MimeType.DIRECTORY, folder?.mimeType)
        assertEquals(10L, folder?.parentId)
        verify(fileDataStorageManager).saveFile(folder)
    }

    @Test
    fun createLocalFolderCreatesMissingParents() {
        val root = OCFile(OCFile.ROOT_PATH).apply { fileId = 1 }
        whenever(fileDataStorageManager.getFileByPath(OCFile.ROOT_PATH)).thenReturn(root)
        var nextId = 2L
        doAnswer {
            it.getArgument<OCFile>(0).fileId = nextId++
            true
        }.whenever(fileDataStorageManager).saveFile(any())

        val folder = resolver.createLocalFolder("/a/b/")

        assertEquals("/a/b/", folder?.remotePath)
        assertEquals(2L, folder?.parentId)
        verify(fileDataStorageManager).saveFile(argThat { remotePath == "/a/" && parentId == 1L })
    }

    @Test
    fun createLocalFolderReturnsNullWithoutParentPath() {
        assertNull(resolver.createLocalFolder(OCFile.ROOT_PATH))
        verify(fileDataStorageManager, never()).saveFile(any())
    }

    @Test
    fun updateOCFileCopiesRemoteProperties() {
        val remoteFile: RemoteFile = mock {
            on { creationTimestamp } doReturn 1L
            on { length } doReturn 2L
            on { mimeType } doReturn "image/jpeg"
            on { modifiedTimestamp } doReturn 3L
            on { etag } doReturn "etag"
            on { remoteId } doReturn "remote-id"
            on { permissions } doReturn "RGDNVW"
            on { uploadTimestamp } doReturn 4L
            on { isHasPreview } doReturn true
        }
        val file = OCFile("/photo.jpg")

        resolver.updateOCFile(file, remoteFile)

        assertEquals(1L, file.creationTimestamp)
        assertEquals(2L, file.fileLength)
        assertEquals("image/jpeg", file.mimeType)
        assertEquals(3L, file.modificationTimestamp)
        assertEquals(3L, file.modificationTimestampAtLastSyncForData)
        assertEquals("etag", file.etag)
        assertEquals("etag", file.etagOnServer)
        assertEquals("remote-id", file.remoteId)
        assertEquals("RGDNVW", file.permissions)
        assertEquals(4L, file.uploadTimestamp)
        assertTrue(file.isPreviewAvailable)
    }

    private companion object {
        const val UPLOAD_ID = 5L
        val FILE_NAMES = listOf("first.txt", "second.txt")
    }
}
