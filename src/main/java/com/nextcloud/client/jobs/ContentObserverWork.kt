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
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.evernote.android.job.JobRequest
import com.evernote.android.job.util.support.PersistableBundleCompat
import com.nextcloud.client.device.PowerManagementService
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.jobs.FilesSyncJob
import com.owncloud.android.jobs.MediaFoldersDetectionJob
import com.owncloud.android.utils.FilesSyncHelper

/**
 * This work is triggered when OS detects change in media folders.
 *
 * It fires media detection job and sync job and finishes immediately.
 *
 * This job must not be started on API < 24.
 */
@RequiresApi(Build.VERSION_CODES.N)
class ContentObserverWork(
    appContext: Context,
    private val params: WorkerParameters,
    private val syncerFolderProvider: SyncedFolderProvider,
    private val powerManagementService: PowerManagementService,
    private val backgroundJobManager: BackgroundJobManager
) : Worker(appContext, params) {

    override fun doWork(): Result {
        if (params.triggeredContentUris.size > 0) {
            checkAndStartFileSyncJob()
            startMediaFolderDetectionJob()
        }
        recheduleSelf()
        return Result.success()
    }

    private fun recheduleSelf() {
        backgroundJobManager.scheduleContentObserverJob()
    }

    private fun checkAndStartFileSyncJob() {
        val syncFolders = syncerFolderProvider.countEnabledSyncedFolders() > 0
        if (!powerManagementService.isPowerSavingEnabled && syncFolders) {
            val persistableBundleCompat = PersistableBundleCompat()
            persistableBundleCompat.putBoolean(FilesSyncJob.SKIP_CUSTOM, true)

            FilesSyncHelper.startFilesSyncJobNow(persistableBundleCompat)
        }
    }

    private fun startMediaFolderDetectionJob() {
        JobRequest.Builder(MediaFoldersDetectionJob.TAG)
            .startNow()
            .setUpdateCurrent(false)
            .build()
            .schedule()
    }
}
