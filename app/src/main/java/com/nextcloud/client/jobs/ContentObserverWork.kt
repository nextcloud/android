/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.jobs

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nextcloud.client.device.PowerManagementService
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.lib.common.utils.Log_OC

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
    private val syncerFolderProvider: SyncedFolderProvider,
    private val powerManagementService: PowerManagementService,
    private val backgroundJobManager: BackgroundJobManager
) : Worker(appContext, params) {

    override fun doWork(): Result {
        backgroundJobManager.logStartOfWorker(BackgroundJobManagerImpl.formatClassTag(this::class))

        if (params.triggeredContentUris.size > 0) {
            Log_OC.d(TAG,"FILESYNC Content Observer detected files change")
            checkAndStartFileSyncJob()
            backgroundJobManager.startMediaFoldersDetectionJob()
        }
        recheduleSelf()

        val result = Result.success()
        backgroundJobManager.logEndOfWorker(BackgroundJobManagerImpl.formatClassTag(this::class), result)
        return result
    }

    private fun recheduleSelf() {
        backgroundJobManager.scheduleContentObserverJob()
    }

    private fun checkAndStartFileSyncJob() {
        val syncFolders = syncerFolderProvider.countEnabledSyncedFolders() > 0
        if (!powerManagementService.isPowerSavingEnabled && syncFolders) {
            val changedFiles = mutableListOf<String>()
            for (uri in params.triggeredContentUris) {
                changedFiles.add(uri.toString())
            }
            backgroundJobManager.startImmediateFilesSyncJob(false, changedFiles.toTypedArray())
        }
    }

    companion object {
        val TAG: String = ContentObserverWork::class.java.simpleName
    }
}
