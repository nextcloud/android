/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.nextcloud.client.database.entity.OfflineOperationEntity
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.utils.ErrorMessageAdapter
import com.owncloud.android.utils.FileStorageUtils

fun Pair<RemoteOperationResult<*>?, RemoteOperation<*>?>?.getErrorMessage(): String {
    val result = this?.first ?: return MainApp.string(R.string.unexpected_error_occurred)
    val operation = this.second ?: return MainApp.string(R.string.unexpected_error_occurred)
    return ErrorMessageAdapter.getErrorCauseMessage(result, operation, MainApp.getAppContext().resources)
}

fun RemoteOperationResult<*>?.getConflictedRemoteIdsWithOfflineOperations(
    offlineOperations: List<OfflineOperationEntity>
): HashMap<String, String?>? {
    val newFiles = toOCFile() ?: return null

    val conflictedMap = newFiles
        .flatMap { file ->
            offlineOperations
                .filter { it.filename == file.fileName }
                .map { file.remoteId to it.path }
        }
        .toMap(HashMap())

    return conflictedMap.ifEmpty { null }
}

@Suppress("Deprecation")
fun RemoteOperationResult<*>?.toOCFile(): List<OCFile>? {
    return if (this?.isSuccess == true) {
        data?.toOCFileList()
    } else {
        null
    }
}

private fun ArrayList<Any>.toOCFileList(): List<OCFile> {
    return this.mapNotNull {
        val remoteFile = (it as? RemoteFile)

        remoteFile?.let {
            remoteFile.toOCFile()
        }
    }
}

private fun RemoteFile?.toOCFile(): OCFile = FileStorageUtils.fillOCFile(this)
