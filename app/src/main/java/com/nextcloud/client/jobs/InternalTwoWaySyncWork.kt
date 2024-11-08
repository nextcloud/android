/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Tobias Kaminsky <tobias.kaminsky@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.DownloadFileOperation
import com.owncloud.android.utils.FileStorageUtils
import java.io.File

@Suppress("Detekt.NestedBlockDepth", "ReturnCount", "LongParameterList")
class InternalTwoWaySyncWork(
    private val context: Context,
    params: WorkerParameters,
    private val userAccountManager: UserAccountManager,
    private val powerManagementService: PowerManagementService,
    private val connectivityService: ConnectivityService,
    private val appPreferences: AppPreferences
) : CoroutineWorker(context, params) {
    private var shouldRun = true

    override suspend fun doWork(): Result {
        return try {
            Log_OC.d(TAG, "InternalTwoWaySyncWork started!")

            @Suppress("ComplexCondition")
            if (!appPreferences.isTwoWaySyncEnabled ||
                powerManagementService.isPowerSavingEnabled ||
                !connectivityService.isConnected ||
                connectivityService.isInternetWalled ||
                !connectivityService.connectivity.isWifi
            ) {
                Log_OC.d(TAG, "Not starting due to constraints!")
                return Result.success()
            }

            userAccountManager.allUsers.forEach { user ->
                val fileDataStorageManager = FileDataStorageManager(user, context.contentResolver)
                val folders = fileDataStorageManager.getInternalTwoWaySyncFolders(user)

                folders.forEach { folder ->
                    val files = fileDataStorageManager.getAllFilesRecursivelyInsideFolder(folder)
                    var lastOperationCode = ""

                    checkFreeSpace(folder)?.let { checkFreeSpaceResult ->
                        return checkFreeSpaceResult
                    }

                    Log_OC.d(TAG, "Folder ${folder.remotePath}: started!")

                    files.forEach { file ->
                        if (!shouldRun) {
                            Log_OC.d(TAG, "InternalTwoWaySyncWork was stopped!")
                            return Result.failure()
                        }

                        if (!file.isDown) {
                            val downloadFileOperation = DownloadFileOperation(user, file, context)
                            val client = OwnCloudClientManagerFactory.getDefaultSingleton()
                                .getClientFor(user.toOwnCloudAccount(), context)
                            val downloadFileOperationResult = downloadFileOperation.execute(client)
                            lastOperationCode = downloadFileOperationResult.code.toString()
                        }
                    }

                    folder.apply {
                        internalFolderSyncResult = lastOperationCode
                        internalFolderSyncTimestamp = System.currentTimeMillis()
                    }

                    fileDataStorageManager.saveFile(folder)
                }
            }

            Log_OC.d(TAG, "InternalTwoWaySyncWork finished with success!")
            Result.success()
        } catch (t: Throwable) {
            Log_OC.d(TAG, "InternalTwoWaySyncWork finished with failure!")
            Result.failure()
        } finally {
            Log_OC.d(TAG, "InternalTwoWaySyncWork cleanup")
            shouldRun = false
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
