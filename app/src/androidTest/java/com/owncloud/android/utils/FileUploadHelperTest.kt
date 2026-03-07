/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.utils

import com.nextcloud.client.database.dao.UploadDao
import com.nextcloud.client.database.entity.UploadEntity
import com.nextcloud.client.database.entity.toOCUpload
import com.nextcloud.client.database.entity.toUploadEntity
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus
import com.owncloud.android.db.OCUpload
import com.owncloud.android.db.UploadResult
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.resources.status.OCCapability
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@Suppress("TooManyFunctions")
class FileUploadHelperTest {

    private lateinit var uploadDao: UploadDao
    private lateinit var fileUploadHelper: FileUploadHelper

    private val accountName = "test@nextcloud.example.com"
    private val localPath = "/sdcard/DCIM/photo.jpg"

    @Suppress("LongParameterList")
    private fun buildEntity(
        id: Int = 1,
        localPath: String = this.localPath,
        remotePath: String = "/remote/path/photo.jpg",
        accountName: String = this.accountName,
        status: Int = UploadStatus.UPLOAD_IN_PROGRESS.value,
        lastResult: Int = UploadResult.UNKNOWN.value,
        nameCollisionPolicy: Int = NameCollisionPolicy.DEFAULT.serialize(),
        fileSize: Long = 1024L,
        isWifiOnly: Int = 0,
        isWhileChargingOnly: Int = 0,
        isCreateRemoteFolder: Int = 1,
        createdBy: Int = 0,
        folderUnlockToken: String? = null,
        uploadEndTimestampLong: Long? = null
    ) = UploadEntity(
        id = id,
        localPath = localPath,
        remotePath = remotePath,
        accountName = accountName,
        fileSize = fileSize,
        status = status,
        localBehaviour = 0,
        uploadTime = null,
        nameCollisionPolicy = nameCollisionPolicy,
        isCreateRemoteFolder = isCreateRemoteFolder,
        uploadEndTimestamp = 0,
        uploadEndTimestampLong = uploadEndTimestampLong,
        lastResult = lastResult,
        isWhileChargingOnly = isWhileChargingOnly,
        isWifiOnly = isWifiOnly,
        createdBy = createdBy,
        folderUnlockToken = folderUnlockToken
    )

    private fun buildOCUpload(
        localPath: String = this.localPath,
        remotePath: String = "/remote/path/photo.jpg",
        accountName: String = this.accountName
    ): OCUpload = OCUpload(localPath, remotePath, accountName).apply {
        uploadId = 1L
        fileSize = 1024L
        uploadStatus = UploadStatus.UPLOAD_IN_PROGRESS
        nameCollisionPolicy = NameCollisionPolicy.DEFAULT
        isCreateRemoteFolder = true
        localAction = 0
        isUseWifiOnly = false
        isWhileChargingOnly = false
        lastResult = UploadResult.UNKNOWN
        createdBy = 0
        folderUnlockToken = null
    }

    @Before
    fun setUp() {
        uploadDao = mockk(relaxed = true)
        fileUploadHelper = spyk(FileUploadHelper(), recordPrivateCalls = true)
        val uploadsStorageManager = mockk<com.owncloud.android.datamodel.UploadsStorageManager>(relaxed = true)

        val daoField = com.owncloud.android.datamodel.UploadsStorageManager::class.java
            .declaredFields
            .first { it.type == UploadDao::class.java }
        daoField.isAccessible = true
        daoField.set(uploadsStorageManager, uploadDao)

        val field = FileUploadHelper::class.java.getDeclaredField("uploadsStorageManager")
        field.isAccessible = true
        field.set(fileUploadHelper, uploadsStorageManager)
    }

    @Test
    fun getUploadByPathsExactMatch() {
        val remotePath = "/remote/path/photo.jpg"
        val entity = buildEntity(remotePath = remotePath)
        every { uploadDao.getUploadByAccountAndPaths(accountName, localPath, remotePath) } returns entity

        val result = fileUploadHelper.getUploadByPaths(accountName, localPath, remotePath)

        assertNotNull(result)
        assertEquals(remotePath, result?.remotePath)
        verify(exactly = 1) { uploadDao.getUploadByAccountAndPaths(accountName, localPath, remotePath) }

        // alternative path should NOT be queried when exact match found
        verify(exactly = 0) { uploadDao.getUploadByAccountAndPaths(accountName, localPath, "/remote/path/photo.JPG") }
    }

