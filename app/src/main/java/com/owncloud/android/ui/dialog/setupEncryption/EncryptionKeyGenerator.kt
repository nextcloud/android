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
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.e2ee.CsrHelper
import com.owncloud.android.lib.resources.users.DeletePublicKeyRemoteOperation
import com.owncloud.android.lib.resources.users.SendCSRRemoteOperation
import com.owncloud.android.lib.resources.users.StorePrivateKeyRemoteOperation
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.crypto.CryptoHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object EncryptionKeyGenerator {
    
    val TAG: String = EncryptionKeyGenerator::class.java.simpleName

    suspend fun generatePrivateKey(
        context: Context,
        user: User,
        keyWords: ArrayList<String>?
    ): String= withContext(Dispatchers.IO) {

        val arbitraryDataProvider = ArbitraryDataProviderImpl(context)

        //  - create CSR, push to server, store returned public key in database
        //  - encrypt private key, push key to server, store unencrypted private key in database
        try {
            val certificate: String

            // Create public/private key pair
            val keyPair = EncryptionUtils.generateKeyPair()

            // create CSR
            val accountManager = AccountManager.get(context)

            val userId = accountManager.getUserData(user.toPlatformAccount(), AccountUtils.Constants.KEY_USER_ID)
            val urlEncoded = CsrHelper().generateCsrPemEncodedString(keyPair, userId)
            val operation = SendCSRRemoteOperation(urlEncoded)
            val result = operation.executeNextcloudClient(user, context)

            if (result.isSuccess) {
                certificate = result.resultData
                if (!EncryptionUtils.isMatchingKeys(keyPair, certificate)) {
                    EncryptionUtils.reportE2eError(arbitraryDataProvider, user)
                    throw RuntimeException("Wrong CSR returned")
                }
                Log_OC.d(TAG, "public key success")
            } else {
                return@withContext ""
            }

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
            if (storePrivateKeyResult.isSuccess) {
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

                return@withContext storePrivateKeyResult.resultData
            } else {
                val deletePublicKeyOperation = DeletePublicKeyRemoteOperation()
                deletePublicKeyOperation.executeNextcloudClient(user, context)
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, e.message)
        }
        return@withContext ""
    }

    fun generateMnemonicString(keyWords: ArrayList<String>?, withWhitespace: Boolean): String {
        val stringBuilder = StringBuilder()

        keyWords?.let {
            for (string in it) {
                stringBuilder.append(string)
                if (withWhitespace) {
                    stringBuilder.append(' ')
                }
            }
        }

        return stringBuilder.toString()
    }
}