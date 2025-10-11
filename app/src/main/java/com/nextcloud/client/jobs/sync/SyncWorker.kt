/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.User
import com.nextcloud.utils.extensions.updateSyncStateOfFolder
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.DownloadFileOperation
import com.owncloud.android.ui.helpers.FileOperationsHelper
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

class SyncWorker(
    private val user: User,
    private val context: Context,
    private val viewThemeUtils: ViewThemeUtils,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncWorker"
        private val _liveSyncStates = MutableStateFlow<Map<Long, SyncState>>(emptyMap())
        val liveSyncStates: StateFlow<Map<Long, SyncState>> = _liveSyncStates

        fun updateLiveSyncState(id: Long, state: SyncState) {
            _liveSyncStates.update { it + (id to state) }
        }

        const val FOLDER_ID = "FOLDER_ID"
    }

    private var notificationManager: SyncWorkerNotificationManager? = null

    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    override suspend fun doWork(): Result {
        val folderID = inputData.getLong(FOLDER_ID, -1)
        if (folderID == -1L) {
            return Result.failure()
        }
        val storageManager = FileDataStorageManager(user, context.contentResolver)
        val folder = storageManager.getFileById(folderID) ?: return Result.failure()
        updateLiveSyncState(folder.fileId, SyncState.SYNCING)

        notificationManager = SyncWorkerNotificationManager(context, viewThemeUtils)

        Log_OC.d(TAG, "SyncWorker started")

        val foregroundInfo = notificationManager?.getForegroundInfo(folder) ?: return Result.failure()
        setForeground(foregroundInfo)

        return withContext(Dispatchers.IO) {
            try {
                val files = getFiles(folder, storageManager)
                val client = getClient()

                var result = true
                files.forEachIndexed { index, file ->
                    if (!checkDiskSize(file)) {
                        return@withContext Result.failure()
                    }

                    updateLiveSyncState(file.fileId, SyncState.SYNCING)

                    withContext(Dispatchers.Main) {
                        notificationManager?.showProgressNotification(
                            folder.fileName,
                            file.fileName,
                            index,
                            files.size
                        )
                    }

                    val syncFileResult = syncFile(file, client)
                    if (syncFileResult) {
                        updateLiveSyncState(file.fileId, SyncState.COMPLETED)
                    } else {
                        updateLiveSyncState(file.fileId, SyncState.FAILED)
                        result = false
                    }
                }

                withContext(Dispatchers.Main) {
                    notificationManager?.showCompletionMessage(folder.fileName, result)
                }

                if (result) {
                    storageManager.updateSyncStateOfFolder(folder, SyncState.COMPLETED)
                    updateLiveSyncState(folder.fileId, SyncState.COMPLETED)
                    Log_OC.d(TAG, "SyncWorker completed")
                    Result.success()
                } else {
                    storageManager.updateSyncStateOfFolder(folder, SyncState.FAILED)
                    updateLiveSyncState(folder.fileId, SyncState.FAILED)
                    Log_OC.d(TAG, "SyncWorker failed")
                    Result.failure()
                }
            } catch (e: Exception) {
                storageManager.updateSyncStateOfFolder(folder, SyncState.FAILED)
                updateLiveSyncState(folder.fileId, SyncState.FAILED)
                Log_OC.d(TAG, "SyncWorker failed reason: $e")
                Result.failure()
            } finally {
                notificationManager?.dismiss()
            }
        }
    }

    private fun getFiles(folder: OCFile, storageManager: FileDataStorageManager): List<OCFile> =
        storageManager.getFolderContent(folder, false)
            .filter { !it.isFolder && !it.isDown }

    @Suppress("DEPRECATION")
    private fun getClient(): OwnCloudClient {
        val account = user.toOwnCloudAccount()
        return OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(account, context)
    }

    private suspend fun checkDiskSize(file: OCFile): Boolean {
        val fileSizeInByte = file.fileLength
        val availableDiskSpace = FileOperationsHelper.getAvailableSpaceOnDevice()

        return if (availableDiskSpace < fileSizeInByte) {
            notificationManager?.showNotAvailableDiskSpace()
            false
        } else {
            true
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun syncFile(file: OCFile, client: OwnCloudClient): Boolean = withContext(Dispatchers.IO) {
        val operation = DownloadFileOperation(user, file, context).execute(client)
        Log_OC.d(TAG, "Syncing file: " + file.decryptedRemotePath)
        operation.isSuccess
    }
}
