/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.nextcloud.client.database.entity.UploadEntity
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.lib.common.operations.RemoteOperationResult

fun UploadsStorageManager.updateStatus(entity: UploadEntity?, status: UploadsStorageManager.UploadStatus) {
    entity ?: return
    uploadDao.update(entity.withStatus(status))
}

fun UploadsStorageManager.updateStatus(entity: UploadEntity?, result: RemoteOperationResult<*>) {
    entity ?: return
    val newStatus = if (result.isSuccess) {
        UploadsStorageManager.UploadStatus.UPLOAD_SUCCEEDED
    } else {
        UploadsStorageManager.UploadStatus.UPLOAD_FAILED
    }
    uploadDao.update(entity.withStatus(newStatus))
}

private fun UploadEntity.withStatus(newStatus: UploadsStorageManager.UploadStatus) = this.copy(status = newStatus.value)
