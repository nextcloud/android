/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.nextcloud.client.database.entity.OfflineOperationEntity
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.utils.FileStorageUtils

fun RemoteOperationResult<*>?.getConflictedRemoteIdsWithOfflineOperations(
    offlineOperations: List<OfflineOperationEntity>
): Pair<ArrayList<String>, ArrayList<String?>>? {
    val newFiles = toOCFile() ?: return null

    val (remoteIds, offlineOperationsPaths) = newFiles
        .flatMap { file ->
            offlineOperations
                .filter { it.filename == file.fileName }
                .map { file.remoteId to it.path }
        }
        .unzip()

    return ArrayList(remoteIds) to ArrayList(offlineOperationsPaths)
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
