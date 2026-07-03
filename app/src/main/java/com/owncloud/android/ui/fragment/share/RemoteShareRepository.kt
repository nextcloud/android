/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.share

import com.nextcloud.repository.ClientRepository
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.GetSharesForFileOperation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RemoteShareRepository(
    private val clientRepository: ClientRepository,
    private val fileDataStorageManager: FileDataStorageManager
) : ShareRepository {
    private val tag = "RemoteShareRepository"

    @Suppress("DEPRECATION")
    override suspend fun fetchSharees(remotePath: String): Boolean = withContext(Dispatchers.IO) {
        val client = clientRepository.getOwncloudClient() ?: return@withContext false
        val operation =
            GetSharesForFileOperation(
                path = remotePath,
                reshares = true,
                subfiles = false,
                storageManager = fileDataStorageManager
            )

        val result = operation.execute(client)

        Log_OC.i(tag, "Remote path for the refresh shares: $remotePath")

        withContext(Dispatchers.Main) {
            val isSuccess = result.isSuccess
            if (isSuccess) {
                Log_OC.d(tag, "Successfully refreshed shares for the specified remote path.")
            } else {
                Log_OC.w(
                    tag,
                    "Failed to refresh shares for the specified remote path. " +
                        "An error occurred during the operation."
                )
            }
            return@withContext isSuccess
        }
    }
}