    @Test
    fun getUploadByPathsCaseInsensitiveExtensionFallback() {
        // DB stores "/a/b/1.TXT", caller searches with "/a/b/1.txt"
        val searchPath = "/a/b/AAA/b/1.txt"
        val storedPath = "/a/b/AAA/b/1.TXT"
        val entity = buildEntity(remotePath = storedPath)

        every { uploadDao.getUploadByAccountAndPaths(accountName, localPath, searchPath) } returns null
        every { uploadDao.getUploadByAccountAndPaths(accountName, localPath, storedPath) } returns entity

        val result = fileUploadHelper.getUploadByPaths(accountName, localPath, searchPath)

        assertNotNull(result)
        assertEquals(storedPath, result?.remotePath)
        verify(exactly = 1) { uploadDao.getUploadByAccountAndPaths(accountName, localPath, searchPath) }
        verify(exactly = 1) { uploadDao.getUploadByAccountAndPaths(accountName, localPath, storedPath) }
    }

    @Test
    fun getUploadByPathsFindsRecordWhenDBHasLowercaseExtensionButSearchUsesUppercase() {
        // DB stores "/a/b/1.txt", caller searches with "/a/b/1.TXT"
        val searchPath = "/a/b/AAA/b/1.TXT"
        val storedPath = "/a/b/AAA/b/1.txt"
        val entity = buildEntity(remotePath = storedPath)

        every { uploadDao.getUploadByAccountAndPaths(accountName, localPath, searchPath) } returns null
        every { uploadDao.getUploadByAccountAndPaths(accountName, localPath, storedPath) } returns entity

        val result = fileUploadHelper.getUploadByPaths(accountName, localPath, searchPath)

        assertNotNull(result)
        assertEquals(storedPath, result?.remotePath)
    }

    @Test
    fun getUploadByPathsReturnsNullWhenNeitherExactNorAlternativeExtensionPathExists() {
        val remotePath = "/a/b/1.jpg"
        every { uploadDao.getUploadByAccountAndPaths(accountName, localPath, remotePath) } returns null
        every { uploadDao.getUploadByAccountAndPaths(accountName, localPath, "/a/b/1.JPG") } returns null

        val result = fileUploadHelper.getUploadByPaths(accountName, localPath, remotePath)

        assertNull(result)
    }

    @Test
    fun getUploadByPathsReturnsNullWhenRemotePathHasNoExtension() {
        val remotePath = "/a/b/fileWithoutExtension"
        every { uploadDao.getUploadByAccountAndPaths(accountName, localPath, remotePath) } returns null

        val result = fileUploadHelper.getUploadByPaths(accountName, localPath, remotePath)

        assertNull(result)
    }

    @Test
    fun getUploadByPathsReturnsNullWhenRemotePathIsEmpty() {
        val remotePath = ""
        every { uploadDao.getUploadByAccountAndPaths(accountName, localPath, remotePath) } returns null

        val result = fileUploadHelper.getUploadByPaths(accountName, localPath, remotePath)

        assertNull(result)
    }

    @Test
    fun getUploadByPathsHandlesDeepNestedPathWithUppercaseExtension() {
        val searchPath = "/a/b/c/d/e/file.PNG"
        val storedPath = "/a/b/c/d/e/file.png"
        val entity = buildEntity(remotePath = storedPath)

        every { uploadDao.getUploadByAccountAndPaths(accountName, localPath, searchPath) } returns null
        every { uploadDao.getUploadByAccountAndPaths(accountName, localPath, storedPath) } returns entity

        val result = fileUploadHelper.getUploadByPaths(accountName, localPath, searchPath)

        assertNotNull(result)
        assertEquals(storedPath, result?.remotePath)
    }

    @Test
    fun getUploadByPathsOnlyTogglesExtensionNotRestOfFilenameOrPath() {
        val searchPath = "/a/b/AAA/b/1.txt"
        val wrongPath = "/a/b/aaa/b/1.TXT"
        val storedPath = "/a/b/AAA/b/1.TXT"
        val entity = buildEntity(remotePath = storedPath)

        every { uploadDao.getUploadByAccountAndPaths(accountName, localPath, searchPath) } returns null
        every { uploadDao.getUploadByAccountAndPaths(accountName, localPath, storedPath) } returns entity
        every { uploadDao.getUploadByAccountAndPaths(accountName, localPath, wrongPath) } returns null

        val result = fileUploadHelper.getUploadByPaths(accountName, localPath, searchPath)

        assertNotNull(result)
        assertEquals(storedPath, result?.remotePath)
        verify(exactly = 0) { uploadDao.getUploadByAccountAndPaths(accountName, localPath, wrongPath) }
    }

