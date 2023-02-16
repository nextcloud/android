/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2023 Tobias Kaminsky
 * Copyright (C) 2023 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.utils

import androidx.annotation.VisibleForTesting
import com.google.gson.reflect.TypeToken
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedMetadata
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedUser
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedFolderMetadataFile
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedMetadata
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedUser
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class EncryptionUtilsV2 {
    @VisibleForTesting
    fun encryptMetadata(metadata: DecryptedMetadata): EncryptedMetadata {
        val json = EncryptionUtils.serializeJSON(metadata)
        val gzip = gZipCompress(json)
        val base64encoded = EncryptionUtils.encodeBytesToBase64String(gzip)
        val encryptedData = EncryptionUtils.encryptStringSymmetricWithIVandAuthTag(
            base64encoded,
            metadata.metadataKey.toByteArray()
        )

        return EncryptedMetadata(encryptedData.first, encryptedData.second, encryptedData.third)
    }

    @VisibleForTesting
    fun decryptMetadata(metadata: EncryptedMetadata, metadataKey: String): DecryptedMetadata {
        val decrypted = EncryptionUtils.decryptStringSymmetric(
            metadata.ciphertext,
            metadataKey.toByteArray(),
            metadata.authenticationTag
        )
        val base64decoded = EncryptionUtils.decodeStringToBase64Bytes(decrypted)
        val json = gZipDecompress(base64decoded)

        val decryptedMetadata = EncryptionUtils.deserializeJSON(json, object : TypeToken<DecryptedMetadata>() {})
        decryptedMetadata.metadataKey = metadataKey

        return decryptedMetadata
    }

    fun encryptFolderMetadataFile(metadataFile: DecryptedFolderMetadataFile): EncryptedFolderMetadataFile {
        val encryptedMetadata = encryptMetadata(metadataFile.metadata)

        val users = metadataFile.users.map {
            encryptUser(it, metadataFile.metadata.metadataKey)
        }

        return EncryptedFolderMetadataFile(
            encryptedMetadata,
            users,
            emptyMap()
        )
    }

    @Throws(IllegalStateException::class)
    fun decryptFolderMetadataFile(
        metadataFile: EncryptedFolderMetadataFile, currentUser: com.nextcloud.client.account.User,
        privateKey: String
    ): DecryptedFolderMetadataFile {
        val user = metadataFile.users.find { it.userId == currentUser.accountName }
            ?: throw IllegalStateException("User not found!")

        val decryptedMetadataKey = decryptMetadataKey(user, privateKey)

        val users = metadataFile.users.map { transformUser(it) }

        val decryptedMetadata = decryptMetadata(metadataFile.metadata, decryptedMetadataKey)

        return DecryptedFolderMetadataFile(
            decryptedMetadata,
            users,
            emptyMap() // TODO
        )
    }

    @VisibleForTesting
    fun encryptUser(user: DecryptedUser, metadataKey: String): EncryptedUser {
        val encryptedKey = EncryptionUtils.encryptStringAsymmetric(
            metadataKey,
            user.certificate
        )

        return EncryptedUser(
            user.userId,
            user.certificate,
            encryptedKey
        )
    }

    @VisibleForTesting
    fun transformUser(user: EncryptedUser): DecryptedUser {
        return DecryptedUser(
            user.userId,
            user.certificate,
        )
    }

    @VisibleForTesting
    fun decryptMetadataKey(user: EncryptedUser, privateKey: String): String {
        return EncryptionUtils.decryptStringAsymmetric(
            user.encryptedKey,
            privateKey
        )
    }

    @VisibleForTesting
    fun gZipCompress(string: String): ByteArray {
        val outputStream = ByteArrayOutputStream()
        GZIPOutputStream(outputStream).apply {
            write(string.toByteArray())
            flush()
            close()
        }

        return outputStream.toByteArray()
    }

    @VisibleForTesting
    fun gZipDecompress(compressed: ByteArray): String {
        val stringBuilder = StringBuilder()
        val inputStream = GZIPInputStream(compressed.inputStream())
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))

        var line = bufferedReader.readLine()
        while (line != null) {
            stringBuilder.appendLine(line)
            line = bufferedReader.readLine()
        }

        return stringBuilder.toString()
    }
}
