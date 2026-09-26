/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.e2ee

import android.content.Context
import com.google.gson.reflect.TypeToken
import com.nextcloud.client.account.UserAccountManager
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.e2e.v1.encrypted.EncryptedFolderMetadataFileV1
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedFolderMetadataFile
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.e2ee.GetMetadataRemoteOperation
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.lib.resources.users.GetPublicKeyRemoteOperation
import com.owncloud.android.lib.resources.users.GetServerPublicKeyRemoteOperation
import com.owncloud.android.operations.GetCapabilitiesOperation
import com.owncloud.android.ui.dialog.setupEncryption.CertificateValidator
import com.owncloud.android.ui.dialog.setupEncryption.model.DownloadKeyResult
import com.owncloud.android.utils.EncryptionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.httpclient.HttpStatus
import javax.inject.Inject

@Suppress("TooGenericExceptionCaught", "ReturnCount")
class E2EEKeyInspector @Inject constructor(
    private val context: Context,
    private val storageManager: FileDataStorageManager,
    private val certificateValidator: CertificateValidator,
    private val arbitraryDataProvider: ArbitraryDataProvider,
    private val accountManager: UserAccountManager
) {
    companion object {
        private const val TAG = "E2EEKeyInspector"
    }

    fun isLocalKeysAbsent(): Boolean =
        arbitraryDataProvider.getValue(accountManager.user, EncryptionUtils.PRIVATE_KEY).isEmpty() ||
            arbitraryDataProvider.getValue(accountManager.user, EncryptionUtils.PUBLIC_KEY).isEmpty()

    suspend fun fetchCapabilities(): Boolean = withContext(Dispatchers.IO) {
        val result = GetCapabilitiesOperation(storageManager).execute(context).isSuccess
        if (result) {
            Log_OC.i(TAG, "capabilities fetched")
        } else {
            Log_OC.e(TAG, "error fetching capabilities")
        }
        return@withContext result
    }

    suspend fun compareWithServerKey(storedPublicKey: String): DownloadKeyResult = withContext(Dispatchers.IO) {
        Log_OC.d(TAG, "comparing local public key with remote public key")

        val user = accountManager.user

        val certificateResult = GetPublicKeyRemoteOperation().executeNextcloudClient(user, context)
        if (!certificateResult.isSuccess) {
            return@withContext if (certificateResult.httpCode == HttpStatus.SC_NOT_FOUND) {
                DownloadKeyResult.NoServerKey
            } else {
                DownloadKeyResult.CertificateUnavailable()
            }
        }

        val serverPublicKeyResult = GetServerPublicKeyRemoteOperation().executeNextcloudClient(user, context)
        if (!serverPublicKeyResult.isSuccess) return@withContext DownloadKeyResult.ServerPublicKeyUnavailable()

        val isCertificateValid = certificateValidator.validate(
            serverPublicKeyResult.resultData,
            certificateResult.resultData
        )
        if (!isCertificateValid) return@withContext DownloadKeyResult.CertificateVerificationFailed()

        return@withContext DownloadKeyResult.CompareKeys(same = (storedPublicKey == certificateResult.resultData))
    }

    suspend fun canDecryptFolderMetadata(folder: OCFile, capability: OCCapability): Boolean =
        withContext(Dispatchers.IO) {
            Log_OC.d(TAG, "checking folder metadata key")

            val client = OwnCloudClientFactory.createOwnCloudClient(accountManager.currentAccount, context)
            val metadataResult = GetMetadataRemoteOperation(folder.localId).execute(client)

            if (!metadataResult.isSuccess) {
                return@withContext false
            }

            val privateKey = arbitraryDataProvider.getValue(accountManager.user, EncryptionUtils.PRIVATE_KEY)
            if (privateKey.isEmpty()) {
                Log_OC.e(TAG, "user try to decrypt folder with empty private key")
                return@withContext false
            }

            val metadata = metadataResult.resultData

            return@withContext if (E2EVersionHelper.isV2Plus(capability)) {
                decryptsMetadataV2(metadata.metadata, privateKey, client.userId)
            } else {
                decryptsMetadataV1(metadata.metadata, privateKey, folder.localId)
            }
        }

    private fun decryptsMetadataV2(serializedMetadata: String, privateKey: String, userId: String): Boolean {
        val metadataFile = EncryptionUtils.deserializeJSON(
            serializedMetadata,
            object : TypeToken<EncryptedFolderMetadataFile>() {}
        )

        val user = metadataFile.users.find { it.userId == userId }
            ?: throw IllegalStateException("cannot find current user in metadata")

        return try {
            EncryptionUtils.decryptStringAsymmetricV2(user.encryptedMetadataKey, privateKey)
            true
        } catch (e: Exception) {
            Log_OC.w(TAG, "user tried to decrypt folder's metadata with different private key: $e")
            false
        }
    }

    private fun decryptsMetadataV1(serializedMetadata: String, privateKey: String, folderLocalId: Long): Boolean {
        val metadataFile = EncryptionUtils.deserializeJSON(
            serializedMetadata,
            object : TypeToken<EncryptedFolderMetadataFileV1?>() {}
        )

        return try {
            EncryptionUtils.decryptFolderMetaData(
                metadataFile,
                privateKey,
                arbitraryDataProvider,
                accountManager.user,
                folderLocalId
            )
            true
        } catch (e: Exception) {
            Log_OC.w(TAG, "user tried to decrypt folder's metadata with different private key: $e")
            false
        }
    }
}
