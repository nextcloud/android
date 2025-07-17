/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 ZetaTom <70907959+zetatom@users.noreply.github.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations

import com.nextcloud.android.lib.resources.files.FileDownloadLimit
import com.nextcloud.android.lib.resources.files.GetFilesDownloadLimitRemoteOperation
import com.nextcloud.common.NextcloudClient
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.operations.common.SyncOperation

class GetFilesDownloadLimitOperation(val share: OCShare, storageManager: FileDataStorageManager) :
    SyncOperation(
        storageManager
    ) {
    override fun run(client: NextcloudClient): RemoteOperationResult<List<FileDownloadLimit>> {
        val token = share.token ?: return RemoteOperationResult(RemoteOperationResult.ResultCode.SHARE_NOT_FOUND)
        val operation = GetFilesDownloadLimitRemoteOperation(token)

        val result = operation.execute(client)

        return result
    }
}
