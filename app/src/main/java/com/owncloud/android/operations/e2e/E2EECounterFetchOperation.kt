/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.e2e

import android.content.Context
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.utils.e2ee.E2EVersionHelper.isV2Plus
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.utils.theme.CapabilityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class E2EECounterFetchOperation: Injectable {
    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var clientFactory: ClientFactory

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        private const val TAG = "E2EECounterFetchOperation"
    }

    fun fetchAsync(
        file: OCFile,
        storageManager: FileDataStorageManager,
        context: Context,
        onComplete: (Long) -> Unit
    ) {
        scope.launch {
            val counter = resolveE2EECounter(file, storageManager, context)
            withContext(Dispatchers.Main) {
                onComplete(counter)
            }
        }
    }

    fun resolveE2EECounter(
        file: OCFile,
        storageManager: FileDataStorageManager,
        context: Context
    ): Long {
        return try {
            val client = clientFactory.create(accountManager.user)
            val metadata = RefreshFolderOperation
                .getDecryptedFolderMetadata(true, file, client, accountManager.user, context)
            if (metadata is DecryptedFolderMetadataFile) {
                file.setE2eCounter(metadata.metadata.counter)
                storageManager.saveFile(file)
                Log_OC.i(TAG, "latest counter fetched")
                metadata.metadata.counter
            } else {
                Log_OC.w(TAG, "local counter is used")
                getFallbackCounter(context, file)
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, "error refreshing E2E counter: ${e.message}")
            getFallbackCounter(context, file)
        }
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
