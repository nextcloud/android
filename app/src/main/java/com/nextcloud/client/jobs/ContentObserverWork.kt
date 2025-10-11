/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nextcloud.client.device.PowerManagementService
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.FilesSyncHelper

/**
 * This work is triggered when OS detects change in media folders.
 *
 * It fires media detection job and sync job and finishes immediately.
 *
 * This job must not be started on API < 24.
 */
class ContentObserverWork(
    appContext: Context,
    private val params: WorkerParameters,
    private val syncedFolderProvider: SyncedFolderProvider,
    private val powerManagementService: PowerManagementService,
    private val backgroundJobManager: BackgroundJobManager
) : Worker(appContext, params) {

    override fun doWork(): Result {
        backgroundJobManager.logStartOfWorker(BackgroundJobManagerImpl.formatClassTag(this::class))

        if (params.triggeredContentUris.isNotEmpty()) {
            Log_OC.d(TAG, "File-sync Content Observer detected files change")
            checkAndTriggerAutoUpload()
            backgroundJobManager.startMediaFoldersDetectionJob()
        } else {
            Log_OC.d(TAG, "triggeredContentUris empty")
        }
        recheduleSelf()

        val result = Result.success()
        backgroundJobManager.logEndOfWorker(BackgroundJobManagerImpl.formatClassTag(this::class), result)
        return result
    }

    private fun recheduleSelf() {
        backgroundJobManager.scheduleContentObserverJob()
    }

    private fun checkAndTriggerAutoUpload() {
        if (!powerManagementService.isPowerSavingEnabled && syncedFolderProvider.countEnabledSyncedFolders() > 0) {
            val contentUris = mutableListOf<String>()
            for (uri in params.triggeredContentUris) {
                // adds uri strings e.g. content://media/external/images/media/2281
                contentUris.add(uri.toString())
            }
            FilesSyncHelper.startAutoUploadImmediatelyWithContentUris(
                syncedFolderProvider,
                backgroundJobManager,
                false,
                contentUris.toTypedArray()
            )
        } else {
            Log_OC.w(TAG, "cant startAutoUploadImmediatelyWithContentUris")
        }
    }

    companion object {
        val TAG: String = ContentObserverWork::class.java.simpleName
    }
}
