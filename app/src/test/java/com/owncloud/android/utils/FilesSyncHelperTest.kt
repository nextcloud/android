/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.utils

import com.nextcloud.client.jobs.BackgroundJobManager
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderProvider
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class FilesSyncHelperTest {

    private val jobManager: BackgroundJobManager = mock()
    private val provider: SyncedFolderProvider = mock()
    private val enabled: SyncedFolder = mock()
    private val disabled: SyncedFolder = mock()

    @Before
    fun setup() {
        whenever(enabled.isEnabled) doReturn true
        whenever(disabled.isEnabled) doReturn false
        whenever(provider.syncedFolders) doReturn mutableListOf(enabled, disabled)
    }

    @Test
    fun `starts auto upload only for enabled folders, passing override power saving through`() {
        FilesSyncHelper.startAutoUploadForEnabledSyncedFolders(provider, jobManager, true)

        verify(jobManager).startAutoUpload(enabled, true)
        verify(jobManager, never()).startAutoUpload(eq(disabled), any())
    }
}
