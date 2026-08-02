/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.autoUpload

import com.nextcloud.client.database.dao.FileSystemDao
import com.nextcloud.client.database.dao.UploadDao
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.preferences.SubFolderRule
import com.owncloud.android.datamodel.MediaFolderType
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus
import com.owncloud.android.files.services.NameCollisionPolicy
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AutoUploadRescanHelperTest {

    companion object {
        private const val ACCOUNT = "user@nextcloud.test"
        private const val OTHER_ACCOUNT = "other@nextcloud.test"
        private const val CAMERA_PATH = "/storage/emulated/0/DCIM/Camera"
        private const val ENABLED_FOLDER_ID = 1L
        private const val DISABLED_FOLDER_ID = 2L
        private const val FOREIGN_FOLDER_ID = 3L
        private const val LAST_SCAN_TIMESTAMP_MS = 1_700_000_000_000L
    }

    @Mock
    lateinit var fileSystemDao: FileSystemDao

    @Mock
    lateinit var uploadDao: UploadDao

    @Mock
    lateinit var syncedFolderProvider: SyncedFolderProvider

    @Mock
    lateinit var backgroundJobManager: BackgroundJobManager

    private lateinit var helper: AutoUploadRescanHelper

    @Before
    fun setUp() = runBlocking {
        MockitoAnnotations.openMocks(this@AutoUploadRescanHelperTest)
        whenever(fileSystemDao.requeueFilesWithoutSuccessfulUpload(any(), any())).thenReturn(0)
        whenever(uploadDao.deleteUnsuccessfulUploadsInFolder(any(), any(), any())).thenReturn(0)
        helper = AutoUploadRescanHelper(fileSystemDao, uploadDao, syncedFolderProvider, backgroundJobManager)
    }

    @Test
    fun `rescan queues files without successful upload and starts a full rescan`() = runBlocking {
        val folder = syncedFolder(ENABLED_FOLDER_ID, CAMERA_PATH, ACCOUNT, enabled = true)
        whenever(syncedFolderProvider.syncedFolders).thenReturn(mutableListOf(folder))

        val rescannedFolders = helper.rescan(ACCOUNT)

        assertEquals(1, rescannedFolders)
        verify(fileSystemDao).requeueFilesWithoutSuccessfulUpload(
            syncedFolderId = ENABLED_FOLDER_ID.toString(),
            succeededStatus = UploadStatus.UPLOAD_SUCCEEDED.value
        )
        verify(uploadDao).deleteUnsuccessfulUploadsInFolder(
            accountName = ACCOUNT,
            localPathPrefix = "$CAMERA_PATH/",
            succeededStatus = UploadStatus.UPLOAD_SUCCEEDED.value
        )
        verify(backgroundJobManager).startAutoUpload(
            syncedFolder = folder,
            overridePowerSaving = true,
            fullRescan = true
        )
    }

    @Test
    fun `rescan forgets the last scan so the worker does not skip unchanged files`() = runBlocking {
        val folder = syncedFolder(ENABLED_FOLDER_ID, CAMERA_PATH, ACCOUNT, enabled = true)
        whenever(syncedFolderProvider.syncedFolders).thenReturn(mutableListOf(folder))

        helper.rescan(ACCOUNT)

        assertEquals(SyncedFolder.NOT_SCANNED_YET, folder.lastScanTimestampMs)
        verify(syncedFolderProvider).updateSyncFolder(folder)
    }

    @Test
    fun `rescan skips disabled folders and folders of other accounts`() = runBlocking {
        val enabledFolder = syncedFolder(ENABLED_FOLDER_ID, CAMERA_PATH, ACCOUNT, enabled = true)
        val disabledFolder = syncedFolder(DISABLED_FOLDER_ID, "$CAMERA_PATH/Disabled", ACCOUNT, enabled = false)
        val foreignFolder = syncedFolder(FOREIGN_FOLDER_ID, "$CAMERA_PATH/Foreign", OTHER_ACCOUNT, enabled = true)
        whenever(syncedFolderProvider.syncedFolders)
            .thenReturn(mutableListOf(enabledFolder, disabledFolder, foreignFolder))

        val rescannedFolders = helper.rescan(ACCOUNT)

        assertEquals(1, rescannedFolders)
        verify(fileSystemDao, never()).requeueFilesWithoutSuccessfulUpload(
            eq(DISABLED_FOLDER_ID.toString()),
            any()
        )
        verify(fileSystemDao, never()).requeueFilesWithoutSuccessfulUpload(
            eq(FOREIGN_FOLDER_ID.toString()),
            any()
        )
        verify(backgroundJobManager).startAutoUpload(eq(enabledFolder), eq(true), eq(true))
    }

    @Test
    fun `rescan still starts the worker when the local path is unusable`() = runBlocking {
        val folder = syncedFolder(ENABLED_FOLDER_ID, "", ACCOUNT, enabled = true)
        whenever(syncedFolderProvider.syncedFolders).thenReturn(mutableListOf(folder))

        val rescannedFolders = helper.rescan(ACCOUNT)

        assertEquals(1, rescannedFolders)
        verify(uploadDao, never()).deleteUnsuccessfulUploadsInFolder(any(), any(), any())
        verify(backgroundJobManager).startAutoUpload(eq(folder), eq(true), eq(true))
    }

    private fun syncedFolder(id: Long, localPath: String, account: String, enabled: Boolean): SyncedFolder =
        SyncedFolder(
            id,
            localPath,
            "/InstantUpload",
            true,
            false,
            true,
            false,
            account,
            0,
            NameCollisionPolicy.ASK_USER.serialize(),
            enabled,
            LAST_SCAN_TIMESTAMP_MS,
            MediaFolderType.IMAGE,
            false,
            SubFolderRule.YEAR_MONTH,
            false,
            LAST_SCAN_TIMESTAMP_MS
        )
}
