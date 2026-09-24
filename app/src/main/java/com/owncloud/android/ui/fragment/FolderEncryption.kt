/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment

import com.nextcloud.client.account.User
import com.nextcloud.utils.SnackbarUtil
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.e2ee.ToggleEncryptionRemoteOperation
import com.owncloud.android.ui.dialog.setupEncryption.EncryptionKeyGenerator
import com.owncloud.android.ui.events.EncryptionEvent
import com.owncloud.android.utils.EncryptionUtils
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
            val storageManager = fragment.containerActivity.storageManager
            val folder = storageManager.getFileByRemoteId(remoteId) ?: run {
                Log_OC.e(TAG, "folder is null, cannot encrypt")
                return@withContext false
            }

            val user = fragment.accountManager.user
            val provider = fragment.arbitraryDataProvider
            val publicKey = provider.getValue(user, EncryptionUtils.PUBLIC_KEY)
            val privateKey = provider.getValue(user, EncryptionUtils.PRIVATE_KEY)
            val client = fragment.clientFactory.create(user)

            val result = ToggleEncryptionRemoteOperation(localId, remotePath, shouldBeEncrypted)
                .execute(client)

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
    suspend fun onToggleSuccess(
        remoteId: String,
        shouldBeEncrypted: Boolean,
        folder: OCFile,
        client: OwnCloudClient,
        user: User,
        publicKey: String,
        privateKey: String,
        storageManager: FileDataStorageManager
    ): Boolean {
        val result = EncryptionKeyGenerator(fragment.requireContext(), user)
            .uploadEncryptedFolderMetadata(
                folder,
                client,
                publicKey,
                privateKey,
                storageManager,
                fragment.arbitraryDataProvider
            )

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
        SnackbarUtil.show(fragment, messageResId)
    }
}
