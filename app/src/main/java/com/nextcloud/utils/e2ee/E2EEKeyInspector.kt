/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.e2ee

import android.content.Context
import com.google.gson.reflect.TypeToken
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.e2e.v1.encrypted.EncryptedFolderMetadataFileV1
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.e2ee.GetMetadataRemoteOperation
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.lib.resources.users.GetCertificateRemoteOperation
import com.owncloud.android.lib.resources.users.GetServerPublicKeyRemoteOperation
import com.owncloud.android.operations.GetCapabilitiesOperation
import com.owncloud.android.ui.dialog.setupEncryption.CertificateValidator
import com.owncloud.android.ui.dialog.setupEncryption.model.DownloadKeyResult
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.EncryptionUtilsV2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.httpclient.HttpStatus
import java.security.cert.CertificateException
import javax.inject.Inject

@Suppress("TooGenericExceptionCaught", "ReturnCount", "DEPRECATION")
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
            arbitraryDataProvider.getValue(accountManager.user, EncryptionUtils.CERTIFICATE).isEmpty()

    suspend fun fetchCapabilities(): Boolean = withContext(Dispatchers.IO) {
        val result = GetCapabilitiesOperation(storageManager).execute(context).isSuccess
        if (result) {
            Log_OC.i(TAG, "capabilities fetched")
        } else {
            Log_OC.e(TAG, "error fetching capabilities")
        }
        return@withContext result
    }

    suspend fun compareWithServerCertificate(storedCertificate: String): DownloadKeyResult =
        withContext(Dispatchers.IO) {
            Log_OC.d(TAG, "comparing stored certificate against server certificate")

            val user = accountManager.user

            val certificateResult = GetCertificateRemoteOperation().executeNextcloudClient(user, context)
            if (!certificateResult.isSuccess) {
                return@withContext if (certificateResult.httpCode == HttpStatus.SC_NOT_FOUND) {
                    DownloadKeyResult.NoCertificateOnServer
                } else {
                    DownloadKeyResult.CertificateUnavailable()
                }
            }

            val remoteCertificate = certificateResult.resultData
            val storedServerPublicKey = arbitraryDataProvider.getValue(user, EncryptionUtils.SERVER_PUBLIC_KEY)
            if (storedServerPublicKey.isEmpty()) {
                return@withContext fetchServerPublicKeyStoreAndCompare(user, storedCertificate, remoteCertificate)
            }

            val isSignedByStoredServerPublicKey = certificateValidator.validate(
                serverPublicKeyString = storedServerPublicKey,
                certificate = remoteCertificate
            )
            if (!isSignedByStoredServerPublicKey) {
                Log_OC.w(TAG, "certificate cannot be verified with the stored server public key")
                return@withContext DownloadKeyResult.CertificateNotSignedByStoredServerKey
            }

            return@withContext DownloadKeyResult.CompareKeys(
                same = haveSamePublicKey(storedCertificate, remoteCertificate)
            )
        }

    private fun fetchServerPublicKeyStoreAndCompare(
        user: User,
        storedCertificate: String,
        remoteCertificate: String
    ): DownloadKeyResult {
        val serverPublicKeyResult = GetServerPublicKeyRemoteOperation().executeNextcloudClient(user, context)
        if (!serverPublicKeyResult.isSuccess) return DownloadKeyResult.ServerPublicKeyUnavailable()

        val serverPublicKey = serverPublicKeyResult.resultData
        if (!certificateValidator.validate(serverPublicKeyString = serverPublicKey, certificate = remoteCertificate)) {
            return DownloadKeyResult.CertificateNotSignedByCurrentServerKey()
        }

        arbitraryDataProvider.storeOrUpdateKeyValue(user, EncryptionUtils.SERVER_PUBLIC_KEY, serverPublicKey)
        return DownloadKeyResult.CompareKeys(same = haveSamePublicKey(storedCertificate, remoteCertificate))
    }

    private fun haveSamePublicKey(firstCertificate: String, secondCertificate: String): Boolean = try {
        val firstPublicKey = EncryptionUtils.convertPublicKeyFromString(firstCertificate)
        val secondPublicKey = EncryptionUtils.convertPublicKeyFromString(secondCertificate)
        firstPublicKey.encoded.contentEquals(secondPublicKey.encoded)
    } catch (e: CertificateException) {
        Log_OC.w(TAG, "certificate could not be parsed: $e")
        false
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
                decryptsMetadataV2(folder, privateKey, client)
            } else {
                decryptsMetadataV1(metadata.metadata, privateKey, folder.localId)
            }
        }

    private fun decryptsMetadataV2(ocFile: OCFile, privateKey: String, client: OwnCloudClient): Boolean {
        val userId = client.userId

        return try {
            val metadataKey = EncryptionUtilsV2().retrieveTopMostMetadataKey(
                ocFile,
                storageManager,
                client,
                userId,
                privateKey,
                accountManager.user,
                context,
                arbitraryDataProvider
            )
            metadataKey.isNotEmpty()
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
