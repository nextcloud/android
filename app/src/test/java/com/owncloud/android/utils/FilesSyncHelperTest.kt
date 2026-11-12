/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils

import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.autoUpload.AutoUploadRequestResult
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class FilesSyncHelperTest {

    private val backgroundJobManager: BackgroundJobManager = mock()

    private fun syncedFolder(id: Long, enabled: Boolean): SyncedFolder = mock {
        on { this.id } doReturn id
        on { isEnabled } doReturn enabled
    }

    private fun provider(vararg folders: SyncedFolder): SyncedFolderProvider = mock {
        on { syncedFolders } doReturn folders.toMutableList()
    }

    @Test
    fun `sync now starts only enabled folders and asks them to ignore power saving`() {
        val enabled = syncedFolder(id = 1, enabled = true)
        val disabled = syncedFolder(id = 2, enabled = false)

        val result = FilesSyncHelper.startAutoUploadIgnoringPowerSaving(
            provider(enabled, disabled),
            backgroundJobManager
        )

        assertEquals(AutoUploadRequestResult.STARTED, result)
        verify(backgroundJobManager).startAutoUpload(enabled, true)
        verify(backgroundJobManager, never()).startAutoUpload(disabled, true)
    }

    @Test
    fun `sync now leaves a folder alone that already ignores power saving`() {
        val running = syncedFolder(id = 1, enabled = true)
        whenever(backgroundJobManager.isAutoUploadIgnoringPowerSavingScheduled(running.id)).thenReturn(true)

        val result = FilesSyncHelper.startAutoUploadIgnoringPowerSaving(provider(running), backgroundJobManager)

        assertEquals(AutoUploadRequestResult.ALREADY_RUNNING, result)
        verify(backgroundJobManager, never()).startAutoUpload(any(), any())
    }

    @Test
    fun `sync now reports that there is nothing to upload without an enabled folder`() {
        val result = FilesSyncHelper.startAutoUploadIgnoringPowerSaving(
            provider(syncedFolder(id = 1, enabled = false)),
            backgroundJobManager
        )

        assertEquals(AutoUploadRequestResult.NO_ENABLED_FOLDER, result)
        verify(backgroundJobManager, never()).startAutoUpload(any(), any())
    }

    @Test
    fun `scheduled runs keep the power saving check enabled`() {
        val enabled = syncedFolder(id = 1, enabled = true)

        val startedFolderCount = FilesSyncHelper.startAutoUploadForEnabledSyncedFolders(
            provider(enabled),
            backgroundJobManager,
            overridePowerSaving = false
        )

        assertEquals(1, startedFolderCount)
        verify(backgroundJobManager).startAutoUpload(enabled, false)
    }

    @Test
    fun `rescan preserves scheduled folder work and starts only idle enabled folders`() {
        val scheduled = syncedFolder(id = 1, enabled = true)
        val idle = syncedFolder(id = 2, enabled = true)
        val disabled = syncedFolder(id = 3, enabled = false)
        whenever(backgroundJobManager.isAutoUploadScheduled(scheduled.id)).thenReturn(true)

        val count = FilesSyncHelper.startAutoUploadForEnabledSyncedFoldersIfNotActive(
            provider(scheduled, idle, disabled),
            backgroundJobManager
        )

        assertEquals(1, count)
        verify(backgroundJobManager, never()).startAutoUpload(scheduled, false)
        verify(backgroundJobManager, never()).startAutoUpload(disabled, false)
        verify(backgroundJobManager).startAutoUpload(idle, false)
    }
}
