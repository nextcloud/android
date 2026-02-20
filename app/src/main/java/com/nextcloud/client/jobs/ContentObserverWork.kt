/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.autoUpload.AutoUploadHelper
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.FilesSyncHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * This work is triggered when OS detects change in media folders.
 *
 * It fires media detection worker and auto upload worker and finishes immediately.
 *
 */
@Suppress("TooGenericExceptionCaught")
class ContentObserverWork(
    context: Context,
    private val params: WorkerParameters,
    private val syncedFolderProvider: SyncedFolderProvider,
    private val powerManagementService: PowerManagementService,
    private val backgroundJobManager: BackgroundJobManager,
    private val autoUploadHelper: AutoUploadHelper
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "🔍" + "ContentObserverWork"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val workerName = BackgroundJobManagerImpl.formatClassTag(this@ContentObserverWork::class)
        backgroundJobManager.logStartOfWorker(workerName)
        Log_OC.d(TAG, "started")

        try {
            if (params.triggeredContentUris.isNotEmpty()) {
                Log_OC.d(TAG, "📸 content observer detected file changes.")
                checkAndTriggerAutoUpload()

                // prevent worker fail because of another worker
                try {
                    backgroundJobManager.startMediaFoldersDetectionJob()
                } catch (e: Exception) {
                    Log_OC.d(TAG, "⚠️ media folder detection job failed :$e")
                }
            } else {
                Log_OC.d(TAG, "⚠️ triggeredContentUris is empty — nothing to sync.")
            }

            val result = Result.success()
            backgroundJobManager.logEndOfWorker(workerName, result)
            Log_OC.d(TAG, "finished")
            result
        } catch (e: Exception) {
            Log_OC.e(TAG, "❌ Exception in ContentObserverWork: ${e.message}", e)
            Result.retry()
        } finally {
            Log_OC.d(TAG, "🔄" + "re-scheduling job")
            backgroundJobManager.scheduleContentObserverJob()
        }
    }

    private suspend fun checkAndTriggerAutoUpload() = withContext(Dispatchers.IO) {
        if (powerManagementService.isPowerSavingEnabled) {
            Log_OC.w(TAG, "⚡ Power saving mode active — skipping file sync.")
            return@withContext
        }

        val enabledFoldersCount = syncedFolderProvider.countEnabledSyncedFolders()
        if (enabledFoldersCount <= 0) {
            Log_OC.w(TAG, "🚫 No enabled synced folders found — skipping file sync.")
            return@withContext
        }

        val contentUris = params.triggeredContentUris.map { uri ->
            // adds uri strings e.g. content://media/external/images/media/2281
            uri.toString()
        }.toTypedArray<String>()

        Log_OC.d(TAG, "📄 Content uris detected")

        try {
            syncedFolderProvider.syncedFolders.forEach {
                if (it.isEnabled) {
                    if (contentUris.isEmpty()) {
                        Log_OC.d(TAG, "inserting all entries")
                        autoUploadHelper.insertEntries(it)
                    } else {
                        Log_OC.d(TAG, "inserting changed entries")
                        val isContentUrisStored = autoUploadHelper.insertChangedEntries(it, contentUris)
                        if (!isContentUrisStored) {
                            Log_OC.w(
                                TAG,
                                "changed content uris not stored, fallback to insert all db entries to not lose files"
                            )
                            autoUploadHelper.insertEntries(it)
                        }
                    }
                }
            }

            FilesSyncHelper.startAutoUploadForEnabledSyncedFolders(
                syncedFolderProvider,
                backgroundJobManager,
                false
            )
            Log_OC.d(TAG, "✅ auto upload triggered successfully for ${contentUris.size} file(s).")
        } catch (e: Exception) {
            Log_OC.e(TAG, "❌ Failed to start auto upload for changed files: ${e.message}", e)
        }
    }
}
