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

import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.upload.FileUploadHelper.Companion.instance
import com.nextcloud.client.network.ConnectivityService
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
        uris: Array<String?>,
        overridePowerSaving: Boolean
    ) {
        Log_OC.d(TAG, "start auto upload worker for each enabled folder")

        provider.syncedFolders.forEach {
            if (it.isEnabled) {
                manager.startAutoUpload(it, overridePowerSaving, uris)
            }
        }
    }
}
