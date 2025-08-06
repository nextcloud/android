/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils.crypto

import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.EncryptionUtils
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@Suppress("MagicNumber")
object CryptoHelper {

    private const val TAG = "CryptoHelper"
    private enum class Algorithm(
        val secretKeyFactoryAlgorithm: String,
        val secretKeySpecAlgorithm: String,
        val iterationCount: Int,
        val keyStrength: Int
    ) {
        SHA1("PBKDF2WithHmacSHA1", "AES", 1024, 256),
        SHA1_WITH_600000("PBKDF2WithHmacSHA1", "AES", 600000, 256),
        SHA256("PBKDF2WithHmacSHA256", "AES", 600000, 256)
    }

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 16
    private const val IV_LENGTH = 16
    private const val SALT_LENGTH = 40
    private const val GCM_TLEN = GCM_TAG_LENGTH * 8
    private val charset = StandardCharsets.UTF_8

    // region Decrypt
    @Suppress("TooGenericExceptionCaught", "LongMethod", "ThrowsCount")
    fun decryptPrivateKey(privateKey: String, keyPhrase: String): String {
        if (privateKey.isEmpty()) {
            throw CryptoError.EmptyPrivateKey()
        }

        val cleanedKeyPhrase = keyPhrase.replace(" ", "")

        // Split up cipher, iv, salt
        val strings = if (privateKey.contains(EncryptionUtils.ivDelimiter)) {
            privateKey.split(EncryptionUtils.ivDelimiter)
        } else {
            // Backward compatibility
            privateKey.split(EncryptionUtils.ivDelimiterOld)
        }

        if (strings.size != 3) {
            throw CryptoError.InvalidPrivateKeyFormat()
        }

        // This contains cipher + tag
        val encryptedDataBase64 = strings[0]
        val iv = EncryptionUtils.decodeStringToBase64Bytes(strings[1])
        val salt = EncryptionUtils.decodeStringToBase64Bytes(strings[2])

        val decryptedBytes = try {
            decrypt(Algorithm.SHA256, encryptedDataBase64, cleanedKeyPhrase.toCharArray(), salt, iv)
        } catch (sha256DecryptionError: Throwable) {
            Log_OC.w(TAG, "Failed to decrypt private key with SHA256, trying SHA1: $sha256DecryptionError")
            try {
                decrypt(Algorithm.SHA1, encryptedDataBase64, cleanedKeyPhrase.toCharArray(), salt, iv)
            } catch (sha1DecryptionError: Throwable) {
                try {
                    Log_OC.w(
                        TAG,
                        "Failed to decrypt private key with SHA1WITH600000, trying SHA1: $sha1DecryptionError"
                    )
                    decrypt(Algorithm.SHA1_WITH_600000, encryptedDataBase64, cleanedKeyPhrase.toCharArray(), salt, iv)
                } catch (sha1With6000DecryptionError: Throwable) {
                    throw CryptoError.SHA1Decryption(
                        sha1DecryptionError.message ?: sha1With6000DecryptionError.toString()
                    )
                }
            }
        }

        // Decode the Base64 encoded private key
        val encodedString = String(decryptedBytes, charset)
        val bytes = EncryptionUtils.decodeStringToBase64Bytes(encodedString)
        val decodedPrivateKey = String(bytes, charset)
        return CryptoStringUtils.rawPrivateKey(decodedPrivateKey)
    }

    private fun decrypt(
        algorithm: Algorithm,
        encryptedDataBase64: String,
        password: CharArray,
        salt: ByteArray,
        iv: ByteArray
    ): ByteArray {
        val encryptedData = EncryptionUtils.decodeStringToBase64Bytes(encryptedDataBase64)

        val secretKeyFactory = SecretKeyFactory.getInstance(algorithm.secretKeyFactoryAlgorithm)
        val keySpec = PBEKeySpec(password, salt, algorithm.iterationCount, algorithm.keyStrength)
        val secretKey = secretKeyFactory.generateSecret(keySpec)
        val secretKeySpec = SecretKeySpec(secretKey.encoded, algorithm.secretKeySpecAlgorithm)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TLEN, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, gcmSpec)

        return cipher.doFinal(encryptedData)
    }
    // endregion

    // region Encrypt
    fun encryptPrivateKey(privateKey: String, keyPhrase: String): String {
        val cleanedKeyPhrase = keyPhrase.replace(" ", "")

        val salt = generateSalt()
        val iv = generateIV()

        val privateKeyBytes = privateKey.toByteArray(charset)
        val privateKeyBase64 = EncryptionUtils.encodeBytesToBase64String(privateKeyBytes)
        val privateKeyBase64Bytes = privateKeyBase64.toByteArray(charset)

        // Encrypt the data
        val encryptedData = encrypt(
            algorithm = Algorithm.SHA256,
            data = privateKeyBase64Bytes,
            password = cleanedKeyPhrase.toCharArray(),
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
        val secretKeyFactory = SecretKeyFactory.getInstance(algorithm.secretKeyFactoryAlgorithm)
        val keySpec = PBEKeySpec(password, salt, algorithm.iterationCount, algorithm.keyStrength)
        val secretKey = secretKeyFactory.generateSecret(keySpec)
        val secretKeySpec = SecretKeySpec(secretKey.encoded, algorithm.secretKeySpecAlgorithm)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val gcmSpec = GCMParameterSpec(GCM_TLEN, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, gcmSpec)

        return cipher.doFinal(data)
    }
    // endregion

    // region Helper functions
    private fun generateIV(): ByteArray {
        val iv = ByteArray(IV_LENGTH)
        SecureRandom().nextBytes(iv)
        return iv
    }

    private fun generateSalt(): ByteArray {
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)
        return salt
    }
    // endregion
}
