/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.share

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.nextcloud.repository.ClientRepository
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.GetSharesForFileOperation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RemoteShareRepository(
    private val clientRepository: ClientRepository,
    lifecycleOwner: LifecycleOwner,
    private val fileDataStorageManager: FileDataStorageManager
) : ShareRepository {
    private val tag = "RemoteShareRepository"
    private val scope = lifecycleOwner.lifecycleScope

    override fun fetchSharees(remotePath: String, onCompleted: () -> Unit, onError: () -> Unit) {
        scope.launch(Dispatchers.IO) {
            val client = clientRepository.getOwncloudClient() ?: return@launch
            val operation =
                GetSharesForFileOperation(
                    path = remotePath,
                    reshares = true,
                    subfiles = false,
                    storageManager = fileDataStorageManager
                )

            @Suppress("DEPRECATION")
            val result = operation.execute(client)

            Log_OC.i(tag, "Remote path for the refresh shares: $remotePath")

            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    Log_OC.d(tag, "Successfully refreshed shares for the specified remote path.")
                    onCompleted()
                } else {
                    Log_OC.w(
                        tag,
                        "Failed to refresh shares for the specified remote path. " +
                            "An error occurred during the operation."
                    )
                    onError()
                }
            }
        }
    }
}
