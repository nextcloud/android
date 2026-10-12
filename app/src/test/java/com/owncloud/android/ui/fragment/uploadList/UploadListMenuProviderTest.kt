/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.uploadList

import android.content.Context
import android.view.MenuItem
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.UploadsStorageManager
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class UploadListMenuProviderTest {

    private val preferences: AppPreferences = mock {
        on { isGlobalUploadPaused() } doReturn true
    }
    private val backgroundJobManager: BackgroundJobManager = mock()
    private val uploadHelper: FileUploadHelper = mock()
    private val enabledFolder: SyncedFolder = mock {
        on { id } doReturn ENABLED_FOLDER_ID
        on { isEnabled } doReturn true
    }
    private val syncedFolderProvider: SyncedFolderProvider = mock {
        on { syncedFolders } doReturn mutableListOf(enabledFolder)
    }
    private val menuItem: MenuItem = mock {
        on { itemId } doReturn R.id.action_toggle_global_pause
    }

    private val menuProvider = UploadListMenuProvider(
        mock<Context>(),
        preferences,
        mock<UserAccountManager>(),
        mock<UploadsStorageManager>(),
        syncedFolderProvider,
        backgroundJobManager,
        { uploadHelper }
    ) {}

    @Test
    fun `resuming global pause starts auto upload of enabled folders without waiting for the rescan`() {
        menuProvider.onMenuItemSelected(menuItem)

        verify(backgroundJobManager).startAutoUpload(enabledFolder, false)
    }

    private companion object {
        const val ENABLED_FOLDER_ID = 1L
    }
}
