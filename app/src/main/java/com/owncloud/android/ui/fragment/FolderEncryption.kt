/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment

import com.nextcloud.client.account.User
import com.nextcloud.utils.e2ee.E2EVersionHelper
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.e2ee.ToggleEncryptionRemoteOperation
import com.owncloud.android.lib.resources.status.E2EVersion
import com.owncloud.android.ui.events.EncryptionEvent
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.EncryptionUtilsV2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.httpclient.HttpStatus

@Suppress("TooGenericExceptionCaught")
class FolderEncryption(private val fragment: OCFileListFragment) {

    companion object {
        private const val TAG = "FolderEncryption"
    }

    suspend fun toggle(event: EncryptionEvent): Boolean = withContext(Dispatchers.IO) {
        val localId = event.localId
        val remoteId = event.remoteId
        val remotePath = event.remotePath
        val shouldBeEncrypted = event.shouldBeEncrypted

        try {
            val storageManager = fragment.mContainerActivity.storageManager
            val folder = storageManager.getFileByRemoteId(remoteId) ?: run {
                Log_OC.e(TAG, "folder is null, cannot encrypt")
                return@withContext false
            }

            val user = fragment.accountManager.user
            val provider = fragment.arbitraryDataProvider
            val publicKey = provider.getValue(user, EncryptionUtils.PUBLIC_KEY)
            val privateKey = provider.getValue(user, EncryptionUtils.PRIVATE_KEY)
            val client = fragment.clientFactory.create(user)
            val nextcloudClient = fragment.clientFactory.createNextcloudClient(user)

            val result = ToggleEncryptionRemoteOperation(localId, remotePath, shouldBeEncrypted)
                .execute(nextcloudClient)

            return@withContext when {
                result.isSuccess -> onToggleSuccess(
                    remoteId,
                    shouldBeEncrypted,
                    folder,
                    client,
                    user,
                    publicKey,
                    privateKey,
                    storageManager
                )

                result.httpCode == HttpStatus.SC_FORBIDDEN -> {
                    showSnackbar(
                        R.string.end_to_end_encryption_folder_not_empty
                    )
                    false
                }

                else -> {
                    showSnackbar(R.string.common_error_unknown)
                    false
                }
            }
        } catch (t: Throwable) {
            Log_OC.e(TAG, "error encrypting folder", t)
            false
        }
    }

    @Suppress("LongParameterList")
    private suspend fun onToggleSuccess(
        remoteId: String,
        shouldBeEncrypted: Boolean,
        folder: OCFile,
        client: OwnCloudClient,
        user: User,
        publicKey: String,
        privateKey: String,
        storageManager: FileDataStorageManager
    ): Boolean {
        val capability = storageManager.getCapability(user.accountName)
        val isE2EEV2 = E2EVersionHelper.isV2Plus(capability)
        var e2eCounter = EncryptionUtils.E2E_V1_INITIAL_COUNTER
        if (isE2EEV2) {
            e2eCounter = EncryptionUtils.E2E_V2_INITIAL_COUNTER
        }
        val token = EncryptionUtils.lockFolder(folder, client, e2eCounter)

        val result = when {
            isE2EEV2 -> {
                val result = EncryptionUtils.retrieveMetadata(
                    folder,
                    client,
                    privateKey,
                    publicKey,
                    storageManager,
                    user,
                    fragment.requireContext(),
                    fragment.arbitraryDataProvider
                )
                val encryptionUtil = EncryptionUtilsV2()
                encryptionUtil.serializeAndUploadMetadata(
                    folder,
                    result.second,
                    token,
                    client,
                    result.first,
                    fragment.requireContext(),
                    user,
                    storageManager
                )
                EncryptionUtils.unlockFolder(folder, client, token)
                true
            }

            E2EVersionHelper.isV1(capability) -> {
                EncryptionUtils.unlockFolderV1(folder, client, token)
                false
            }

            capability.endToEndEncryptionApiVersion == E2EVersion.UNKNOWN -> {
                throw IllegalArgumentException("Unknown E2E version")
            }

            else -> {
                false
            }
        }

        withContext(Dispatchers.Main) {
            val isFileExists = (fragment.adapter.getFileByRemoteId(remoteId) != null)
            if (!isFileExists) {
                val newFile = storageManager.getFileByRemoteId(remoteId)
                fragment.adapter.insertFile(newFile)
            }

            fragment.adapter.updateFileEncryptionById(remoteId, shouldBeEncrypted)
        }

        return result
    }

    private suspend fun showSnackbar(messageResId: Int) = withContext(Dispatchers.Main) {
        DisplayUtils.showSnackMessage(fragment, messageResId)
    }
}
