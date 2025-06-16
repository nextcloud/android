/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.nextcloud.client.database.entity.OfflineOperationEntity
import com.nextcloud.model.OfflineOperationType
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.utils.ErrorMessageAdapter
import com.owncloud.android.utils.FileStorageUtils

@Suppress("ReturnCount")
fun Pair<RemoteOperationResult<*>?, RemoteOperation<*>?>?.getErrorMessage(): String {
    val result = this?.first ?: return MainApp.string(R.string.unexpected_error_occurred)
    val operation = this.second ?: return MainApp.string(R.string.unexpected_error_occurred)
    return ErrorMessageAdapter.getErrorCauseMessage(result, operation, MainApp.getAppContext().resources)
}

fun RemoteOperationResult<*>?.getConflictedRemoteIdsWithOfflineOperations(
    offlineOperations: List<OfflineOperationEntity>,
    fileDataStorageManager: FileDataStorageManager
): HashMap<String, String>? {
    val relevantOperations = offlineOperations
        .filter {
            it.type is OfflineOperationType.CreateFile || it.type is OfflineOperationType.CreateFolder
        }
        .filter { fileDataStorageManager.fileExists(it.path) }
        .groupBy { it.filename }

    val newFiles = toOCFile() ?: return null

    val result = HashMap<String, String>()
    for (file in newFiles) {
        relevantOperations[file.fileName]?.forEach { op ->
            op.path?.let { result[file.remoteId] = it }
        }
    }

    return if (result.isEmpty()) null else result
}

fun ResultCode.isFileSpecificError(): Boolean {
    val errorCodes = listOf(
        ResultCode.INSTANCE_NOT_CONFIGURED,
        ResultCode.QUOTA_EXCEEDED,
        ResultCode.LOCAL_STORAGE_FULL,
        ResultCode.WRONG_CONNECTION,
        ResultCode.UNAUTHORIZED,
        ResultCode.OK_NO_SSL,
        ResultCode.MAINTENANCE_MODE,
        ResultCode.UNTRUSTED_DOMAIN,
        ResultCode.ACCOUNT_NOT_THE_SAME,
        ResultCode.ACCOUNT_EXCEPTION,
        ResultCode.ACCOUNT_NOT_NEW,
        ResultCode.ACCOUNT_NOT_FOUND,
        ResultCode.ACCOUNT_USES_STANDARD_PASSWORD,
        ResultCode.INCORRECT_ADDRESS,
        ResultCode.BAD_OC_VERSION
    )

    return !errorCodes.contains(this)
}

@Suppress("Deprecation")
fun RemoteOperationResult<*>?.toOCFile(): List<OCFile>? = if (this?.isSuccess == true) {
    data?.toOCFileList()
} else {
    null
}

private fun ArrayList<Any>.toOCFileList(): List<OCFile> = this.mapNotNull {
    val remoteFile = (it as? RemoteFile)

    remoteFile?.let {
        remoteFile.toOCFile()
    }
}

private fun RemoteFile?.toOCFile(): OCFile = FileStorageUtils.fillOCFile(this)
