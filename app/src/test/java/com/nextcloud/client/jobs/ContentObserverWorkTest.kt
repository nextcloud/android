/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.nextcloud.client.jobs

import android.content.Context
import android.net.Uri
import androidx.work.WorkerParameters
import com.nextcloud.client.device.PowerManagementService
import com.owncloud.android.datamodel.SyncedFolderProvider
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

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        worker = ContentObserverWork(
            appContext = context,
            params = params,
            syncedFolderProvider = folderProvider,
            powerManagementService = powerManagementService,
            backgroundJobManager = backgroundJobManager
        )
        val uri: Uri = Mockito.mock(Uri::class.java)
        whenever(params.triggeredContentUris).thenReturn(listOf(uri))
    }

    @Test
    fun job_reschedules_self_after_each_run_unconditionally() {
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

    @Test
    @Ignore("TODO: needs further refactoring")
    fun sync_is_triggered() {
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

    @Test
    @Ignore("TODO: needs further refactoring")
    fun sync_is_not_triggered_under_power_saving_mode() {
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

    @Test
    @Ignore("TODO: needs further refactoring")
    fun sync_is_not_triggered_if_no_folder_are_synced() {
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
