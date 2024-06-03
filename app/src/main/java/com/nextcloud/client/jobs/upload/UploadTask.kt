/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs.upload

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
    private val fileDataStorageManager: FileDataStorageManager
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
