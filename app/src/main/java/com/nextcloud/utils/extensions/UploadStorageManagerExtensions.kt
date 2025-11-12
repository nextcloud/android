/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.nextcloud.client.database.entity.UploadEntity
import com.owncloud.android.datamodel.UploadsStorageManager

fun UploadsStorageManager.updateStatus(entity: UploadEntity?, status: UploadsStorageManager.UploadStatus) {
    entity ?: return
    uploadDao.insertOrReplace(entity.withStatus(status))
}

fun UploadsStorageManager.updateStatus(entity: UploadEntity?, success: Boolean) {
    entity ?: return
    val newStatus = if (success) {
        UploadsStorageManager.UploadStatus.UPLOAD_SUCCEEDED
    } else {
        UploadsStorageManager.UploadStatus.UPLOAD_FAILED
    }
    uploadDao.insertOrReplace(entity.withStatus(newStatus))
}

private fun UploadEntity.withStatus(newStatus: UploadsStorageManager.UploadStatus) = this.copy(status = newStatus.value)
