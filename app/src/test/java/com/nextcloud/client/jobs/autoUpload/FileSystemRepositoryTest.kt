/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.autoUpload

import android.content.Context
import com.nextcloud.client.database.dao.FileSystemDao
import com.nextcloud.client.database.dao.UploadDao
import com.nextcloud.client.database.entity.FilesystemEntity
import com.nextcloud.client.database.entity.UploadEntity
import com.nextcloud.client.preferences.SubFolderRule
import com.owncloud.android.datamodel.AutoUploadFolder
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus
import com.owncloud.android.db.UploadResult
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FileSystemRepositoryTest {

    private val uploadDao: UploadDao = mockk()
    private val uploadsStorageManager: UploadsStorageManager = mockk()
    private val repository = FileSystemRepository(
        mockk<FileSystemDao>(relaxed = true),
        uploadsStorageManager,
        mockk<Context>(relaxed = true)
    )

    private val file = File(LOCAL_PATH)
    private val folder = AutoUploadFolder(
        id = 1L,
        localPath = "/tmp",
        remotePath = "/Photos",
        isWifiOnly = false,
        isChargingOnly = false,
        alsoUploadExistingFiles = true,
        isSubfolderByDate = false,
        account = ACCOUNT,
        uploadAction = 0,
        nameCollisionPolicyInt = 0,
        enabled = true,
        timestampMs = 0L,
        type = MediaFolderType.IMAGE,
        isHidden = false,
        subfolderRule = SubFolderRule.YEAR_MONTH,
        isExcludeHidden = false,
        lastScanTimestampMs = 0L
    )
    private val entity = FilesystemEntity(
        id = 1,
        localPath = LOCAL_PATH,
        remotePath = REMOTE_PATH,
        fileIsFolder = 0,
        fileFoundRecently = 0L,
        fileSentForUpload = 1,
        syncedFolderId = "1",
        crc32 = null,
        fileModified = 0L
    )

    @Test
    fun succeeded_upload_is_finished() {
        stubUpload(UploadStatus.UPLOAD_SUCCEEDED, UploadResult.UPLOADED)

        assertTrue(repository.wasUploadFinished(entity, folder, LOCAL_PATH, file))
    }

    @Test
    fun missing_upload_is_finished() {
        every { uploadsStorageManager.uploadDao } returns uploadDao
        every { uploadDao.getUploadByAccountAndPaths(ACCOUNT, LOCAL_PATH, REMOTE_PATH) } returns null

        assertTrue(repository.wasUploadFinished(entity, folder, LOCAL_PATH, file))
    }

    @Test
    fun failed_upload_with_retryable_result_is_not_finished() {
        stubUpload(UploadStatus.UPLOAD_FAILED, UploadResult.NETWORK_CONNECTION)

        assertFalse(repository.wasUploadFinished(entity, folder, LOCAL_PATH, file))
    }

    @Test
    fun failed_upload_with_conflict_is_finished() {
        stubUpload(UploadStatus.UPLOAD_FAILED, UploadResult.SYNC_CONFLICT)

        assertTrue(repository.wasUploadFinished(entity, folder, LOCAL_PATH, file))
    }

    private fun stubUpload(status: UploadStatus, lastResult: UploadResult) {
        every { uploadsStorageManager.uploadDao } returns uploadDao
        every {
            uploadDao.getUploadByAccountAndPaths(ACCOUNT, LOCAL_PATH, REMOTE_PATH)
        } returns UploadEntity(
            id = 1,
            localPath = LOCAL_PATH,
            remotePath = REMOTE_PATH,
            accountName = ACCOUNT,
            fileSize = 1L,
            status = status.value,
            localBehaviour = 0,
            uploadTime = 0,
            nameCollisionPolicy = 0,
            isCreateRemoteFolder = 0,
            uploadEndTimestamp = 0,
            uploadEndTimestampLong = 0L,
            lastResult = lastResult.value,
            isWhileChargingOnly = 0,
            isWifiOnly = 0,
            createdBy = 0,
            folderUnlockToken = null
        )
    }

    companion object {
        private const val ACCOUNT = "account"
        private const val LOCAL_PATH = "/tmp/photo.jpg"
        private const val REMOTE_PATH = "/Photos/photo.jpg"
    }
}
