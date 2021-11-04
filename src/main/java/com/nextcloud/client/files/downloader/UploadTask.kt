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

import android.content.Context
import com.nextcloud.client.account.User
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.network.ConnectivityService
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.operations.UploadFileOperation

@Suppress("LongParameterList")
class UploadTask(
    private val applicationContext: Context,
    private val uploadsStorageManager: UploadsStorageManager,
    private val connectivityService: ConnectivityService,
    private val powerManagementService: PowerManagementService,
    private val clientProvider: () -> OwnCloudClient,
    private val fileDataStorageManager: FileDataStorageManager,
) {

    data class Result(val file: OCFile, val success: Boolean)

    /**
     * This class is a helper factory to to keep static dependencies
     * injection out of the upload task instance.
     */
    @Suppress("LongParameterList")
    class Factory(
        private val applicationContext: Context,
        private val uploadsStorageManager: UploadsStorageManager,
        private val connectivityService: ConnectivityService,
        private val powerManagementService: PowerManagementService,
        private val clientProvider: () -> OwnCloudClient,
        private val fileDataStorageManager: FileDataStorageManager
    ) {
        fun create(): UploadTask {
            return UploadTask(
                applicationContext,
                uploadsStorageManager,
                connectivityService,
                powerManagementService,
                clientProvider,
                fileDataStorageManager
            )
        }
    }

    fun upload(user: User, upload: OCUpload): Result {
        val file = UploadFileOperation.obtainNewOCFileToUpload(
            upload.remotePath,
            upload.localPath,
            upload.mimeType
        )
        val op = UploadFileOperation(
            uploadsStorageManager,
            connectivityService,
            powerManagementService,
            user,
            file,
            upload,
            NameCollisionPolicy.ASK_USER,
            upload.localAction,
            applicationContext,
            upload.isUseWifiOnly,
            upload.isWhileChargingOnly,
            false,
            fileDataStorageManager
        )
        val client = clientProvider()
        uploadsStorageManager.updateDatabaseUploadStart(op)
        val result = op.execute(client)
        uploadsStorageManager.updateDatabaseUploadResult(result, op)
        return Result(file, result.isSuccess)
    }
}
