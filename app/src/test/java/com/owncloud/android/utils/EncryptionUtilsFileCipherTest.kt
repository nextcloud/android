/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.utils

import com.nextcloud.client.account.MockUser
import com.owncloud.android.datamodel.ArbitraryDataProvider
import java.io.File
import java.util.Base64
import javax.crypto.Cipher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class EncryptionUtilsFileCipherTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val arbitraryDataProvider: ArbitraryDataProvider = mock()
    private val user = MockUser(ACCOUNT_NAME, ACCOUNT_TYPE)

    @Test
    fun decryptFileToBytesReturnsPlaintextWhenAuthenticationTagMatchesCiphertextTag() {
        val encryptedFile = encryptedFile(PLAINTEXT)
        val authenticationTag = authenticationTag(encryptedFile.readBytes())
        val cipher = decryptCipher()

        val plaintext = EncryptionUtils.decryptFileToBytes(
            cipher,
            encryptedFile,
            authenticationTag,
            arbitraryDataProvider,
            user
        )

        assertArrayEquals(PLAINTEXT, plaintext)
    }

    @Test
    fun getAuthenticationTagReturnsCiphertextTag() {
        val encryptedFile = encryptedFile(PLAINTEXT)

        val authenticationTag = EncryptionUtils.getAuthenticationTag(encryptedFile)

        assertEquals(authenticationTag(encryptedFile.readBytes()), authenticationTag)
    }

    @Test
    fun decryptFileToBytesRejectsMismatchingAuthenticationTag() {
        val encryptedFile = encryptedFile(PLAINTEXT)
        val wrongAuthenticationTag = wrongAuthenticationTag(encryptedFile.readBytes())
        val cipher = decryptCipher()

        assertThrows(SecurityException::class.java) {
            EncryptionUtils.decryptFileToBytes(
                cipher,
                encryptedFile,
                wrongAuthenticationTag,
                arbitraryDataProvider,
                user
            )
        }
        verify(arbitraryDataProvider).incrementValue(ACCOUNT_NAME, ArbitraryDataProvider.E2E_ERRORS)
    }

    private fun encryptedFile(plaintext: ByteArray): File {
        val encryptedBytes = encryptCipher().doFinal(plaintext)
        return temporaryFolder.newFile("encrypted.bin").apply {
            writeBytes(encryptedBytes)
        }
    }

    private fun encryptCipher() = EncryptionUtils.getCipher(Cipher.ENCRYPT_MODE, KEY, IV)

    private fun decryptCipher() = EncryptionUtils.getCipher(Cipher.DECRYPT_MODE, KEY, IV)

    private fun authenticationTag(encryptedBytes: ByteArray): String = Base64.getEncoder().encodeToString(
        encryptedBytes.copyOfRange(encryptedBytes.size - TAG_BYTES, encryptedBytes.size)
    )

    private fun wrongAuthenticationTag(encryptedBytes: ByteArray): String {
        val tag = encryptedBytes.copyOfRange(encryptedBytes.size - TAG_BYTES, encryptedBytes.size)
        tag[0] = tag[0].inc()
        return Base64.getEncoder().encodeToString(tag)
    }

    companion object {
        private const val ACCOUNT_NAME = "user@example.org"
        private const val ACCOUNT_TYPE = "nextcloud"
        private const val TAG_BYTES = 16
        private val IV = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        private val KEY = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
        private val PLAINTEXT = "vault media plaintext".toByteArray()
    }
}
