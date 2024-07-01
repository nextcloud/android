/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.asynctasks

import android.content.Context
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.utils.FileStorageUtils

class GetRemoteFileTask(
    private val context: Context,
    private val fileUrl: String,
    private val client: OwnCloudClient,
    private val storageManager: FileDataStorageManager,
    private val user: User
) : () -> GetRemoteFileTask.Result {

    data class Result(val success: Boolean = false, val file: OCFile = OCFile("/"))

    override fun invoke(): Result {
        val result = ReadFileRemoteOperation(fileUrl).execute(client)
        if (result.isSuccess) {
            val remoteFile = result.resultData
            val temp = FileStorageUtils.fillOCFile(remoteFile)
            val remoteOcFile = storageManager.saveFileWithParent(temp, context)
            if (remoteOcFile.isFolder) {
                // perform folder synchronization
                val synchFolderOp: RemoteOperation<Any> = RefreshFolderOperation(
                    remoteOcFile,
                    System.currentTimeMillis(),
                    false,
                    true,
                    storageManager,
                    user,
                    context
                )
                synchFolderOp.execute(client)
            }
            return Result(true, remoteOcFile)
        } else {
            return Result(false, OCFile(""))
        }
    }
}
