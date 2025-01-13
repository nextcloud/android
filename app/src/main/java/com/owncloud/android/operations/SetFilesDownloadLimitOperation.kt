/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 ZetaTom <70907959+zetatom@users.noreply.github.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations

import com.nextcloud.android.lib.resources.files.RemoveFilesDownloadLimitRemoteOperation
import com.nextcloud.android.lib.resources.files.SetFilesDownloadLimitRemoteOperation
import com.nextcloud.common.NextcloudClient
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult

class SetFilesDownloadLimitOperation(
    private val shareId: Long,
    private val newLimit: Int,
    private val fileDataStorageManager: FileDataStorageManager
) : RemoteOperation<Void>() {
    override fun run(client: NextcloudClient): RemoteOperationResult<Void> {
        val share = fileDataStorageManager.getShareById(shareId)
        val token = share?.token ?: return RemoteOperationResult(RemoteOperationResult.ResultCode.SHARE_NOT_FOUND)

        return if (newLimit > 0) {
            val operation = SetFilesDownloadLimitRemoteOperation(token, newLimit)
            client.execute(operation)
        } else {
            val operation = RemoveFilesDownloadLimitRemoteOperation(token)
            client.execute(operation)
        }
    }
}