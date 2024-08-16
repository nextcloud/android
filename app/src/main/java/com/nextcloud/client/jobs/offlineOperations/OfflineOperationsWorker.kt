/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.offlineOperations

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.User
import com.nextcloud.client.database.entity.OfflineOperationEntity
import com.nextcloud.client.network.ClientFactoryImpl
import com.nextcloud.model.OfflineOperationType
import com.nextcloud.model.WorkerState
import com.nextcloud.model.WorkerStateLiveData
import com.nextcloud.receiver.NetworkChangeReceiver
import com.nextcloud.utils.extensions.showToast
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.CreateFolderOperation
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class OfflineOperationsWorker(
    private val user: User,
    private val context: Context,
    viewThemeUtils: ViewThemeUtils,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private val TAG = OfflineOperationsWorker::class.java.simpleName
        const val JOB_NAME = "job_name"
    }

    private val fileDataStorageManager = FileDataStorageManager(user, context.contentResolver)
    private val clientFactory = ClientFactoryImpl(context)
    private val notificationManager = OfflineOperationsNotificationManager(context, viewThemeUtils)

    override suspend fun doWork(): Result = coroutineScope {
        val jobName = inputData.getString(JOB_NAME)
        Log_OC.d(
            TAG,
            "$jobName -----------------------------------\n" +
                "OfflineOperationsWorker started" +
                "\n-----------------------------------"
        )

        if (!NetworkChangeReceiver.isNetworkAvailable(context)) {
            Log_OC.d(TAG, "OfflineOperationsWorker cancelled, no internet connection")
            return@coroutineScope Result.success()
        }

        val isEmpty = fileDataStorageManager.offlineOperationDao.isEmpty()
        if (isEmpty) {
            Log_OC.d(TAG, "OfflineOperationsWorker cancelled, no offline operations were found")
            return@coroutineScope Result.success()
        }

        val client = clientFactory.create(user)
        val offlineOperations = fileDataStorageManager.offlineOperationDao.getAll()
        offlineOperations.forEachIndexed { index, operation ->
            notificationManager.start(offlineOperations.size, index, operation.filename ?: "")

            when (operation.type) {
                OfflineOperationType.CreateFolder -> {
                    val createFolderOperation = async(Dispatchers.IO) { createFolder(operation, client) }
                    val result = createFolderOperation.await()

                    val operationLog = "path: ${operation.path}, type: ${operation.type}"
                    if (result?.isSuccess == true) {
                        Log_OC.d(TAG, "Operation completed, $operationLog")
                        fileDataStorageManager.offlineOperationDao.delete(operation)
                    } else if (result?.code == RemoteOperationResult.ResultCode.FOLDER_ALREADY_EXISTS) {
                        context.showToast(context.getString(R.string.folder_already_exists_server, operation.filename))
                        Log_OC.d(TAG, "Operation terminated, $operationLog")
                        fileDataStorageManager.offlineOperationDao.delete(operation)
                    }
                }

                else -> {
                    Log_OC.d(TAG, "Operation terminated, not supported operation type")
                }
            }

            notificationManager.update(offlineOperations.size, index)
        }

        Log_OC.d(TAG, "OfflineOperationsWorker successfully completed")
        notificationManager.dismissNotification()
        WorkerStateLiveData.instance().setWorkState(WorkerState.OfflineOperationsCompleted)
        return@coroutineScope Result.success()
    }

    @Suppress("TooGenericExceptionCaught", "Deprecation")
    private suspend fun createFolder(
        operation: OfflineOperationEntity,
        client: OwnCloudClient
    ): RemoteOperationResult<*>? {
        return withContext(Dispatchers.IO) {
            val createFolderOperation = CreateFolderOperation(operation.path, user, context, fileDataStorageManager)

            try {
                createFolderOperation.execute(client)
            } catch (e: Exception) {
                Log_OC.d(TAG, "Create folder operation terminated, $e")
                null
            }
        }
    }


}
