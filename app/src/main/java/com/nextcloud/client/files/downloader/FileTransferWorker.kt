/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2021 Chris Narkiewicz <hello@ezaquarii.com>
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
package com.nextcloud.client.files.downloader

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.nextcloud.client.account.User
import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.logger.Logger
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.notifications.AppNotificationManager
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.UploadsStorageManager

@Suppress("LongParameterList")
class FileTransferWorker(
    private val notificationsManager: AppNotificationManager,
    private val clientFactory: ClientFactory,
    private val runner: AsyncRunner,
    private val logger: Logger,
    private val uploadsStorageManager: UploadsStorageManager,
    private val connectivityService: ConnectivityService,
    private val powerManagementService: PowerManagementService,
    private val fileDataStorageManager: FileDataStorageManager,
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    private val gson = Gson()

    companion object {
        const val TAG = "DownloaderService"
        const val EXTRA_REQUEST = "request"

        var manager: Manager? = null
    }

    @Suppress("TooGenericExceptionCaught")
    override fun doWork(): Result {
        return try {
            val request = gson.fromJson(inputData.keyValueMap[EXTRA_REQUEST] as String, Request::class.java)

            if (!isRunning) {
                notificationsManager.buildDownloadServiceForegroundNotification()
            }

            val transferManager = getTransferManager(request.user)
            transferManager.enqueue(request)
            manager = Manager(transferManager)

            logger.d(TAG, "Enqueued new transfer: ${request.uuid} ${request.file.remotePath}")

            Result.success()
        } catch (t: Throwable) {
            logger.d(TAG, "Error caught at FileTransferWorker: ${t.localizedMessage}")
            Result.failure()
        }
    }

    class Manager(downloader: TransferManagerImpl) : TransferManager by downloader

    val isRunning: Boolean get() = downloader.any { it.value.isRunning }

    private val downloader: MutableMap<String, TransferManagerImpl> = mutableMapOf()

    @SuppressLint("RestrictedApi")
    private fun onTransferUpdate(transfer: Transfer) {
        if (!isRunning) {
            logger.d(TAG, "All downloads completed")
            notificationsManager.cancelTransferNotification()
            stop()
        } else if (transfer.direction == Direction.DOWNLOAD) {
            notificationsManager.postDownloadTransferProgress(
                fileOwner = transfer.request.user,
                file = transfer.request.file,
                progress = transfer.progress,
                allowPreview = !transfer.request.test
            )
        } else if (transfer.direction == Direction.UPLOAD) {
            notificationsManager.postUploadTransferProgress(
                fileOwner = transfer.request.user,
                file = transfer.request.file,
                progress = transfer.progress
            )
        }
    }

    private fun getTransferManager(user: User): TransferManagerImpl {
        val existingDownloader = downloader[user.accountName]
        return if (existingDownloader != null) {
            existingDownloader
        } else {
            val downloadTaskFactory = DownloadTask.Factory(
                applicationContext,
                { clientFactory.create(user) },
                context.contentResolver
            )
            val uploadTaskFactory = UploadTask.Factory(
                applicationContext,
                uploadsStorageManager,
                connectivityService,
                powerManagementService,
                { clientFactory.create(user) },
                fileDataStorageManager
            )
            val newDownloader = TransferManagerImpl(runner, downloadTaskFactory, uploadTaskFactory)
            newDownloader.registerTransferListener(this::onTransferUpdate)
            downloader[user.accountName] = newDownloader
            newDownloader
        }
    }
}
