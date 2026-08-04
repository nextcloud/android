/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils

import com.nextcloud.client.jobs.BackgroundJobManager
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class FilesSyncHelperTest {

    private val backgroundJobManager: BackgroundJobManager = mock()

    private fun syncedFolder(enabled: Boolean): SyncedFolder = mock {
        on { isEnabled } doReturn enabled
    }

    private fun provider(vararg folders: SyncedFolder): SyncedFolderProvider = mock {
        on { syncedFolders } doReturn folders.toMutableList()
    }

    @Test
    fun `sync now starts only enabled folders and asks them to ignore power saving`() {
        val enabled = syncedFolder(enabled = true)
        val disabled = syncedFolder(enabled = false)

        val startedFolderCount = FilesSyncHelper.startAutoUploadForEnabledSyncedFolders(
            provider(enabled, disabled),
            backgroundJobManager,
            overridePowerSaving = true
        )

        assertEquals(1, startedFolderCount)
        verify(backgroundJobManager).startAutoUpload(enabled, true)
        verify(backgroundJobManager, never()).startAutoUpload(disabled, true)
    }

    @Test
    fun `scheduled runs keep the power saving check enabled`() {
        val enabled = syncedFolder(enabled = true)

        val startedFolderCount = FilesSyncHelper.startAutoUploadForEnabledSyncedFolders(
            provider(enabled),
            backgroundJobManager,
            overridePowerSaving = false
        )

        assertEquals(1, startedFolderCount)
        verify(backgroundJobManager).startAutoUpload(enabled, false)
    }

    @Test
    fun `no enabled folder reports nothing to sync`() {
        val startedFolderCount = FilesSyncHelper.startAutoUploadForEnabledSyncedFolders(
            provider(syncedFolder(enabled = false)),
            backgroundJobManager,
            overridePowerSaving = true
        )

        assertEquals(0, startedFolderCount)
        verify(backgroundJobManager, never()).startAutoUpload(any(), any())
    }
}
