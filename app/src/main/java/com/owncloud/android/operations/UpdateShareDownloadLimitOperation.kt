/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations

import com.nextcloud.android.lib.resources.files.FileDownloadLimit
import com.nextcloud.android.lib.resources.files.GetFilesDownloadLimitRemoteOperation
import com.nextcloud.utils.extensions.toEntity
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.resources.shares.OCShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UpdateShareDownloadLimitOperation(
    private val share: OCShare,
    private val client: OwnCloudClient,
    private val storageManager: FileDataStorageManager,
    private val accountName: String
) {
    @Suppress("DEPRECATION")
    suspend fun run(): OCShare = withContext(Dispatchers.IO) {
        val remotePath = share.path ?: return@withContext share

        val op = GetFilesDownloadLimitRemoteOperation(remotePath)
        val result = op.execute(client)

        if (!result.isSuccess) {
            return@withContext share
        }

        val limits = result.data
            ?.filterIsInstance<FileDownloadLimit>()
            ?: return@withContext share

        val newLimit = limits.firstOrNull() ?: return@withContext share

        share.fileDownloadLimit = newLimit
        storageManager.shareDao.update(share.toEntity(accountName))

        return@withContext share
    }
}
