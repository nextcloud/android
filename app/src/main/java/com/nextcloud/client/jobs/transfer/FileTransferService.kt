/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs.transfer

import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleService
import com.nextcloud.client.account.User
import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.core.LocalBinder
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.files.Direction
import com.nextcloud.client.files.Request
import com.nextcloud.client.jobs.download.DownloadTask
import com.nextcloud.client.jobs.upload.UploadTask
import com.nextcloud.client.logger.Logger
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.notifications.AppNotificationManager
import com.nextcloud.utils.ForegroundServiceHelper
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.ForegroundServiceType
import com.owncloud.android.datamodel.UploadsStorageManager
import dagger.android.AndroidInjection
import javax.inject.Inject
import javax.inject.Named

class FileTransferService : LifecycleService() {

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
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent == null || intent.action != ACTION_TRANSFER) {
            return START_NOT_STICKY
        }

        if (!isRunning && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            ForegroundServiceHelper.startService(
                this,
                AppNotificationManager.TRANSFER_NOTIFICATION_ID,
                notificationsManager.buildDownloadServiceForegroundNotification(),
                ForegroundServiceType.DataSync
            )
        }

        val request: Request = intent.getParcelableArgument(EXTRA_REQUEST, Request::class.java)!!

        getTransferManager(request.user).run {
            enqueue(request)
        }

        logger.d(TAG, "Enqueued new transfer: ${request.uuid} ${request.file.remotePath}")

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        val user = intent.getParcelableArgument(EXTRA_USER, User::class.java) ?: return null
        return Binder(getTransferManager(user), this)
    }

    private fun onTransferUpdate(transfer: Transfer) {
        if (!isRunning) {
            logger.d(TAG, "All downloads completed")
            notificationsManager.cancelTransferNotification()
            stopForeground(STOP_FOREGROUND_DETACH)
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
