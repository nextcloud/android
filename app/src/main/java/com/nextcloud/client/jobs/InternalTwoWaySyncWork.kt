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
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.network.ConnectivityService
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.SynchronizeFolderOperation
import com.owncloud.android.utils.FileStorageUtils
import java.io.File

@Suppress("Detekt.NestedBlockDepth", "ReturnCount")
class InternalTwoWaySyncWork(
    private val context: Context,
    params: WorkerParameters,
    private val userAccountManager: UserAccountManager,
    private val powerManagementService: PowerManagementService,
    private val connectivityService: ConnectivityService
) : Worker(context, params) {
    override fun doWork(): Result {
        Log_OC.d(TAG, "Worker started!")

        var result = true

        if (powerManagementService.isPowerSavingEnabled ||
            !connectivityService.isConnected || connectivityService.isInternetWalled
        ) {
            Log_OC.d(TAG, "Not starting due to constraints!")
            return Result.success()
        }

        val users = userAccountManager.allUsers

        for (user in users) {
            val fileDataStorageManager = FileDataStorageManager(user, context.contentResolver)
            val folders = fileDataStorageManager.getInternalTwoWaySyncFolders(user)

            for (folder in folders) {
                checkFreeSpace(folder)?.let { checkFreeSpaceResult ->
                    return checkFreeSpaceResult
                }

                Log_OC.d(TAG, "Folder ${folder.remotePath}: started!")
                val operation = SynchronizeFolderOperation(context, folder.remotePath, user, fileDataStorageManager)
                    .execute(context)

                if (operation.isSuccess) {
                    Log_OC.d(TAG, "Folder ${folder.remotePath}: finished!")
                } else {
                    Log_OC.d(TAG, "Folder ${folder.remotePath} failed!")
                    result = false
                }

                folder.apply {
                    internalFolderSyncResult = operation.code.toString()
                    internalFolderSyncTimestamp = System.currentTimeMillis()
                }

                fileDataStorageManager.saveFile(folder)
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
        const val TAG = "InternalTwoWaySyncWork"
    }
}
