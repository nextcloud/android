/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextcloud.client.jobs

import android.content.Context
import android.net.Uri
import androidx.work.WorkerParameters
import com.nextcloud.client.device.PowerManagementService
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.owncloud.android.datamodel.SyncedFolderProvider
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

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
            syncerFolderProvider = folderProvider,
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
