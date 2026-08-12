/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.utils.EncryptionUtils
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

@Singleton
class E2eeVaultSecretStore @Inject constructor(
    private val arbitraryDataProvider: ArbitraryDataProvider,
    private val cipher: E2eeVaultSecretCipher
) {
    fun hasSecrets(accountName: String): Boolean =
        arbitraryDataProvider.getValue(accountName, ENCRYPTED_SECRETS_KEY).isNotEmpty() ||
            readLegacySecrets(accountName) != null

    fun getPrivateKey(accountName: String): String? = getSecrets(accountName)?.privateKey

    fun getMnemonic(accountName: String): String? = getSecrets(accountName)?.mnemonic

    fun getSecrets(accountName: String): E2eeVaultSecrets? {
        val encryptedPayload = arbitraryDataProvider.getValue(accountName, ENCRYPTED_SECRETS_KEY)

        if (encryptedPayload.isNotEmpty()) {
            return decryptSecrets(accountName, encryptedPayload)
        }

        return migrateLegacySecrets(accountName)
    }

    fun storeSecrets(accountName: String, secrets: E2eeVaultSecrets) {
        val serializedSecrets = serializeSecrets(secrets)
        val encryptedPayload = cipher.encrypt(accountName, serializedSecrets.toByteArray(StandardCharsets.UTF_8))

        arbitraryDataProvider.storeOrUpdateKeyValue(
            accountName,
            ENCRYPTED_SECRETS_KEY,
            serializePayload(encryptedPayload)
        )
    }

    fun deleteSecrets(accountName: String) {
        arbitraryDataProvider.deleteKeyForAccount(accountName, ENCRYPTED_SECRETS_KEY)
        arbitraryDataProvider.deleteKeyForAccount(accountName, MIGRATED_MARKER_KEY)
        arbitraryDataProvider.deleteKeyForAccount(accountName, EncryptionUtils.PRIVATE_KEY)
        arbitraryDataProvider.deleteKeyForAccount(accountName, EncryptionUtils.MNEMONIC)
        cipher.deleteKey(accountName)
    }

    private fun migrateLegacySecrets(accountName: String): E2eeVaultSecrets? {
        val legacySecrets = readLegacySecrets(accountName) ?: return null

        storeSecrets(accountName, legacySecrets)
        val migratedSecrets =
            decryptSecrets(accountName, arbitraryDataProvider.getValue(accountName, ENCRYPTED_SECRETS_KEY))

        if (migratedSecrets != legacySecrets) {
            arbitraryDataProvider.deleteKeyForAccount(accountName, ENCRYPTED_SECRETS_KEY)
            throw SecurityException("E2EE vault secret migration verification failed")
        }

        arbitraryDataProvider.deleteKeyForAccount(accountName, EncryptionUtils.PRIVATE_KEY)
        arbitraryDataProvider.deleteKeyForAccount(accountName, EncryptionUtils.MNEMONIC)
        arbitraryDataProvider.storeOrUpdateKeyValue(accountName, MIGRATED_MARKER_KEY, true.toString())

        return migratedSecrets
    }

    private fun readLegacySecrets(accountName: String): E2eeVaultSecrets? {
        val privateKey = arbitraryDataProvider.getValue(accountName, EncryptionUtils.PRIVATE_KEY)
        val mnemonic = arbitraryDataProvider.getValue(accountName, EncryptionUtils.MNEMONIC)

        if (privateKey.isEmpty() || mnemonic.isEmpty()) {
            return null
        }

        return E2eeVaultSecrets(privateKey, mnemonic)
    }

    private fun decryptSecrets(accountName: String, serializedPayload: String): E2eeVaultSecrets {
        val payload = deserializePayload(serializedPayload)
        val decryptedPayload = cipher.decrypt(accountName, payload)

        return deserializeSecrets(String(decryptedPayload, StandardCharsets.UTF_8))
    }

    private fun serializePayload(payload: E2eeVaultEncryptedPayload): String = JSONObject()
        .put(JSON_VERSION, CURRENT_VERSION)
        .put(JSON_INITIALIZATION_VECTOR, payload.initializationVector)
        .put(JSON_CIPHERTEXT, payload.ciphertext)
        .toString()

    private fun deserializePayload(serializedPayload: String): E2eeVaultEncryptedPayload {
        val json = JSONObject(serializedPayload)
        require(json.getInt(JSON_VERSION) == CURRENT_VERSION)

        return E2eeVaultEncryptedPayload(
            initializationVector = json.getString(JSON_INITIALIZATION_VECTOR),
            ciphertext = json.getString(JSON_CIPHERTEXT)
        )
    }

    private fun serializeSecrets(secrets: E2eeVaultSecrets): String = JSONObject()
        .put(JSON_VERSION, CURRENT_VERSION)
        .put(JSON_PRIVATE_KEY, secrets.privateKey)
        .put(JSON_MNEMONIC, secrets.mnemonic)
        .toString()

    private fun deserializeSecrets(serializedSecrets: String): E2eeVaultSecrets {
        val json = JSONObject(serializedSecrets)
        require(json.getInt(JSON_VERSION) == CURRENT_VERSION)

        return E2eeVaultSecrets(
            privateKey = json.getString(JSON_PRIVATE_KEY),
            mnemonic = json.getString(JSON_MNEMONIC)
        )
    }

    companion object {
        const val ENCRYPTED_SECRETS_KEY = "E2EE_VAULT_SECRETS_V1"
        const val MIGRATED_MARKER_KEY = "E2EE_VAULT_SECRETS_MIGRATED_V1"

        private const val CURRENT_VERSION = 1
        private const val JSON_CIPHERTEXT = "ciphertext"
        private const val JSON_INITIALIZATION_VECTOR = "iv"
        private const val JSON_MNEMONIC = "mnemonic"
        private const val JSON_PRIVATE_KEY = "privateKey"
        private const val JSON_VERSION = "version"
    }
}
