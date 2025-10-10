/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.utils.ForegroundServiceHelper
import com.owncloud.android.R
import com.owncloud.android.datamodel.ForegroundServiceType
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.FilesSyncHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * This work is triggered when OS detects change in media folders.
 *
 * It fires media detection job and sync job and finishes immediately.
 *
 * This job must not be started on API < 24.
 */
class ContentObserverWork(
    private val context: Context,
    private val params: WorkerParameters,
    private val syncedFolderProvider: SyncedFolderProvider,
    private val powerManagementService: PowerManagementService,
    private val backgroundJobManager: BackgroundJobManager
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "🔍" + "ContentObserverWork"
        private const val CHANNEL_ID = NotificationUtils.NOTIFICATION_CHANNEL_CONTENT_OBSERVER
        private const val NOTIFICATION_ID = 774
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val workerName = BackgroundJobManagerImpl.formatClassTag(this@ContentObserverWork::class)
        backgroundJobManager.logStartOfWorker(workerName)
        Log_OC.d(TAG, "started")

        val notificationTitle = context.getString(R.string.content_observer_work_notification_title)
        val notification = createNotification(notificationTitle)
        updateForegroundInfo(notification)

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

            rescheduleSelf()

            val result = Result.success()
            backgroundJobManager.logEndOfWorker(workerName, result)
            Log_OC.d(TAG, "finished")
            result
        } catch (e: Exception) {
            Log_OC.e(TAG, "❌ Exception in ContentObserverWork: ${e.message}", e)
            Result.retry()
        }
    }

    private suspend fun updateForegroundInfo(notification: Notification) {
        val foregroundInfo = ForegroundServiceHelper.createWorkerForegroundInfo(
            NOTIFICATION_ID,
            notification,
            ForegroundServiceType.DataSync
        )
        setForeground(foregroundInfo)
    }

    private fun createNotification(title: String): Notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle(title)
        .setSmallIcon(R.drawable.ic_find_in_page)
        .setOngoing(true)
        .setSound(null)
        .setVibrate(null)
        .setOnlyAlertOnce(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setSilent(true)
        .build()

    /**
     * Re-schedules this observer to ensure continuous monitoring of media changes.
     */
    private fun rescheduleSelf() {
        Log_OC.d(TAG, "🔁 Rescheduling ContentObserverWork for continued observation.")
        backgroundJobManager.scheduleContentObserverJob()
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

        val contentUris = params.triggeredContentUris.map { uri -> uri.toString() }.toTypedArray()
        Log_OC.d(TAG, "📄 Content uris detected")

        try {
            FilesSyncHelper.startFilesSyncForAllFolders(
                syncedFolderProvider,
                backgroundJobManager,
                false,
                contentUris
            )
            Log_OC.d(TAG, "✅ auto upload triggered successfully for ${contentUris.size} file(s).")
        } catch (e: Exception) {
            Log_OC.e(TAG, "❌ Failed to start auto upload for changed files: ${e.message}", e)
        }
    }
}
