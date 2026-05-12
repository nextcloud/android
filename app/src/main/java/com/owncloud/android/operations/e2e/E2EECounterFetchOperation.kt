/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.e2e

import android.content.Context
import com.nextcloud.client.account.User
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.utils.e2ee.E2EVersionHelper.isV2Plus
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.utils.theme.CapabilityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class E2EECounterFetchOperation {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "E2EECounterFetchOperation"
    }

    fun fetchAsync(
        context: Context,
        file: OCFile,
        storageManager: FileDataStorageManager,
        user: User,
        clientFactory: ClientFactory,
        onComplete: (Long) -> Unit
    ) {
        scope.launch {
            val client = clientFactory.create(user)
            val counter = fetch(context, file, storageManager, user, client)
            withContext(Dispatchers.Main) {
                onComplete(counter)
            }
        }
    }

    fun fetch(
        context: Context,
        file: OCFile,
        storageManager: FileDataStorageManager,
        user: User,
        client: OwnCloudClient
    ): Long = try {
        val metadata = RefreshFolderOperation
            .getDecryptedFolderMetadata(true, file, client, user, context)
        if (metadata is DecryptedFolderMetadataFile) {
            val result = metadata.metadata.counter
            file.setE2eCounter(result)
            storageManager.saveFile(file)
            Log_OC.i(TAG, "latest counter fetched, counter is: $result")
            result
        } else {
            val result = getFallbackCounter(context, file)
            Log_OC.w(TAG, "local counter is used, counter is: $result")
            result
        }
    } catch (e: Exception) {
        Log_OC.e(TAG, "error refreshing E2E counter: ${e.message}")
        getFallbackCounter(context, file)
    }

    fun stop() {
        scope.cancel()
    }

    // region private methods
    private fun supportsE2EEv2(context: Context): Boolean {
        val capability = CapabilityUtils.getCapability(context)
        return isV2Plus(capability)
    }

    private fun getFallbackCounter(context: Context, file: OCFile): Long {
        if (!supportsE2EEv2(context)) {
            return -1
        }
        return file.e2eCounter + 1
    }
    // endregion
}
