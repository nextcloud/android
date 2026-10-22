/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment.autoUpload.data

import com.nextcloud.client.jobs.BackgroundJobManager
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderDisplayItem
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.utils.FilesSyncHelper

class AutoUploadPersistence(
    private val syncedFolderProvider: SyncedFolderProvider,
    private val backgroundJobManager: BackgroundJobManager
) {

    fun saveOrUpdateAutoUploadFolder(item: SyncedFolderDisplayItem) {
        if (item.id == SyncedFolder.UNPERSISTED_ID) {
            storeAutoUploadFolder(item)
            return
        }

        syncedFolderProvider.updateSyncFolder(item)
        applySyncState(item)
    }

    fun persistSyncStatus(item: SyncedFolderDisplayItem) {
        if (item.id > SyncedFolder.UNPERSISTED_ID) {
            syncedFolderProvider.updateSyncedFolderEnabled(item.id, item.isEnabled)
            return
        }

        val storedId = syncedFolderProvider.storeSyncedFolder(item)
        if (storedId != INVALID_ID) {
            item.id = storedId
        }
    }

    fun deleteAutoUploadFolder(id: Long) {
        syncedFolderProvider.deleteSyncedFolder(id)
    }

    private fun storeAutoUploadFolder(item: SyncedFolderDisplayItem) {
        val storedId = syncedFolderProvider.storeSyncedFolder(item)
        if (storedId == INVALID_ID) {
            return
        }

        item.id = storedId
        applySyncState(item)
    }

    private fun applySyncState(item: SyncedFolderDisplayItem) {
        if (item.isEnabled) {
            backgroundJobManager.startAutoUpload(item, overridePowerSaving = false)
            return
        }

        ArbitraryDataProviderImpl(MainApp.getAppContext())
            .deleteKeyForAccount(FilesSyncHelper.GLOBAL, MISSPELLED_SYNCED_FOLDER_INITIATED_PREFIX + item.id)
    }

    companion object {
        private const val INVALID_ID = -1L
        private const val MISSPELLED_SYNCED_FOLDER_INITIATED_PREFIX = "syncedFolderIntitiated_"
    }
}
