/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Raphael Vieira raphaelecv.projects@gmail.com
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.nextcloud.client.jobs.upload

import android.content.Context
import com.nextcloud.client.account.User
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.network.ConnectivityService
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.operations.UploadFileOperation
import javax.inject.Inject

interface FileUploadOperationFactory {
    fun create(upload: OCUpload, user: User, storageManager: FileDataStorageManager): UploadFileOperation
}

class FileUploadOperationFactoryImpl @Inject constructor(
    private val uploadsStorageManager: UploadsStorageManager,
    private val connectivityService: ConnectivityService,
    private val powerManagementService: PowerManagementService,
    private val context: Context
) : FileUploadOperationFactory {
    override fun create(upload: OCUpload, user: User, storageManager: FileDataStorageManager): UploadFileOperation =
        UploadFileOperation(
            uploadsStorageManager,
            connectivityService,
            powerManagementService,
            user,
            null,
            upload,
            upload.nameCollisionPolicy,
            upload.localAction,
            context,
            upload.isUseWifiOnly,
            upload.isWhileChargingOnly,
            storageManager
        )
}
