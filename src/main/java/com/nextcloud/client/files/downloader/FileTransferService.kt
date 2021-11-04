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

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.nextcloud.client.account.User
import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.core.LocalBinder
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.logger.Logger
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.notifications.AppNotificationManager
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.UploadsStorageManager
import dagger.android.AndroidInjection
import javax.inject.Inject
import javax.inject.Named

class FileTransferService : Service() {

    companion object {
        const val TAG = "DownloaderService"
        const val ACTION_TRANSFER = "transfer"
        const val EXTRA_REQUEST = "request"
        const val EXTRA_USER = "user"

        fun createBindIntent(context: Context, user: User): Intent {
            return Intent(context, FileTransferService::class.java).apply {
                putExtra(EXTRA_USER, user)
            }
        }

        fun createTransferRequestIntent(context: Context, request: Request): Intent {
            return Intent(context, FileTransferService::class.java).apply {
                action = ACTION_TRANSFER
                putExtra(EXTRA_REQUEST, request)
            }
        }
    }

    /**
     * Binder forwards [TransferManager] API calls to selected instance of downloader.
     */
    class Binder(
        downloader: TransferManagerImpl,
        service: FileTransferService
    ) : LocalBinder<FileTransferService>(service),
        TransferManager by downloader

    @Inject
    lateinit var notificationsManager: AppNotificationManager

    @Inject
    lateinit var clientFactory: ClientFactory

    @Inject
    @Named("io")
    lateinit var runner: AsyncRunner

    @Inject
    lateinit var logger: Logger

    @Inject
    lateinit var uploadsStorageManager: UploadsStorageManager

    @Inject
    lateinit var connectivityService: ConnectivityService

    @Inject
    lateinit var powerManagementService: PowerManagementService

    @Inject
    lateinit var fileDataStorageManager: FileDataStorageManager

    val isRunning: Boolean get() = downloaders.any { it.value.isRunning }

    private val downloaders: MutableMap<String, TransferManagerImpl> = mutableMapOf()

    override fun onCreate() {
        AndroidInjection.inject(this)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.action != ACTION_TRANSFER) {
            return START_NOT_STICKY
        }

        if (!isRunning) {
            startForeground(
                AppNotificationManager.TRANSFER_NOTIFICATION_ID,
                notificationsManager.buildDownloadServiceForegroundNotification()
            )
        }

        val request = intent.getParcelableExtra(EXTRA_REQUEST) as Request
        val transferManager = getTransferManager(request.user)
        transferManager.enqueue(request)

        logger.d(TAG, "Enqueued new transfer: ${request.uuid} ${request.file.remotePath}")

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        val user = intent?.getParcelableExtra<User>(EXTRA_USER)
        if (user != null) {
            return Binder(getTransferManager(user), this)
        } else {
            return null
        }
    }

    private fun onTransferUpdate(transfer: Transfer) {
        if (!isRunning) {
            logger.d(TAG, "All downloads completed")
            notificationsManager.cancelTransferNotification()
            stopForeground(true)
            stopSelf()
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

    override fun onDestroy() {
        super.onDestroy()
        logger.d(TAG, "Stopping downloader service")
    }

    private fun getTransferManager(user: User): TransferManagerImpl {
        val existingDownloader = downloaders[user.accountName]
        return if (existingDownloader != null) {
            existingDownloader
        } else {
            val downloadTaskFactory = DownloadTask.Factory(
                applicationContext,
                { clientFactory.create(user) },
                contentResolver
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
            downloaders[user.accountName] = newDownloader
            newDownloader
        }
    }
}
