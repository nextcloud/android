/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 ZetaTom <70907959+zetatom@users.noreply.github.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations

import android.content.Context
import com.nextcloud.android.lib.resources.files.GetFilesDownloadLimitRemoteOperation
import com.nextcloud.android.lib.resources.files.RemoveFilesDownloadLimitRemoteOperation
import com.nextcloud.android.lib.resources.files.SetFilesDownloadLimitRemoteOperation
import com.nextcloud.utils.extensions.toNextcloudClient
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult

class SetFilesDownloadLimitOperation(
    private val shareId: Long,
    private val newLimit: Int,
    private val fileDataStorageManager: FileDataStorageManager,
    private val context: Context
) : RemoteOperation<Void>() {
    @Deprecated("Deprecated in Java")
    override fun run(client: OwnCloudClient): RemoteOperationResult<Void> {
        val nextcloudClient = client.toNextcloudClient(context)
        val share = fileDataStorageManager.getShareById(shareId)
        val token = share?.token ?: return RemoteOperationResult(RemoteOperationResult.ResultCode.SHARE_NOT_FOUND)

        val result = if (newLimit > 0) {
            val operation = SetFilesDownloadLimitRemoteOperation(token, newLimit)
            nextcloudClient.execute(operation)
        } else {
            val operation = RemoveFilesDownloadLimitRemoteOperation(token)
            nextcloudClient.execute(operation)
        }

        val path = share.path
        if (result.isSuccess && path != null) {
            val getFilesDownloadLimitRemoteOperation = GetFilesDownloadLimitRemoteOperation(path, false)
            val remoteOperationResult = getFilesDownloadLimitRemoteOperation.execute(client)

            if (remoteOperationResult.isSuccess) {
                share.fileDownloadLimit = remoteOperationResult.resultData.firstOrNull { updatedDownloadLimit ->
                    updatedDownloadLimit.token == share.token
                }
                fileDataStorageManager.saveShare(share)
            }
        }

        return result
    }
}
