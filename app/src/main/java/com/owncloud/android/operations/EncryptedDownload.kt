/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations

import com.owncloud.android.lib.common.operations.OperationCancelledException
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.upload.EncryptedFileContent
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.function.BooleanSupplier
import javax.crypto.Cipher

object EncryptedDownload {
    fun save(encrypted: File, destination: File, cipher: () -> Cipher, cancelled: BooleanSupplier) {
        usePlaintext(encrypted, requireNotNull(destination.absoluteFile.parentFile), cipher, cancelled) { plaintext ->
            Files.move(plaintext.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    fun <T> usePlaintext(
        encrypted: File,
        directory: File,
        cipher: () -> Cipher,
        cancelled: BooleanSupplier,
        consume: (File) -> T
    ): T {
        var plaintext: File? = null
        try {
            Files.createDirectories(directory.toPath())
            plaintext = File.createTempFile("decrypted-download-", null, directory)
            EncryptedFileContent.decrypt(encrypted, plaintext, cipher(), cancelled)
            if (cancelled.asBoolean) throw OperationCancelledException()
            return consume(plaintext)
        } finally {
            plaintext?.let { discard(it) }
            discard(encrypted)
        }
    }

    private fun discard(file: File) {
        if (file.exists() && !file.delete()) {
            Log_OC.w(EncryptedDownload::class.java.simpleName, "Unable to remove temporary download")
        }
    }
}
