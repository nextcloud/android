/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.utils.EncryptionUtils
import java.nio.charset.StandardCharsets
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class E2eeVaultSecretStoreTest {
    private val arbitraryDataProvider = FakeArbitraryDataProvider()
    private val cipher = FakeE2eeVaultSecretCipher()
    private val secretStore = E2eeVaultSecretStore(arbitraryDataProvider, cipher)

    @Test
    fun storeSecretsStoresEncryptedPayloadOnly() {
        secretStore.storeSecrets(ACCOUNT_NAME, SECRETS)

        val storedPayload = arbitraryDataProvider.getValue(ACCOUNT_NAME, E2eeVaultSecretStore.ENCRYPTED_SECRETS_KEY)

        assertTrue(storedPayload.isNotEmpty())
        assertFalse(storedPayload.contains(SECRETS.privateKey))
        assertFalse(storedPayload.contains(SECRETS.mnemonic))
        assertEquals(SECRETS, secretStore.getSecrets(ACCOUNT_NAME))
    }

    @Test
    fun getSecretsMigratesLegacySecretsAndDeletesLegacyValues() {
        arbitraryDataProvider.storeOrUpdateKeyValue(ACCOUNT_NAME, EncryptionUtils.PRIVATE_KEY, SECRETS.privateKey)
        arbitraryDataProvider.storeOrUpdateKeyValue(ACCOUNT_NAME, EncryptionUtils.MNEMONIC, SECRETS.mnemonic)

        val migratedSecrets = secretStore.getSecrets(ACCOUNT_NAME)

        assertEquals(SECRETS, migratedSecrets)
        assertEquals("", arbitraryDataProvider.getValue(ACCOUNT_NAME, EncryptionUtils.PRIVATE_KEY))
        assertEquals("", arbitraryDataProvider.getValue(ACCOUNT_NAME, EncryptionUtils.MNEMONIC))
        assertEquals("true", arbitraryDataProvider.getValue(ACCOUNT_NAME, E2eeVaultSecretStore.MIGRATED_MARKER_KEY))
    }

    @Test
    fun getSecretsKeepsLegacyValuesWhenMigrationVerificationFails() {
        arbitraryDataProvider.storeOrUpdateKeyValue(ACCOUNT_NAME, EncryptionUtils.PRIVATE_KEY, SECRETS.privateKey)
        arbitraryDataProvider.storeOrUpdateKeyValue(ACCOUNT_NAME, EncryptionUtils.MNEMONIC, SECRETS.mnemonic)
        cipher.decryptResult =
            """{"version":1,"privateKey":"wrong","mnemonic":"wrong"}""".toByteArray(StandardCharsets.UTF_8)

        val result = runCatching { secretStore.getSecrets(ACCOUNT_NAME) }

        assertTrue(result.exceptionOrNull() is SecurityException)
        assertEquals(SECRETS.privateKey, arbitraryDataProvider.getValue(ACCOUNT_NAME, EncryptionUtils.PRIVATE_KEY))
        assertEquals(SECRETS.mnemonic, arbitraryDataProvider.getValue(ACCOUNT_NAME, EncryptionUtils.MNEMONIC))
        assertEquals("", arbitraryDataProvider.getValue(ACCOUNT_NAME, E2eeVaultSecretStore.ENCRYPTED_SECRETS_KEY))
    }

    @Test
    fun getSecretsKeepsEncryptedPayloadWhenCipherAccessIsLocked() {
        secretStore.storeSecrets(ACCOUNT_NAME, SECRETS)
        val storedPayload = arbitraryDataProvider.getValue(ACCOUNT_NAME, E2eeVaultSecretStore.ENCRYPTED_SECRETS_KEY)
        cipher.decryptFailure = SecurityException("locked")

        val result = runCatching { secretStore.getSecrets(ACCOUNT_NAME) }

        assertTrue(result.exceptionOrNull() is SecurityException)
        assertEquals(
            storedPayload,
            arbitraryDataProvider.getValue(ACCOUNT_NAME, E2eeVaultSecretStore.ENCRYPTED_SECRETS_KEY)
        )
        assertEquals("", arbitraryDataProvider.getValue(ACCOUNT_NAME, EncryptionUtils.PRIVATE_KEY))
        assertEquals("", arbitraryDataProvider.getValue(ACCOUNT_NAME, EncryptionUtils.MNEMONIC))
    }

    @Test
    fun getSecretsFailsWhenEncryptedPayloadBelongsToDifferentAccount() {
        secretStore.storeSecrets(ACCOUNT_NAME, SECRETS)
        arbitraryDataProvider.storeOrUpdateKeyValue(
            OTHER_ACCOUNT_NAME,
            E2eeVaultSecretStore.ENCRYPTED_SECRETS_KEY,
            arbitraryDataProvider.getValue(ACCOUNT_NAME, E2eeVaultSecretStore.ENCRYPTED_SECRETS_KEY)
        )

        val result = runCatching { secretStore.getSecrets(OTHER_ACCOUNT_NAME) }

        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test
    fun getSecretsReturnsNullWhenNoSecretsExist() {
        assertNull(secretStore.getSecrets(ACCOUNT_NAME))
    }

    @Test
    fun getSecretsReturnsNullWhenLegacySecretsAreIncomplete() {
        arbitraryDataProvider.storeOrUpdateKeyValue(ACCOUNT_NAME, EncryptionUtils.PRIVATE_KEY, SECRETS.privateKey)

        assertFalse(secretStore.hasSecrets(ACCOUNT_NAME))
        assertNull(secretStore.getSecrets(ACCOUNT_NAME))

        arbitraryDataProvider.deleteKeyForAccount(ACCOUNT_NAME, EncryptionUtils.PRIVATE_KEY)
        arbitraryDataProvider.storeOrUpdateKeyValue(ACCOUNT_NAME, EncryptionUtils.MNEMONIC, SECRETS.mnemonic)

        assertFalse(secretStore.hasSecrets(ACCOUNT_NAME))
        assertNull(secretStore.getSecrets(ACCOUNT_NAME))
    }

    @Test
    fun getPrivateKeyAndMnemonicReturnStoredSecrets() {
        secretStore.storeSecrets(ACCOUNT_NAME, SECRETS)

        assertEquals(SECRETS.privateKey, secretStore.getPrivateKey(ACCOUNT_NAME))
        assertEquals(SECRETS.mnemonic, secretStore.getMnemonic(ACCOUNT_NAME))
    }

    @Test
    fun deleteSecretsDeletesLegacyAndEncryptedValues() {
        arbitraryDataProvider.storeOrUpdateKeyValue(ACCOUNT_NAME, EncryptionUtils.PRIVATE_KEY, SECRETS.privateKey)
        arbitraryDataProvider.storeOrUpdateKeyValue(ACCOUNT_NAME, EncryptionUtils.MNEMONIC, SECRETS.mnemonic)
        secretStore.storeSecrets(ACCOUNT_NAME, SECRETS)

        secretStore.deleteSecrets(ACCOUNT_NAME)

        assertEquals("", arbitraryDataProvider.getValue(ACCOUNT_NAME, EncryptionUtils.PRIVATE_KEY))
        assertEquals("", arbitraryDataProvider.getValue(ACCOUNT_NAME, EncryptionUtils.MNEMONIC))
        assertEquals("", arbitraryDataProvider.getValue(ACCOUNT_NAME, E2eeVaultSecretStore.ENCRYPTED_SECRETS_KEY))
        assertTrue(cipher.deletedAccounts.contains(ACCOUNT_NAME))
    }

    @Test
    fun hasSecretsReturnsTrueForEncryptedOrLegacySecrets() {
        assertFalse(secretStore.hasSecrets(ACCOUNT_NAME))

        arbitraryDataProvider.storeOrUpdateKeyValue(ACCOUNT_NAME, EncryptionUtils.PRIVATE_KEY, SECRETS.privateKey)
        arbitraryDataProvider.storeOrUpdateKeyValue(ACCOUNT_NAME, EncryptionUtils.MNEMONIC, SECRETS.mnemonic)

        assertTrue(secretStore.hasSecrets(ACCOUNT_NAME))

        secretStore.getSecrets(ACCOUNT_NAME)

        assertTrue(secretStore.hasSecrets(ACCOUNT_NAME))
    }

    private class FakeE2eeVaultSecretCipher : E2eeVaultSecretCipher {
        val deletedAccounts = mutableListOf<String>()
        var decryptResult: ByteArray? = null
        var decryptFailure: RuntimeException? = null

        override fun encrypt(accountName: String, plaintext: ByteArray): E2eeVaultEncryptedPayload =
            E2eeVaultEncryptedPayload(
                initializationVector = "iv-$accountName",
                ciphertext = Base64.getEncoder().encodeToString(plaintext)
            )

        override fun decrypt(accountName: String, payload: E2eeVaultEncryptedPayload): ByteArray {
            decryptFailure?.let { throw it }

            if (payload.initializationVector != "iv-$accountName") {
                throw SecurityException("Payload was encrypted for a different account")
            }

            return decryptResult ?: Base64.getDecoder().decode(payload.ciphertext)
        }

        override fun deleteKey(accountName: String) {
            deletedAccounts.add(accountName)
        }
    }

    private class FakeArbitraryDataProvider : ArbitraryDataProvider {
        private val values = mutableMapOf<Pair<String, String>, String>()

        override fun deleteKeyForAccount(account: String, key: String) {
            values.remove(account to key)
        }

        override fun storeOrUpdateKeyValue(accountName: String, key: String, newValue: Long) {
            storeOrUpdateKeyValue(accountName, key, newValue.toString())
        }

        override fun incrementValue(accountName: String, key: String) = Unit

        override fun storeOrUpdateKeyValue(accountName: String, key: String, newValue: Boolean) {
            storeOrUpdateKeyValue(accountName, key, newValue.toString())
        }

        override fun storeOrUpdateKeyValue(accountName: String, key: String, newValue: String) {
            values[accountName to key] = newValue
        }

        override fun storeOrUpdateKeyValue(user: User, key: String, newValue: String) {
            storeOrUpdateKeyValue(user.accountName, key, newValue)
        }

        override fun getLongValue(accountName: String, key: String): Long = getValue(accountName, key).toLong()

        override fun getLongValue(user: User, key: String): Long = getLongValue(user.accountName, key)

        override fun getBooleanValue(accountName: String, key: String): Boolean = getValue(accountName, key).toBoolean()

        override fun getBooleanValue(user: User, key: String): Boolean = getBooleanValue(user.accountName, key)

        override fun getIntegerValue(accountName: String, key: String): Int = getValue(accountName, key).toInt()

        override fun getValue(user: User?, key: String): String = user?.let { getValue(it.accountName, key) }.orEmpty()

        override fun getValue(accountName: String, key: String): String = values[accountName to key].orEmpty()
    }

    companion object {
        private const val ACCOUNT_NAME = "user@example.org"
        private const val OTHER_ACCOUNT_NAME = "other@example.org"
        private val SECRETS = E2eeVaultSecrets(
            privateKey = "private-key",
            mnemonic = "one two three"
        )
    }
}