    @Test
    fun toOCUploadMapsAllFieldsCorrectlyWithoutCapability() {
        val entity = buildEntity(
            id = 42,
            localPath = localPath,
            remotePath = "/remote/photo.jpg",
            accountName = accountName,
            fileSize = 2048L,
            status = UploadStatus.UPLOAD_IN_PROGRESS.value,
            lastResult = UploadResult.UPLOADED.value,
            nameCollisionPolicy = NameCollisionPolicy.RENAME.serialize(),
            isWifiOnly = 1,
            isWhileChargingOnly = 1,
            isCreateRemoteFolder = 1,
            createdBy = 2,
            folderUnlockToken = "token-abc",
            uploadEndTimestampLong = 1234567890L
        )

        val result = entity.toOCUpload()

        assertNotNull(result)
        assertEquals(42L, result!!.uploadId)
        assertEquals(localPath, result.localPath)
        assertEquals("/remote/photo.jpg", result.remotePath)
        assertEquals(accountName, result.accountName)
        assertEquals(2048L, result.fileSize)
        assertEquals(UploadStatus.UPLOAD_IN_PROGRESS, result.uploadStatus)
        assertEquals(UploadResult.UPLOADED, result.lastResult)
        assertEquals(NameCollisionPolicy.RENAME, result.nameCollisionPolicy)
        assertEquals(true, result.isUseWifiOnly)
        assertEquals(true, result.isWhileChargingOnly)
        assertEquals(true, result.isCreateRemoteFolder)
        assertEquals(2, result.createdBy)
        assertEquals("token-abc", result.folderUnlockToken)
        assertEquals(1234567890L, result.uploadEndTimestamp)
    }

    @Test
    fun toOCUploadMapsBooleanFalseFieldsCorrectly() {
        val entity = buildEntity(isWifiOnly = 0, isWhileChargingOnly = 0, isCreateRemoteFolder = 0)

        val result = entity.toOCUpload()

        assertNotNull(result)
        assertEquals(false, result!!.isUseWifiOnly)
        assertEquals(false, result.isWhileChargingOnly)
        assertEquals(false, result.isCreateRemoteFolder)
    }

    @Test
    fun toOCUploadReturnsNullWhenLocalPathIsNull() {
        val entity = buildEntity().copy(localPath = null)

        val result = entity.toOCUpload()

        // OCUpload constructor throws IllegalArgumentException for null localPath
        assertNull(result)
    }

    @Test
    fun toOCUploadReturnsNullWhenRemotePathIsNull() {
        val entity = buildEntity().copy(remotePath = null)

        val result = entity.toOCUpload()

        // OCUpload constructor throws IllegalArgumentException for null remotePath
        assertNull(result)
    }

    @Test
    fun toOCUploadAppliesAutoRenameWhenCapabilityIsProvided() {
        val capability = mockk<OCCapability>(relaxed = true)
        val entity = buildEntity(remotePath = "/remote/photo.jpg")
        val result = entity.toOCUpload(capability)
        assertNotNull(result)
    }

    @Test
    fun toOCUploadHandlesNullOptionalFieldsGracefully() {
        val entity = UploadEntity(
            id = null,
            localPath = localPath,
            remotePath = "/remote/photo.jpg",
            accountName = accountName,
            fileSize = null,
            status = null,
            localBehaviour = null,
            uploadTime = null,
            nameCollisionPolicy = null,
            isCreateRemoteFolder = null,
            uploadEndTimestamp = null,
            uploadEndTimestampLong = null,
            lastResult = null,
            isWhileChargingOnly = null,
            isWifiOnly = null,
            createdBy = null,
            folderUnlockToken = null
        )

        // should not throw; optional fields simply stay at OCUpload defaults
        val result = entity.toOCUpload()

        assertNotNull(result)
    }

