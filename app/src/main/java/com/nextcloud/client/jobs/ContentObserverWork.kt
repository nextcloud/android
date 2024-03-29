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
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nextcloud.client.device.PowerManagementService
import com.owncloud.android.datamodel.SyncedFolderProvider

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
