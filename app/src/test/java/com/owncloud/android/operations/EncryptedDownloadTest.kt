/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations

import com.owncloud.android.lib.common.operations.OperationCancelledException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.util.function.BooleanSupplier
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Suppress("MagicNumber")
class EncryptedDownloadTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val key = SecretKeySpec(ByteArray(16) { it.toByte() }, "AES")
    private val nonce = ByteArray(12) { (it + 1).toByte() }
    private val cancelled = BooleanSupplier { false }
    private val decrypt = { cipher(Cipher.DECRYPT_MODE) }

    @Test
    fun firstDownloadCreatesMissingDirectoriesAndPublishesOnlyPlaintext() {
        val content = ByteArray(32_783) { (it % 251).toByte() }
        val encrypted = encrypted(content)
        val destination = File(temporaryFolder.root, "not/browsed/before/document.pdf")

        EncryptedDownload.save(encrypted, destination, decrypt, cancelled)

        assertArrayEquals(content, destination.readBytes())
        assertFalse(encrypted.exists())
        assertEquals(listOf(destination), destination.parentFile!!.listFiles()!!.toList())
    }

    @Test
    fun invalidTagPreservesExistingCopyAndCleansBothTemporaryFiles() {
        val destination = temporaryFolder.newFile().apply { writeText("previous valid copy") }
        val encrypted = encrypted(ByteArray(20_000)).apply {
            writeBytes(readBytes().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() })
        }

        assertThrows(AEADBadTagException::class.java) {
            EncryptedDownload.save(encrypted, destination, decrypt, cancelled)
        }

        assertEquals("previous valid copy", destination.readText())
        assertEquals(listOf(destination), temporaryFolder.root.listFiles()!!.toList())
    }

    @Test
    fun cancellationPreservesExistingCopyAndCleansDownloads() {
        val destination = temporaryFolder.newFile().apply { writeText("previous valid copy") }
        val encrypted = encrypted(ByteArray(20_000))

        assertThrows(OperationCancelledException::class.java) {
            EncryptedDownload.save(encrypted, destination, decrypt, BooleanSupplier { true })
        }

        assertEquals("previous valid copy", destination.readText())
        assertEquals(listOf(destination), temporaryFolder.root.listFiles()!!.toList())
    }

    @Test
    fun invalidEncryptionParametersCleanDownloadsWithoutPublishingAFile() {
        val encrypted = encrypted(byteArrayOf(1, 2, 3))
        val destination = File(temporaryFolder.root, "document.pdf")

        assertThrows(IllegalStateException::class.java) {
            EncryptedDownload.save(encrypted, destination, { error("Missing key") }, cancelled)
        }

        assertFalse(destination.exists())
        assertTrue(temporaryFolder.root.listFiles()!!.isEmpty())
    }

    @Test
    fun exportReceivesAuthenticatedPlaintextAndLeavesNoLocalDownloadCopy() {
        val encrypted = encrypted(byteArrayOf(1, 2, 3))
        val cache = temporaryFolder.newFolder("cache")
        val exported = temporaryFolder.newFile("exported.pdf")

        EncryptedDownload.usePlaintext(encrypted, cache, decrypt, cancelled) { plaintext ->
            assertArrayEquals(byteArrayOf(1, 2, 3), plaintext.readBytes())
            plaintext.copyTo(exported, overwrite = true)
        }

        assertArrayEquals(byteArrayOf(1, 2, 3), exported.readBytes())
        assertFalse(encrypted.exists())
        assertTrue(cache.listFiles()!!.isEmpty())
    }

    @Test
    fun failingExportStillRemovesPlaintextAndCiphertext() {
        val encrypted = encrypted(byteArrayOf(1, 2, 3))

        assertThrows(IOException::class.java) {
            EncryptedDownload.usePlaintext(encrypted, temporaryFolder.root, decrypt, cancelled) {
                throw IOException("Export failed")
            }
        }

        assertTrue(temporaryFolder.root.listFiles()!!.isEmpty())
    }

    @Test
    fun blockedDestinationPreservesUnrelatedFileAndRemovesCiphertext() {
        val blocker = temporaryFolder.newFile().apply { writeText("preserve") }
        val encrypted = encrypted(byteArrayOf(1, 2, 3))

        assertThrows(IOException::class.java) {
            EncryptedDownload.save(encrypted, File(blocker, "document.pdf"), decrypt, cancelled)
        }

        assertEquals("preserve", blocker.readText())
        assertFalse(encrypted.exists())
    }

    private fun encrypted(bytes: ByteArray): File = temporaryFolder.newFile().apply {
        writeBytes(cipher(Cipher.ENCRYPT_MODE).doFinal(bytes))
    }

    private fun cipher(mode: Int): Cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
        init(mode, key, GCMParameterSpec(128, nonce))
    }
}
