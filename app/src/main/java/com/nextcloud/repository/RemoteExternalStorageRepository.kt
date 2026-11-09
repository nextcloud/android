/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.repository

import com.nextcloud.android.lib.resources.externalstorage.ExternalStorageListMountsRemoteOperation
import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class RemoteExternalStorageRepository @Inject constructor(private val clientRepository: ClientRepository) :
    ExternalStorageRepository {

    override suspend fun fetchMounts(): ExternalStorageMountsResult {
        val client = clientRepository.getNextcloudClient() ?: return ExternalStorageMountsResult.Failure
        val result = withContext(Dispatchers.IO) { ExternalStorageListMountsRemoteOperation().execute(client) }
        if (!result.isSuccess) {
            Log_OC.e(TAG, "Could not fetch external storage mounts")
            return ExternalStorageMountsResult.Failure
        }
        return ExternalStorageMountsResult.Success(result.resultData.orEmpty())
    }

    companion object {
        private const val TAG = "ExternalStorageRepository"
    }
}
