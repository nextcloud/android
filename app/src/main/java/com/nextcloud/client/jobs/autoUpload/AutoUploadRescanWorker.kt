/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.autoUpload

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextcloud.client.jobs.BackgroundJobManager
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.FilesSyncHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AutoUploadRescanWorker(
    context: Context,
    params: WorkerParameters,
    private val syncedFolderProvider: SyncedFolderProvider,
    private val backgroundJobManager: BackgroundJobManager
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "🔄📤" + "AutoUploadRescan"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // cancel enqueued ones so that we can re-start. Some workers may stuck
        // thus we need to have clean start.
        backgroundJobManager.cancelEnqueuedAutoUploads()

        val requestedFolders = FilesSyncHelper.startAutoUploadForEnabledSyncedFoldersIfNotActive(
            syncedFolderProvider,
            backgroundJobManager
        )

        Log_OC.d(TAG, "requested auto upload for $requestedFolders folder(s)")

        Result.success()
    }
}
