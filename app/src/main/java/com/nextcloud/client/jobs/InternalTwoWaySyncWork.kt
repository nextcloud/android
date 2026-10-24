/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Tobias Kaminsky <tobias.kaminsky@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.notification.WorkerNotificationManager
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.SynchronizeFolderOperation
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.FileStorageUtils
import java.io.File
import java.util.concurrent.TimeUnit

@Suppress("Detekt.NestedBlockDepth", "ReturnCount", "LongParameterList")
class InternalTwoWaySyncWork(
    private val context: Context,
    params: WorkerParameters,
    private val userAccountManager: UserAccountManager,
    private val powerManagementService: PowerManagementService,
    private val connectivityService: ConnectivityService,
    private val appPreferences: AppPreferences
) : Worker(context, params) {
    @Volatile
    private var shouldRun = true

    @Volatile
    private var operation: SynchronizeFolderOperation? = null

    override fun doWork(): Result {
        Log_OC.d(TAG, "Worker started!")

        var result = true

        val constraint = when {
            !appPreferences.isTwoWaySyncEnabled -> "disabled"
            powerManagementService.isPowerSavingEnabled -> "power-saving"
            !connectivityService.isConnected -> "disconnected"
            !connectivityService.connectivity.isWifi -> "not-wifi"
            connectivityService.isInternetWalled() -> "server-unavailable"
            else -> null
        }
        if (constraint != null) {
            Log_OC.i(TAG, "SyncTrace skipped reason=$constraint")
            return Result.success()
        }

        val users = userAccountManager.allUsers

        for (user in users) {
            val fileDataStorageManager = FileDataStorageManager(user, context.contentResolver)
            val folders = fileDataStorageManager.getInternalTwoWaySyncFolders(user)

            for (folder in folders) {
                if (!shouldRun) {
                    Log_OC.d(TAG, "Worker was stopped!")
                    return Result.failure()
                }

                checkFreeSpace(folder)?.let { checkFreeSpaceResult ->
                    return checkFreeSpaceResult
                }

                if (!startForeground(folder)) return Result.failure()
                if (!synchronizeFolder(folder, user, fileDataStorageManager)) result = false
            }
        }

        return if (result) {
            Log_OC.d(TAG, "Worker finished with success!")
            Result.success()
        } else {
            Log_OC.d(TAG, "Worker finished with failure!")
            Result.failure()
        }
    }

    private fun synchronizeFolder(folder: OCFile, user: User, fileDataStorageManager: FileDataStorageManager): Boolean {
        val startedAt = System.nanoTime()
        Log_OC.i(TAG, "SyncTrace root start folder=${folder.remotePath}")
        operation =
            SynchronizeFolderOperation.forOngoingSync(
                context,
                folder.remotePath,
                user,
                fileDataStorageManager
            ) { canContinueSynchronization() }
        if (!shouldRun) {
            operation?.cancel()
            return false
        }
        val operationResult = operation?.execute(context)
        val elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
        Log_OC.i(
            TAG,
            "SyncTrace root end folder=${folder.remotePath} elapsedMs=$elapsedMs " +
                "result=${operationResult?.code}"
        )

        if (operationResult?.isSuccess == true) {
            Log_OC.d(TAG, "Folder ${folder.remotePath}: finished!")
        } else {
            Log_OC.d(TAG, "Folder ${folder.remotePath} failed!")
        }

        fileDataStorageManager.getFileByEncryptedRemotePath(folder.remotePath)?.let { currentFolder ->
            operationResult?.let {
                currentFolder.internalFolderSyncResult = it.code.toString()
            }
            currentFolder.internalFolderSyncTimestamp = System.currentTimeMillis()
            fileDataStorageManager.saveFile(currentFolder)
        }
        return operationResult?.isSuccess == true
    }

    private fun canContinueSynchronization(): Boolean = shouldRun && appPreferences.isTwoWaySyncEnabled &&
        !powerManagementService.isPowerSavingEnabled && connectivityService.isConnected &&
        connectivityService.connectivity.isWifi

    override fun onStopped() {
        Log_OC.d(TAG, "OnStopped of worker called!")
        operation?.cancel()
        shouldRun = false
        super.onStopped()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun startForeground(folder: OCFile): Boolean = try {
        val notifications = WorkerNotificationManager(
            SYNC_NOTIFICATION_ID,
            context,
            null,
            R.string.folder_download_worker_ticker_id,
            NotificationUtils.NOTIFICATION_CHANNEL_DOWNLOAD
        )
        val notification = notifications.createSilentNotification(folder.fileName, R.drawable.ic_sync)
        setForegroundAsync(notifications.getForegroundInfo(notification)).get()
        true
    } catch (e: Exception) {
        if (e is InterruptedException) Thread.currentThread().interrupt()
        Log_OC.e(TAG, "Could not start foreground folder synchronization", e)
        false
    }

    @Suppress("TooGenericExceptionCaught")
    private fun checkFreeSpace(folder: OCFile): Result? {
        val storagePath = folder.storagePath ?: MainApp.getStoragePath()
        val file = File(storagePath)

        if (!file.exists()) return null

        return try {
            val freeSpaceLeft = file.freeSpace
            val localFolder = File(storagePath, MainApp.getDataFolder())
            val localFolderSize = FileStorageUtils.getFolderSize(localFolder)
            val remoteFolderSize = folder.fileLength

            if (freeSpaceLeft < (remoteFolderSize - localFolderSize)) {
                Log_OC.d(TAG, "Not enough space left!")
                Result.failure()
            } else {
                null
            }
        } catch (e: Exception) {
            Log_OC.d(TAG, "Error caught at checkFreeSpace: $e")
            null
        }
    }

    companion object {
        private const val SYNC_NOTIFICATION_ID = 392
        const val TAG = "InternalTwoWaySyncWork"
    }
}
