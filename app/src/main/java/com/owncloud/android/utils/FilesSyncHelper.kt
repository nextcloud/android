/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Jonas Mayer <jonas.mayer@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.autoUpload.AutoUploadRequestResult
import com.nextcloud.client.jobs.upload.FileUploadHelper.Companion.instance
import com.nextcloud.client.network.ConnectivityService
import com.owncloud.android.datamodel.SyncedFolder
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.lib.common.utils.Log_OC

object FilesSyncHelper {
    private const val TAG: String = "FileSyncHelper"
    const val GLOBAL: String = "global"

    @JvmStatic
    fun restartUploadsIfNeeded(
        uploadsStorageManager: UploadsStorageManager,
        accountManager: UserAccountManager,
        connectivityService: ConnectivityService,
        powerManagementService: PowerManagementService
    ) {
        Log_OC.d(TAG, "restartUploadsIfNeeded, called")
        instance().retryFailedUploads(
            uploadsStorageManager,
            connectivityService,
            accountManager,
            powerManagementService
        )
    }

    @JvmStatic
    fun startAutoUploadForEnabledSyncedFolders(
        provider: SyncedFolderProvider,
        manager: BackgroundJobManager,
        overridePowerSaving: Boolean
    ): Int {
        Log_OC.d(TAG, "start auto upload worker for each enabled folder")

        return provider.syncedFolders
            .filter { it.isEnabled }
            .onEach { manager.startAutoUpload(it, overridePowerSaving) }
            .size
    }

    fun startAutoUploadIgnoringPowerSaving(
        provider: SyncedFolderProvider,
        manager: BackgroundJobManager
    ): AutoUploadRequestResult {
        val enabledFolders = provider.syncedFolders.filter { it.isEnabled }
        if (enabledFolders.isEmpty()) {
            Log_OC.d(TAG, "no enabled synced folder to start")
            return AutoUploadRequestResult.NO_ENABLED_FOLDER
        }

        val startedFolders = enabledFolders
            .filterNot { manager.isAutoUploadIgnoringPowerSavingScheduled(it.id) }
            .onEach { manager.startAutoUpload(it, overridePowerSaving = true) }

        Log_OC.d(TAG, "start auto upload ignoring power saving for ${startedFolders.size} folder(s)")

        return if (startedFolders.isEmpty()) {
            AutoUploadRequestResult.ALREADY_RUNNING
        } else {
            AutoUploadRequestResult.STARTED
        }
    }

    @JvmStatic
    fun startLocalDeletionForEnabledSyncedFolders(provider: SyncedFolderProvider, manager: BackgroundJobManager) {
        Log_OC.d(TAG, "Start local deletion worker for each enabled folder by any user")

        manager.locallyDeleteAutoUploadedFiles(provider.syncedFolders)
    }

    @JvmStatic
    fun startLocalDeletionForEnabledSyncedFolders(
        provider: SyncedFolderProvider,
        manager: BackgroundJobManager,
        user: User
    ) {
        Log_OC.d(TAG, "start local deletion worker for each enabled folder under user ${user.accountName}")

        manager.locallyDeleteAutoUploadedFiles(
            provider.syncedFolders.filter {
                it.account.equals(user.accountName)
            }
        )
    }

    @JvmStatic
    fun startLocalDeletionForSyncedFolder(folder: SyncedFolder, manager: BackgroundJobManager) {
        Log_OC.d(TAG, "start local deletion worker for folder ${folder.localPath}")

        manager.locallyDeleteAutoUploadedFiles(listOf(folder))
    }
}
