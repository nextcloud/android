/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2014-2015 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2015 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.operations

import com.nextcloud.android.lib.resources.files.GetFilesDownloadLimitRemoteOperation
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.GetSharesForFileRemoteOperation
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.operations.common.SyncOperation

/**
 * Provide a list shares for a specific file.
 */
class GetSharesForFileOperation(
    private val path: String,
    private val reshares: Boolean,
    private val subfiles: Boolean,
    storageManager: FileDataStorageManager
) : SyncOperation(storageManager) {

    @Suppress("DEPRECATION", "NestedBlockDepth")
    @Deprecated("Deprecated in Java")
    override fun run(client: OwnCloudClient): RemoteOperationResult<List<OCShare>> {
        val result = GetSharesForFileRemoteOperation(path, reshares, subfiles).execute(client)

        if (result.isSuccess) {
            // Update DB with the response
            val shares = result.resultData
            Log_OC.d(TAG, "File = $path Share list size ${shares.size}")

            val capability = storageManager.getCapability(storageManager.user)
            if (capability.filesDownloadLimit.isTrue && shares.any { it.shareType == ShareType.PUBLIC_LINK }) {
                val downloadLimitResult = GetFilesDownloadLimitRemoteOperation(path, subfiles).execute(client)
                if (downloadLimitResult.isSuccess) {
                    val downloadLimits = downloadLimitResult.resultData
                    downloadLimits.forEach { downloadLimit ->
                        shares.find { share ->
                            share.token == downloadLimit.token
                        }?.fileDownloadLimit = downloadLimit
                    }
                }
            }

            storageManager.saveSharesDB(shares)
        } else if (result.code == RemoteOperationResult.ResultCode.SHARE_NOT_FOUND) {
            // no share on the file - remove local shares
            storageManager.removeSharesForFile(path)
        }

        return result
    }

    companion object {
        private val TAG: String = GetSharesForFileOperation::class.java.simpleName
    }
}
