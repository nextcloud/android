/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.nextcloud.client.jobs

import android.content.Context
import android.net.Uri
import androidx.work.WorkerParameters
import com.nextcloud.client.database.dao.FileSystemDao
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.autoUpload.AutoUploadHelper
import com.nextcloud.client.jobs.autoUpload.FileSystemRepository
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.UploadsStorageManager
import io.mockk.clearAllMocks
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ContentObserverWorkTest {

    private lateinit var worker: ContentObserverWork

    @Mock
    lateinit var params: WorkerParameters

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var folderProvider: SyncedFolderProvider

    @Mock
    lateinit var powerManagementService: PowerManagementService

    @Mock
    lateinit var backgroundJobManager: BackgroundJobManager

    private val mockDao: FileSystemDao = mockk(relaxed = true)
    private val mockContext: Context = mockk(relaxed = true)
    private val mockUploadsStorageManager: UploadsStorageManager = mockk(relaxed = true)

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        val repo = FileSystemRepository(mockDao, mockUploadsStorageManager, mockContext)
        val helper = AutoUploadHelper(repo)

        worker = ContentObserverWork(
            context = context,
            params = params,
            syncedFolderProvider = folderProvider,
            powerManagementService = powerManagementService,
            backgroundJobManager = backgroundJobManager,
            autoUploadHelper = helper
        )
        val uri: Uri = Mockito.mock(Uri::class.java)
        whenever(params.triggeredContentUris).thenReturn(listOf(uri))
    }

    @After
    fun cleanup() {
        clearAllMocks()
    }

    @Test
    fun job_reschedules_self_after_each_run_unconditionally() {
        runBlocking {
            // GIVEN
            //      nothing to sync
            whenever(params.triggeredContentUris).thenReturn(emptyList())

            // WHEN
            //      worker is called
            worker.doWork()

            // THEN
            //      worker reschedules itself unconditionally
            verify(backgroundJobManager).scheduleContentObserverJob()
        }
    }

    @Test
    @Ignore("TODO: needs further refactoring")
    fun sync_is_triggered() {
        runBlocking {
            // GIVEN
            //      power saving is disabled
            //      some folders are configured for syncing
            whenever(powerManagementService.isPowerSavingEnabled).thenReturn(false)
            whenever(folderProvider.countEnabledSyncedFolders()).thenReturn(1)

            // WHEN
            //      worker is called
            worker.doWork()

            // THEN
            //      sync job is scheduled
            // TO DO: verify(backgroundJobManager).sheduleFilesSync() or something like this
        }
    }

    @Test
    @Ignore("TODO: needs further refactoring")
    fun sync_is_not_triggered_under_power_saving_mode() {
        runBlocking {
            // GIVEN
            //      power saving is enabled
            //      some folders are configured for syncing
            whenever(powerManagementService.isPowerSavingEnabled).thenReturn(true)
            whenever(folderProvider.countEnabledSyncedFolders()).thenReturn(1)

            // WHEN
            //      worker is called
            worker.doWork()

            // THEN
            //      sync job is scheduled
            // TO DO: verify(backgroundJobManager, never()).sheduleFilesSync() or something like this)
        }
    }

    @Test
    @Ignore("TODO: needs further refactoring")
    fun sync_is_not_triggered_if_no_folder_are_synced() {
        runBlocking {
            // GIVEN
            //      power saving is disabled
            //      no folders configured for syncing
            whenever(powerManagementService.isPowerSavingEnabled).thenReturn(false)
            whenever(folderProvider.countEnabledSyncedFolders()).thenReturn(0)

            // WHEN
            //      worker is called
            worker.doWork()

            // THEN
            //      sync job is scheduled
            // TO DO: verify(backgroundJobManager, never()).sheduleFilesSync() or something like this)
        }
    }
}
