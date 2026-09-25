/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Daniele Verducci <daniele.verducci@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.dialog.setupEncryption

import android.accounts.AccountManager
import android.content.Context
import com.nextcloud.client.account.User
import com.nextcloud.utils.e2ee.E2EVersionHelper
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.e2ee.CsrHelper
import com.owncloud.android.lib.resources.status.E2EVersion
import com.owncloud.android.lib.resources.users.DeletePublicKeyRemoteOperation
import com.owncloud.android.lib.resources.users.SendCSRRemoteOperation
import com.owncloud.android.lib.resources.users.StorePrivateKeyRemoteOperation
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.EncryptionUtils.RSA
import com.owncloud.android.utils.EncryptionUtilsV2
import com.owncloud.android.utils.crypto.CryptoHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom

class EncryptionKeyGenerator(val context: Context, val user: User) {
    companion object {
        val TAG: String = EncryptionKeyGenerator::class.java.simpleName

        fun generateMnemonicString(keyWords: List<String>, withWhitespace: Boolean): String =
            keyWords.joinToString("") { if (withWhitespace) "$it " else it }

        @Throws(NoSuchAlgorithmException::class)
        fun generateKeyPair(): KeyPair =
            KeyPairGenerator.getInstance(RSA)
                .apply { initialize(2048, SecureRandom()) }
                .generateKeyPair()
    }

    @Suppress("TooGenericExceptionCaught", "TooGenericExceptionThrown", "ReturnCount")
    suspend fun generatePrivateKey(keyWords: ArrayList<String>): PrivateKeyResult = withContext(Dispatchers.IO) {
        val arbitraryDataProvider = ArbitraryDataProviderImpl(context)

        //  - create CSR, push to server, store returned public key in database
        //  - encrypt private key, push key to server, store unencrypted private key in database
        try {
            val certificate: String

            // Create public/private key pair
            val keyPair = EncryptionKeyGenerator.generateKeyPair()

            // create CSR
            val accountManager = AccountManager.get(context)

            val userId = accountManager.getUserData(user.toPlatformAccount(), AccountUtils.Constants.KEY_USER_ID)
            val urlEncoded = CsrHelper().generateCsrPemEncodedString(keyPair, userId)
            val operation = SendCSRRemoteOperation(urlEncoded)
            val result = operation.executeNextcloudClient(user, context)

            if (!result.isSuccess) {
                return@withContext PrivateKeyResult.Failed
            }

            certificate = result.resultData
            if (!EncryptionUtils.isMatchingKeys(keyPair, certificate)) {
                EncryptionUtils.reportE2eError(arbitraryDataProvider, user)
                throw RuntimeException("Wrong CSR returned")
            }
            Log_OC.d(TAG, "public key success")

            val privateKey = keyPair.private
            val privateKeyString = EncryptionUtils.encodeBytesToBase64String(privateKey.encoded)
            val privatePemKeyString = EncryptionUtils.privateKeyToPEM(privateKey)
            val encryptedPrivateKey = CryptoHelper.encryptPrivateKey(
                privatePemKeyString,
                generateMnemonicString(keyWords, false)
            )

            // upload encryptedPrivateKey
            val storePrivateKeyOperation = StorePrivateKeyRemoteOperation(encryptedPrivateKey)
            val storePrivateKeyResult = storePrivateKeyOperation.executeNextcloudClient(user, context)
            if (!storePrivateKeyResult.isSuccess) {
                val deletePublicKeyOperation = DeletePublicKeyRemoteOperation()
                deletePublicKeyOperation.executeNextcloudClient(user, context)
                return@withContext PrivateKeyResult.Failed
            }

            Log_OC.d(TAG, "private key success")
            arbitraryDataProvider.storeOrUpdateKeyValue(
                user.accountName,
                EncryptionUtils.PRIVATE_KEY,
                privateKeyString
            )
            arbitraryDataProvider.storeOrUpdateKeyValue(
                user.accountName,
                EncryptionUtils.PUBLIC_KEY,
                certificate
            )
            arbitraryDataProvider.storeOrUpdateKeyValue(
                user.accountName,
                EncryptionUtils.MNEMONIC,
                generateMnemonicString(keyWords, true)
            )
            return@withContext PrivateKeyResult.Success(storePrivateKeyResult.resultData)
        } catch (e: Exception) {
            Log_OC.e(TAG, e.message)
        }
        return@withContext PrivateKeyResult.Failed
    }

    @Suppress("LongParameterList")
    fun uploadEncryptedFolderMetadata(
        folder: OCFile,
        client: OwnCloudClient,
        publicKey: String,
        privateKey: String,
        storageManager: FileDataStorageManager,
        arbitraryDataProvider: ArbitraryDataProvider
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
                    context,
                    arbitraryDataProvider
                )
                val encryptionUtil = EncryptionUtilsV2()
                encryptionUtil.serializeAndUploadMetadata(
                    folder,
                    result.second,
                    token,
                    client,
                    result.first,
                    context,
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

        return result
    }

    sealed class PrivateKeyResult {
        data class Success(val key: String): PrivateKeyResult()
        data object Failed: PrivateKeyResult()
    }

}
