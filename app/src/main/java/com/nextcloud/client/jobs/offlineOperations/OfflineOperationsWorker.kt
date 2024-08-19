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
import com.owncloud.android.lib.common.operations.RemoteOperation
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

    @Suppress("Deprecation")
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

        notificationManager.start()

        offlineOperations.forEachIndexed { index, operation ->
            when (operation.type) {
                OfflineOperationType.CreateFolder -> {
                    val createFolderOperation = async(Dispatchers.IO) { createFolder(operation) }.await()
                    val result = createFolderOperation?.execute(client)
                    handleResult(operation, offlineOperations.size, index, result, createFolderOperation)
                }

                else -> {
                    Log_OC.d(TAG, "Operation terminated, not supported operation type")
                }
            }
        }

        Log_OC.d(TAG, "OfflineOperationsWorker successfully completed")
        notificationManager.dismissNotification()
        WorkerStateLiveData.instance().setWorkState(WorkerState.OfflineOperationsCompleted)
        return@coroutineScope Result.success()
    }

    private fun handleResult(
        operation: OfflineOperationEntity,
        operationSize: Int,
        currentOperationIndex: Int,
        result: RemoteOperationResult<*>?,
        remoteOperation: RemoteOperation<*>?
    ) {
        if (result == null) {
            Log_OC.d(TAG, "Operation not completed, result is null")
            return
        }

        if (result.isSuccess) {
            fileDataStorageManager.offlineOperationDao.delete(operation)
            notificationManager.update(operationSize, currentOperationIndex, operation.filename ?: "")
        } else if (result.code == RemoteOperationResult.ResultCode.FOLDER_ALREADY_EXISTS) {
            context.showToast(context.getString(R.string.folder_already_exists_server, operation.filename))
            fileDataStorageManager.offlineOperationDao.delete(operation)
        }

        val logMessage = if (result.isSuccess) "Operation completed" else "Operation terminated"
        val operationLog = "$logMessage path: ${operation.path}, type: ${operation.type}"
        Log_OC.d(TAG, operationLog)

        if (!result.isSuccess && remoteOperation != null) {
            notificationManager.showNewNotification(result, remoteOperation)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun createFolder(
        operation: OfflineOperationEntity,
    ): RemoteOperation<*>? {
        return withContext(Dispatchers.IO) {
            val createFolderOperation = CreateFolderOperation(operation.path, user, context, fileDataStorageManager)

            try {
                createFolderOperation
            } catch (e: Exception) {
                Log_OC.d(TAG, "Create folder operation terminated, $e")
                null
            }
        }
    }
}
