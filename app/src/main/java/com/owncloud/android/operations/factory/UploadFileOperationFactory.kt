/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.factory

import android.content.Context
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.network.ConnectivityService
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener
import com.owncloud.android.operations.UploadFileOperation
import javax.inject.Inject

@Suppress("LongParameterList")
class UploadFileOperationFactory @Inject constructor(
    private val uploadsStorageManager: UploadsStorageManager,
    private val connectivityService: ConnectivityService,
    private val powerManagementService: PowerManagementService,
    private val context: Context,
    private val accountManager: UserAccountManager,
    private val fileDataStorageManager: FileDataStorageManager
) {

    fun create(
        upload: OCUpload,
        progressListener: OnDatatransferProgressListener? = null,
        disableRetries: Boolean = true
    ): UploadFileOperation = UploadFileOperation(
        uploadsStorageManager,
        connectivityService,
        powerManagementService,
        accountManager.user,
        null,
        upload,
        upload.nameCollisionPolicy ?: NameCollisionPolicy.ASK_USER,
        upload.localAction,
        context,
        upload.isUseWifiOnly,
        upload.isWhileChargingOnly,
        disableRetries,
        fileDataStorageManager
    ).apply {
        progressListener?.let { addDataTransferProgressListener(it) }
    }
}
