/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.operations.e2e

import android.content.Context
import com.nextcloud.client.account.UserAccountManager
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class E2EECounterFetchOperation {
    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var clientFactory: ClientFactory

    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "E2EECounterFetchOperation"
    }

    fun execute(
        file: OCFile,
        storageManager: FileDataStorageManager,
        context: Context,
        onComplete: (Long) -> Unit
    ) {
        job = scope.launch(Dispatchers.IO) {
            var counter = getE2ECounter(context, file)

            runCatching {
                val client = clientFactory.create(accountManager.user)
                val metadata = RefreshFolderOperation
                    .getDecryptedFolderMetadata(true, file, client, accountManager.user, context)
                if (metadata is DecryptedFolderMetadataFile) {
                    counter = metadata.metadata.counter
                    file.setE2eCounter(metadata.metadata.counter)
                    storageManager.saveFile(file)
                }
            }.onFailure { e ->
                Log_OC.e(TAG, "Error refreshing E2E counter: ${e.message}")
            }

            withContext(Dispatchers.Main) {
                onComplete(counter)
            }
        }
    }

    private fun isEndToEndVersionAtLeastV2(context: Context): Boolean {
        val capability = CapabilityUtils.getCapability(context)
        return isV2Plus(capability)
    }

    private fun getE2ECounter(context: Context, file: OCFile): Long {
        var counter: Long = -1

        if (isEndToEndVersionAtLeastV2(context)) {
            counter = file.e2eCounter + 1
        }

        return counter
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
