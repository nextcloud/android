/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.e2ee.vault

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import kotlin.math.max

class AndroidKeystoreE2eeVaultSecretCipher @Inject constructor(private val config: E2eeVaultSessionConfig) :
    E2eeVaultSecretCipher {
    override fun encrypt(accountName: String, plaintext: ByteArray): E2eeVaultEncryptedPayload {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey(accountName))

        return E2eeVaultEncryptedPayload(
            initializationVector = encoder.encodeToString(cipher.iv),
            ciphertext = encoder.encodeToString(cipher.doFinal(plaintext))
        )
    }

    override fun decrypt(accountName: String, payload: E2eeVaultEncryptedPayload): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(AUTHENTICATION_TAG_LENGTH_BITS, decoder.decode(payload.initializationVector))
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(accountName), spec)

        return cipher.doFinal(decoder.decode(payload.ciphertext))
    }

    override fun deleteKey(accountName: String) {
        val keyStore = loadKeyStore()
        val alias = keyAlias(accountName)

        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }
    }

    private fun getOrCreateSecretKey(accountName: String): SecretKey {
        val keyStore = loadKeyStore()
        val alias = keyAlias(accountName)
        val existingKey = keyStore.getKey(alias, null) as? SecretKey

        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(createKeySpec(alias))

        return keyGenerator.generateKey()
    }

    private fun loadKeyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    @Suppress("DEPRECATION")
    private fun createKeySpec(alias: String): KeyGenParameterSpec {
        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)

        val validitySeconds = max(
            MINIMUM_AUTHENTICATION_VALIDITY_SECONDS,
            config.unlockDurationMillis / MILLIS_PER_SECOND
        )
            .toInt()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(
                validitySeconds,
                KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL
            )
        } else {
            builder.setUserAuthenticationValidityDurationSeconds(validitySeconds)
        }

        return builder.build()
    }

    private fun keyAlias(accountName: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(accountName.toByteArray(StandardCharsets.UTF_8))
        val accountHash = encoder.encodeToString(digest)

        return "$KEY_ALIAS_PREFIX$accountHash"
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AUTHENTICATION_TAG_LENGTH_BITS = 128
        private const val KEY_ALIAS_PREFIX = "nextcloud.e2ee.vault."
        private const val MILLIS_PER_SECOND = 1_000L
        private const val MINIMUM_AUTHENTICATION_VALIDITY_SECONDS = 1L
        private const val TRANSFORMATION = "AES/GCM/NoPadding"

        private val encoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()
        private val decoder: Base64.Decoder = Base64.getUrlDecoder()
    }
}
