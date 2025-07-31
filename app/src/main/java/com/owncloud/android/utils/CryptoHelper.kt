/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils
import com.owncloud.android.lib.common.utils.Log_OC
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object CryptoHelper {

    private const val TAG = "CryptoHelper"
    private enum class Algorithm(
        val secretKeyFactoryAlgorithm: String,
        val secretKeySpecAlgorithm: String,
        val iterationCount: Int,
        val keyStrength: Int
    ) {
        SHA1("PBKDF2WithHmacSHA1", "AES", 1024, 256),
        SHA256("PBKDF2WithHmacSHA256", "AES", 600000, 256),
    }

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 16
    private const val IV_LENGTH = 16
    private const val SALT_LENGTH = 40

    // region Decrypt
    fun decryptPrivateKey(privateKey: String, keyPhrase: String): String {
        val cleanedKeyPhrase = keyPhrase.replace(" ", "")

        // Split up cipher, iv, salt
        val strings = if (privateKey.contains(EncryptionUtils.ivDelimiter)) {
            privateKey.split(EncryptionUtils.ivDelimiter)
        } else {
            // Backward compatibility
            privateKey.split(EncryptionUtils.ivDelimiterOld)
        }

        if (strings.size != 3) {
            throw IllegalArgumentException("Invalid encrypted private key format: expected 3 parts")
        }

        // This contains cipher + tag
        val encryptedDataBase64 = strings[0]
        val iv = EncryptionUtils.decodeStringToBase64Bytes(strings[1])
        val salt = EncryptionUtils.decodeStringToBase64Bytes(strings[2])

        val decryptedBytes = try {
            decrypt(Algorithm.SHA256, encryptedDataBase64, cleanedKeyPhrase.toCharArray(), salt, iv)
        } catch (t: Throwable) {
            Log_OC.w(TAG, "Failed to decrypt private key with SHA256, trying SHA1: $t")
            try {
                decrypt(Algorithm.SHA1, encryptedDataBase64, cleanedKeyPhrase.toCharArray(), salt, iv)
            } catch (t2: Throwable) {
                Log_OC.e(TAG, "Failed to decrypt private key with SHA1: $t2")
                throw t2
            }
        }

        // Decode the Base64 encoded private key
        val decodedPrivateKey = String(
            EncryptionUtils.decodeStringToBase64Bytes(String(decryptedBytes, StandardCharsets.UTF_8)),
            StandardCharsets.UTF_8
        )

        return decodedPrivateKey
    }

    private fun decrypt(
        algorithm: Algorithm,
        encryptedDataBase64: String,
        password: CharArray,
        salt: ByteArray,
        iv: ByteArray
    ): ByteArray {
        // Decode the full encrypted data (cipher + authentication tag)
        val fullEncryptedData = EncryptionUtils.decodeStringToBase64Bytes(encryptedDataBase64)

        if (fullEncryptedData.size < GCM_TAG_LENGTH) {
            throw IllegalArgumentException("Encrypted data too short")
        }

        // Split cipher and authentication tag
        val cipherData = fullEncryptedData.copyOfRange(0, fullEncryptedData.size - GCM_TAG_LENGTH)
        val authTag = fullEncryptedData.copyOfRange(fullEncryptedData.size - GCM_TAG_LENGTH, fullEncryptedData.size)

        // Derive AES key using PBKDF2
        val secretKeyFactory = SecretKeyFactory.getInstance(algorithm.secretKeyFactoryAlgorithm)
        val keySpec = PBEKeySpec(password, salt, algorithm.iterationCount, algorithm.keyStrength)
        val secretKey = secretKeyFactory.generateSecret(keySpec)
        val secretKeySpec = SecretKeySpec(secretKey.encoded, algorithm.secretKeySpecAlgorithm)

        // Set up AES-GCM decryption
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv) // Tag length in bits
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmSpec)

        // Combine cipher data and authentication tag for GCM
        val dataWithTag = ByteArray(cipherData.size + authTag.size)
        System.arraycopy(cipherData, 0, dataWithTag, 0, cipherData.size)
        System.arraycopy(authTag, 0, dataWithTag, cipherData.size, authTag.size)

        return cipher.doFinal(dataWithTag)
    }
    // endregion

    // region Encrypt
    fun encryptPrivateKey(privateKey: String, keyPhrase: String): String {
        // Clean passphrase from spaces
        val cleanedKeyPhrase = keyPhrase.replace(" ", "")

        // Generate salt and IV
        val salt = generateSalt(SALT_LENGTH)
        val iv = generateIV(IV_LENGTH)

        val privateKeyBytes = privateKey.toByteArray(StandardCharsets.UTF_8)
        val privateKeyBase64 = EncryptionUtils.encodeBytesToBase64String(privateKeyBytes)
        val privateKeyBase64Bytes = privateKeyBase64.toByteArray(StandardCharsets.UTF_8)

        // Encrypt the data
        val encryptedData = encrypt(
            Algorithm.SHA256,
            privateKeyBase64Bytes,
            cleanedKeyPhrase.toCharArray(),
            salt,
            iv
        )

        // Format: base64(cipher+tag)|base64(iv)|base64(salt)
        val cipherBase64 = EncryptionUtils.encodeBytesToBase64String(encryptedData)
        val ivBase64 = EncryptionUtils.encodeBytesToBase64String(iv)
        val saltBase64 = EncryptionUtils.encodeBytesToBase64String(salt)

        return "$cipherBase64${EncryptionUtils.ivDelimiter}$ivBase64${EncryptionUtils.ivDelimiter}$saltBase64"
    }

    private fun encrypt(
        algorithm: Algorithm,
        data: ByteArray,
        password: CharArray,
        salt: ByteArray,
        iv: ByteArray
    ): ByteArray {
        // Derive AES key using PBKDF2
        val secretKeyFactory = SecretKeyFactory.getInstance(algorithm.secretKeyFactoryAlgorithm)
        val keySpec = PBEKeySpec(password, salt, algorithm.iterationCount, algorithm.keyStrength)
        val secretKey = secretKeyFactory.generateSecret(keySpec)
        val secretKeySpec = SecretKeySpec(secretKey.encoded, algorithm.secretKeySpecAlgorithm)

        // Set up AES-GCM encryption
        val cipher = Cipher.getInstance(TRANSFORMATION)

        // Tag length in bits
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH * 8, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmSpec)

        // Encrypt and return data with appended authentication tag
        return cipher.doFinal(data)
    }
    // endregion

    // region Helper functions
    private fun generateIV(length: Int): ByteArray {
        val iv = ByteArray(length)
        SecureRandom().nextBytes(iv)
        return iv
    }

    private fun generateSalt(length: Int): ByteArray {
        val salt = ByteArray(length)
        SecureRandom().nextBytes(salt)
        return salt
    }

    private fun generateKey(length: Int): ByteArray {
        val key = ByteArray(length)
        SecureRandom().nextBytes(key)
        return key
    }
    // endregion
}