    @Test
    fun toUploadEntityMapsAllFieldsCorrectlyWhenUploadIdIsSet() {
        val upload = buildOCUpload().apply {
            uploadId = 10L
            fileSize = 512L
            uploadStatus = UploadStatus.UPLOAD_FAILED
            nameCollisionPolicy = NameCollisionPolicy.ASK_USER
            isCreateRemoteFolder = false
            localAction = 2
            isUseWifiOnly = true
            isWhileChargingOnly = true
            lastResult = UploadResult.FILE_NOT_FOUND
            createdBy = 3
            folderUnlockToken = "unlock-token"
            uploadEndTimestamp = 9876543210L
        }

        val entity = upload.toUploadEntity()

        assertEquals(10, entity.id)
        assertEquals(localPath, entity.localPath)
        assertEquals("/remote/path/photo.jpg", entity.remotePath)
        assertEquals(accountName, entity.accountName)
        assertEquals(512L, entity.fileSize)
        assertEquals(UploadStatus.UPLOAD_FAILED.value, entity.status)
        assertEquals(NameCollisionPolicy.ASK_USER.serialize(), entity.nameCollisionPolicy)
        assertEquals(0, entity.isCreateRemoteFolder)
        assertEquals(2, entity.localBehaviour)
        assertEquals(1, entity.isWifiOnly)
        assertEquals(1, entity.isWhileChargingOnly)
        assertEquals(UploadResult.FILE_NOT_FOUND.value, entity.lastResult)
        assertEquals(3, entity.createdBy)
        assertEquals("unlock-token", entity.folderUnlockToken)
        assertEquals(9876543210L, entity.uploadEndTimestampLong)
        assertEquals(0, entity.uploadEndTimestamp) // legacy field always 0
        assertNull(entity.uploadTime) // always null
    }

    @Test
    fun toUploadEntitySetsIdToNullWhenUploadIdIsNotValidToAllowRoomAutoGenerate() {
        val upload = buildOCUpload().apply { uploadId = -1L }

        val entity = upload.toUploadEntity()

        assertNull(entity.id)
    }

    @Test
    fun toUploadEntityPreservesExistingIdWhenUploadIdIsPositive() {
        val upload = buildOCUpload().apply { uploadId = 99L }

        val entity = upload.toUploadEntity()

        assertEquals(99, entity.id)
    }

    @Test
    fun toUploadEntityIsUseWifiOnlyIsFalseShouldReturnZero() {
        val upload = buildOCUpload().apply { isUseWifiOnly = false }

        val entity = upload.toUploadEntity()

        assertEquals(0, entity.isWifiOnly)
    }

    @Test
    fun toUploadEntityMapsIsWhileChargingIsFalseShouldReturnZero() {
        val upload = buildOCUpload().apply { isWhileChargingOnly = false }

        val entity = upload.toUploadEntity()

        assertEquals(0, entity.isWhileChargingOnly)
    }

    @Test
    fun toUploadEntityMapsIsCreateRemoteFolderTrueShouldReturnOne() {
        val upload = buildOCUpload().apply { isCreateRemoteFolder = true }

        val entity = upload.toUploadEntity()

        assertEquals(1, entity.isCreateRemoteFolder)
    }

    @Test
    fun testEntityAndOCUploadConversionTogether() {
        val original = buildOCUpload().apply {
            uploadId = 7L
            fileSize = 333L
            uploadStatus = UploadStatus.UPLOAD_FAILED
            nameCollisionPolicy = NameCollisionPolicy.RENAME
            isCreateRemoteFolder = true
            localAction = 1
            isUseWifiOnly = true
            isWhileChargingOnly = false
            lastResult = UploadResult.NETWORK_CONNECTION
            createdBy = 1
            folderUnlockToken = "rt-token"
            uploadEndTimestamp = 111L
        }

        val entity = original.toUploadEntity()
        val restored = entity.toOCUpload()

        assertNotNull(restored)
        assertEquals(original.uploadId, restored!!.uploadId)
        assertEquals(original.localPath, restored.localPath)
        assertEquals(original.remotePath, restored.remotePath)
        assertEquals(original.accountName, restored.accountName)
        assertEquals(original.fileSize, restored.fileSize)
        assertEquals(original.uploadStatus, restored.uploadStatus)
        assertEquals(original.nameCollisionPolicy, restored.nameCollisionPolicy)
        assertEquals(original.isCreateRemoteFolder, restored.isCreateRemoteFolder)
        assertEquals(original.localAction, restored.localAction)
        assertEquals(original.isUseWifiOnly, restored.isUseWifiOnly)
        assertEquals(original.isWhileChargingOnly, restored.isWhileChargingOnly)
        assertEquals(original.lastResult, restored.lastResult)
        assertEquals(original.createdBy, restored.createdBy)
        assertEquals(original.folderUnlockToken, restored.folderUnlockToken)
        assertEquals(original.uploadEndTimestamp, restored.uploadEndTimestamp)
    }
}
