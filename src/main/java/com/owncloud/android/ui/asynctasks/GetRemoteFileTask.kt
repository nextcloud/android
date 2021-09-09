/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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
package com.owncloud.android.ui.asynctasks

import android.content.Context
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
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
            val remoteFile = result.getData().get(0) as RemoteFile
            val temp = FileStorageUtils.fillOCFile(remoteFile)
            val remoteOcFile = storageManager.saveFileWithParent(temp, context)
            if (remoteOcFile.isFolder()) {
                // perform folder synchronization
                val synchFolderOp: RemoteOperation<Any> = RefreshFolderOperation(
                    remoteOcFile,
                    System.currentTimeMillis(),
                    false,
                    true,
                    storageManager,
                    user.toPlatformAccount(),
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
