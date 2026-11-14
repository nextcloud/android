/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.upload

import com.owncloud.android.lib.common.operations.OperationCancelledException
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.util.function.BooleanSupplier
import javax.crypto.Cipher

object EncryptedFileContent {
    fun decryptAndCompare(
        encrypted: File,
        plaintext: File,
        local: File,
        cipher: Cipher,
        cancelled: BooleanSupplier
    ): Boolean {
        try {
            decrypt(encrypted, plaintext, cipher, cancelled)
            return sameContent(local, plaintext, cancelled)
        } catch (e: Exception) {
            runCatching { Files.deleteIfExists(plaintext.toPath()) }.onFailure { e.addSuppressed(it) }
            throw e
        }
    }

    fun decrypt(encrypted: File, plaintext: File, cipher: Cipher, cancelled: BooleanSupplier) {
        try {
            decryptContent(encrypted, plaintext, cipher, cancelled)
        } catch (e: Exception) {
            runCatching { Files.deleteIfExists(plaintext.toPath()) }.onFailure { e.addSuppressed(it) }
            throw e
        }
    }

    private fun decryptContent(encrypted: File, plaintext: File, cipher: Cipher, cancelled: BooleanSupplier) {
        encrypted.inputStream().use { input ->
            plaintext.outputStream().use { output ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var count = input.read(buffer)
                while (count != -1) {
                    checkCancelled(cancelled)
                    output.write(cipher.update(buffer, 0, count) ?: byteArrayOf())
                    count = input.read(buffer)
                }
                checkCancelled(cancelled)
                output.write(cipher.doFinal())
            }
        }
    }

    private fun sameContent(left: File, right: File, cancelled: BooleanSupplier): Boolean {
        if (left.length() != right.length()) return false
        left.inputStream().buffered().use { first ->
            right.inputStream().buffered().use { second ->
                return sameContent(first, second, cancelled)
            }
        }
    }

    private fun sameContent(first: InputStream, second: InputStream, cancelled: BooleanSupplier): Boolean {
        while (true) {
            checkCancelled(cancelled)
            val value = first.read()
            if (value != second.read()) return false
            if (value == -1) return true
        }
    }

    private fun checkCancelled(cancelled: BooleanSupplier) {
        if (cancelled.asBoolean) throw OperationCancelledException()
    }
}
