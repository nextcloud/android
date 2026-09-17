/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.e2ee

import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.utils.e2ee.model.E2EEKeyCheck
import com.nextcloud.utils.extensions.isNetworkAndServerAvailableSuspended
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.dialog.setupEncryption.model.DownloadKeyResult
import com.owncloud.android.utils.EncryptionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("LongParameterList")
class E2EEActionResolver @Inject constructor(
    private val storageManager: FileDataStorageManager,
    private val arbitraryDataProvider: ArbitraryDataProvider,
    private val accountManager: UserAccountManager,
    private val connectivityService: ConnectivityService,
    private val inspector: E2EEKeyInspector
) : Injectable {

    companion object {
        private const val TAG = "E2EEActionResolver"
    }

    suspend fun markFolderReadOnly(file: OCFile) = withContext(Dispatchers.IO) {
        storageManager.setReadOnly(file, true)
    }

    suspend fun checkFolderMetadataKey(file: OCFile): Boolean = withContext(Dispatchers.IO) {
        val capability = storageManager.getCapability(accountManager.user)
        val canDecrypt = inspector.canDecryptFolderMetadata(file, capability)
        storageManager.setReadOnly(file, !canDecrypt)
        return@withContext canDecrypt
    }

    suspend fun checkKeys(): E2EEKeyCheck = withContext(Dispatchers.IO) { resolveKeyCheck() }

    private suspend fun resolveKeyCheck(): E2EEKeyCheck {
        val keysAbsentLocally = inspector.isLocalKeysAbsent()

        if (!inspector.fetchCapabilities()) {
            return if (connectivityService.isNetworkAndServerAvailableSuspended()) {
                E2EEKeyCheck.CHECK_FAILED
            } else {
                E2EEKeyCheck.NO_NETWORK
            }
        }

        val capability = storageManager.getCapability(accountManager.user)
        val keysExistOnServer = capability.endToEndEncryptionKeysExist

        val result = when {
            keysExistOnServer.isUnknown || capability.endToEndEncryption.isUnknown -> E2EEKeyCheck.E2EE_UNAVAILABLE
            keysAbsentLocally && keysExistOnServer.isTrue -> E2EEKeyCheck.ONLY_ON_SERVER
            keysAbsentLocally -> E2EEKeyCheck.MISSING_EVERYWHERE
            !keysExistOnServer.isTrue -> E2EEKeyCheck.ONLY_ON_DEVICE
            else -> compareKeys()
        }

        Log_OC.d(TAG, "e2ee key check result: $result")

        return result
    }

    private suspend fun compareKeys(): E2EEKeyCheck {
        val storedPublicKey = arbitraryDataProvider.getValue(accountManager.user, EncryptionUtils.PUBLIC_KEY)

        return when (val result = inspector.compareWithServerKey(storedPublicKey)) {
            is DownloadKeyResult.CompareKeys ->
                if (result.same) E2EEKeyCheck.SAME_AS_SERVER else E2EEKeyCheck.DIFFERS_FROM_SERVER

            is DownloadKeyResult.NoServerKey -> E2EEKeyCheck.ONLY_ON_DEVICE

            else -> E2EEKeyCheck.CHECK_FAILED
        }
    }
}
