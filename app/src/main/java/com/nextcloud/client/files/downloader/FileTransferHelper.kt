/*
 * Nextcloud Android client application
 *
 * @author Alper Ozturk
 * Copyright (C) 2023 Alper Ozturk
 * Copyright (C) 2023 Nextcloud GmbH
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.nextcloud.client.files.downloader

import android.content.Context
import com.nextcloud.client.account.User
import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ConnectivityService
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.UploadsStorageManager
import javax.inject.Inject

@Suppress("LongParameterList")
class FileTransferHelper @Inject constructor(
    private val clientFactory: ClientFactory,
    private val fileDataStorageManager: FileDataStorageManager,
    private val runner: AsyncRunner,
    private val powerManagementService: PowerManagementService,
    private val connectivityService: ConnectivityService,
    private val uploadsStorageManager: UploadsStorageManager
) {
    private val downloader: MutableMap<String, TransferManagerImpl> = mutableMapOf()

    fun getTransferManager(context: Context, user: User): FileTransferWorker.Manager {
        val transferManager = getTransferManager(downloader, context, user, null)
        return FileTransferWorker.Manager(transferManager)
    }

    fun getTransferManager(
        downloader: MutableMap<String, TransferManagerImpl>,
        context: Context,
        user: User,
        listener: ((Transfer) -> Unit)?
    ): TransferManagerImpl {
        val existingDownloader = downloader[user.accountName]

        return if (existingDownloader != null) {
            existingDownloader
        } else {
            val downloadTaskFactory = DownloadTask.Factory(
                context,
                { clientFactory.create(user) },
                context.contentResolver
            )
            val uploadTaskFactory = UploadTask.Factory(
                context,
                uploadsStorageManager,
                connectivityService,
                powerManagementService,
                { clientFactory.create(user) },
                fileDataStorageManager
            )
            val newDownloader = TransferManagerImpl(runner, downloadTaskFactory, uploadTaskFactory)
            listener?.let {
                newDownloader.registerTransferListener(listener)
            }
            downloader[user.accountName] = newDownloader
            return newDownloader
        }
    }
}
