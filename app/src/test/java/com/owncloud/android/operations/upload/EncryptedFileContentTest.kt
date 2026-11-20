/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.upload

import com.owncloud.android.lib.common.operations.OperationCancelledException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.function.BooleanSupplier
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Suppress("MagicNumber")
class EncryptedFileContentTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val key = SecretKeySpec(ByteArray(16) { it.toByte() }, "AES")
    private val nonce = ByteArray(12) { (it + 1).toByte() }

    @Test
    fun identicalPlaintextMatchesDespiteCiphertextSizeAndDifferentTimestamp() {
        val content = ByteArray(32_783) { (it % 251).toByte() }
        val encrypted = encrypted(content)
        val local = temporaryFolder.newFile().apply {
            writeBytes(content)
            setLastModified(1_000)
        }
        val plaintext = temporaryFolder.newFile()

        assertTrue(compare(encrypted, plaintext, local))
        assertArrayEquals(content, plaintext.readBytes())
        assertTrue(encrypted.length() != local.length())
    }

    @Test
    fun differentContentOfSameSizeIsAConflict() {
        val original = ByteArray(16_400) { 7 }
        val changed = original.copyOf().apply { this[lastIndex] = 8 }
        val local = temporaryFolder.newFile().apply { writeBytes(changed) }
        val plaintext = temporaryFolder.newFile()

        assertFalse(compare(encrypted(original), plaintext, local))
        assertArrayEquals(changed, local.readBytes())
        assertArrayEquals(original, plaintext.readBytes())
    }

    @Test
    fun emptyPlaintextMatches() {
        assertTrue(compare(encrypted(byteArrayOf()), temporaryFolder.newFile(), temporaryFolder.newFile()))
    }

    @Test
    fun differentPlaintextLengthIsAConflict() {
        val local = temporaryFolder.newFile().apply { writeBytes(byteArrayOf(1)) }
        assertFalse(compare(encrypted(byteArrayOf(1, 2)), temporaryFolder.newFile(), local))
    }

    @Test
    fun corruptedAuthenticationTagCannotProduceAnEqualityResultOrPreview() {
        val local = temporaryFolder.newFile().apply { writeBytes(ByteArray(9_000)) }
        val encrypted = encrypted(local.readBytes()).apply {
            writeBytes(readBytes().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() })
        }
        val plaintext = temporaryFolder.newFile()

        assertThrows(AEADBadTagException::class.java) { compare(encrypted, plaintext, local) }
        assertFalse(plaintext.exists())
        assertTrue(local.exists())
    }

    @Test
    fun truncatedCiphertextIsRejectedAndPlaintextRemoved() {
        val encrypted = encrypted(byteArrayOf(1, 2, 3)).apply { writeBytes(readBytes().dropLast(1).toByteArray()) }
        val plaintext = temporaryFolder.newFile()

        assertThrows(AEADBadTagException::class.java) { compare(encrypted, plaintext, temporaryFolder.newFile()) }
        assertFalse(plaintext.exists())
    }

    @Test
    fun cancellationRemovesTemporaryPlaintextAndPreservesLocalFile() {
        val local = temporaryFolder.newFile().apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val plaintext = temporaryFolder.newFile()

        assertThrows(OperationCancelledException::class.java) {
            EncryptedFileContent.decryptAndCompare(
                encrypted(local.readBytes()),
                plaintext,
                local,
                cipher(Cipher.DECRYPT_MODE),
                BooleanSupplier { true }
            )
        }
        assertFalse(plaintext.exists())
        assertArrayEquals(byteArrayOf(1, 2, 3), local.readBytes())
    }

    private fun encrypted(bytes: ByteArray): File = temporaryFolder.newFile().apply {
        writeBytes(cipher(Cipher.ENCRYPT_MODE).doFinal(bytes))
    }

    private fun compare(encrypted: File, plaintext: File, local: File): Boolean =
        EncryptedFileContent.decryptAndCompare(
            encrypted,
            plaintext,
            local,
            cipher(Cipher.DECRYPT_MODE),
            BooleanSupplier { false }
        )

    private fun cipher(mode: Int): Cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
        init(mode, key, GCMParameterSpec(128, nonce))
    }
}
