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

import android.accounts.AccountManager
import android.content.Context
import androidx.annotation.VisibleForTesting
import com.google.gson.reflect.TypeToken
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedMetadata
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedUser
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedFolderMetadataFile
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedMetadata
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedUser
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.resources.e2ee.GetMetadataRemoteOperation
import com.owncloud.android.lib.resources.e2ee.StoreMetadataRemoteOperation
import com.owncloud.android.lib.resources.e2ee.UpdateMetadataRemoteOperation
import com.owncloud.android.operations.UploadException
import org.apache.commons.httpclient.HttpStatus
import org.bouncycastle.cert.jcajce.JcaCertStore
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.cms.CMSTypedData
import org.bouncycastle.cms.SignerInformation
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.ContentSigner
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.math.BigInteger
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Security
import java.security.cert.X509Certificate
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class EncryptionUtilsV2 {
    @VisibleForTesting
    fun encryptMetadata(metadata: DecryptedMetadata, metadataKey: String): EncryptedMetadata {
        val json = EncryptionUtils.serializeJSON(metadata)
        val gzip = gZipCompress(json)
        val encryptedData = EncryptionUtils.encryptStringSymmetricWithIVandAuthTag(
            gzip,
            metadataKey.toByteArray()
        )

        return EncryptedMetadata(encryptedData.first, encryptedData.second, encryptedData.third)
    }

    @VisibleForTesting
    fun decryptMetadata(metadata: EncryptedMetadata, metadataKey: String): DecryptedMetadata {
        val decrypted = EncryptionUtils.decryptStringSymmetric(
            metadata.ciphertext,
            EncryptionUtils.decodeStringToBase64Bytes(metadataKey),
            metadata.authenticationTag,
            metadata.nonce
        )
        val json = gZipDecompress(decrypted)

        val decryptedMetadata = EncryptionUtils.deserializeJSON(json, object : TypeToken<DecryptedMetadata>() {})
        decryptedMetadata.metadataKey = metadataKey

        return decryptedMetadata
    }

    fun encryptFolderMetadataFile(
        metadataFile: DecryptedFolderMetadataFile,
        ocFile: OCFile,
        storageManager: FileDataStorageManager,
        client: OwnCloudClient,
        userId: String,
        privateKey: String
    ): EncryptedFolderMetadataFile {
        val encryptedUsers: List<EncryptedUser>
        val encryptedMetadata: EncryptedMetadata
        if (metadataFile.users.isEmpty()) {
            // we are in a subfolder, re-use users array
            val key = retrieveTopMostMetadataKey(
                ocFile,
                storageManager,
                client,
                userId,
                privateKey
            )

            // do not store metadata key
            metadataFile.metadata.metadataKey = ""

            encryptedUsers = emptyList()
            encryptedMetadata = encryptMetadata(metadataFile.metadata, key)
        } else {
            encryptedUsers = metadataFile.users.map {
                encryptUser(it, metadataFile.metadata.metadataKey)
            }
            encryptedMetadata = encryptMetadata(metadataFile.metadata, metadataFile.metadata.metadataKey)
        }

        return EncryptedFolderMetadataFile(
            encryptedMetadata,
            encryptedUsers,
            emptyMap()
        )

        // if (metadataFile.users.isEmpty()) {
        //     // we are in a subfolder, re-use users array
        //     retrieveTopMostMetadata(
        //         ocFile,
        //         storageManager,
        //         client
        //     )
        // } else {
        //    val encryptedUsers = metadataFile.users.map {
        //         encryptUser(it, metadataFile.metadata.metadataKey)
        //     }
        //
        //     return EncryptedFolderMetadataFile(
        //         encryptedMetadata,
        //         encryptedUsers,
        //         emptyMap()
        //     )
        // }
    }

    @Throws(IllegalStateException::class)
    fun decryptFolderMetadataFile(
        metadataFile: EncryptedFolderMetadataFile,
        userId: String,
        privateKey: String,
        ocFile: OCFile,
        storageManager: FileDataStorageManager,
        client: OwnCloudClient
    ): DecryptedFolderMetadataFile {
        var encryptedUser = metadataFile.users.find { it.userId == userId }

        if (encryptedUser == null) {
            // we are in a subfolder, decrypt information is in top most encrypted folder
            val metadataKey = retrieveTopMostMetadataKey(
                ocFile,
                storageManager,
                client,
                userId,
                privateKey
            )

            val decryptedMetadata = decryptMetadata(metadataFile.metadata, metadataKey)
            decryptedMetadata.metadataKey = metadataKey

            return DecryptedFolderMetadataFile(
                decryptedMetadata,
                mutableListOf(),
                emptyMap() // TODO
            )
        } else {
            val decryptedMetadataKey = decryptMetadataKey(encryptedUser, privateKey)

            val users = metadataFile.users.map { transformUser(it) }.toMutableList()

            val decryptedMetadata = decryptMetadata(metadataFile.metadata, decryptedMetadataKey)

            return DecryptedFolderMetadataFile(
                decryptedMetadata,
                users,
                emptyMap() // TODO
            )
        }
    }

    @Throws(IllegalStateException::class)
    fun retrieveTopMostMetadataKey(
        folder: OCFile,
        storageManager: FileDataStorageManager,
        client: OwnCloudClient,
        userId: String,
        privateKey: String
    ): String {
        var topMost = folder
        var parent =
            storageManager.getFileById(topMost.parentId) ?: throw IllegalStateException("Cannot retrieve metadata")

        while (parent.isEncrypted) {
            topMost = parent

            parent =
                storageManager.getFileById(topMost.parentId) ?: throw IllegalStateException("Cannot retrieve metadata")
        }

        // parent is now top most encrypted folder
        val result = GetMetadataRemoteOperation(topMost.localId).execute(client)

        if (result.isSuccess) {
            val v2 = EncryptionUtils.deserializeJSON(
                result.resultData, object : TypeToken<EncryptedFolderMetadataFile>() {}
            )

            return decryptFolderMetadataFile(
                v2,
                userId,
                privateKey,
                topMost,
                storageManager,
                client
            ).metadata.metadataKey
        } else {
            throw IllegalStateException("Cannot retrieve metadata")
        }
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
            user.encryptedMetadataKey,
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
        //val inputStream = compressed.inputStream()
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))

        val sb = java.lang.StringBuilder()
        for (b in compressed) {
            sb.append(String.format("%02X ", b))
        }
        val out = sb.toString()

        var line = bufferedReader.readLine()
        while (line != null) {
            stringBuilder.appendLine(line)
            line = bufferedReader.readLine()
        }

        return stringBuilder.toString()
    }

    fun addShareeToMetadata(
        metadataFile: DecryptedFolderMetadataFile,
        userId: String,
        cert: String
    ): DecryptedFolderMetadataFile {
        metadataFile.users.add(DecryptedUser(userId, cert))
        metadataFile.metadata.metadataKey = EncryptionUtils.generateKeyString()

        return metadataFile
    }

    @Throws(RuntimeException::class)
    fun removeShareeFromMetadata(
        metadataFile: DecryptedFolderMetadataFile,
        userIdToRemove: String
    ): DecryptedFolderMetadataFile {
        val remove = metadataFile.users.remove(metadataFile.users.find { it.userId == userIdToRemove })

        if (!remove) {
            throw java.lang.RuntimeException("Removal of user $userIdToRemove failed!")
        }

        metadataFile.metadata.metadataKey = EncryptionUtils.generateKeyString()
        // TODO add to keyChecksum array

        return metadataFile
    }

    fun addFileToMetadata(
        encryptedFileName: String,
        ocFile: OCFile,
        initializationVector: ByteArray,
        authenticationTag: String,
        key: ByteArray,
        metadataFile: DecryptedFolderMetadataFile
    ): DecryptedFolderMetadataFile {
        val decryptedFile = DecryptedFile(
            ocFile.decryptedFileName,
            ocFile.mimeType,
            EncryptionUtils.encodeBytesToBase64String(initializationVector),
            authenticationTag,
            EncryptionUtils.encodeBytesToBase64String(key)
        )

        metadataFile.metadata.files[encryptedFileName] = decryptedFile
        metadataFile.metadata.counter++

        return metadataFile
    }

    fun addFolderToMetadata(
        encryptedFileName: String,
        fileName: String,
        metadataFile: DecryptedFolderMetadataFile
    ): DecryptedFolderMetadataFile {
        metadataFile.metadata.folders[encryptedFileName] = fileName

        return metadataFile
    }

    fun removeFolderFromMetadata(
        encryptedFileName: String,
        metadataFile: DecryptedFolderMetadataFile
    ): DecryptedFolderMetadataFile {
        metadataFile.metadata.folders.remove(encryptedFileName)

        return metadataFile
    }

    @Throws(IllegalStateException::class)
    fun removeFileFromMetadata(
        fileName: String,
        metadata: DecryptedFolderMetadataFile
    ) {
        metadata.metadata.files.remove(fileName)
            ?: throw IllegalStateException("File $fileName not found in metadata!")
    }

    @Throws(IllegalStateException::class)
    fun renameFile(
        key: String,
        newName: String,
        metadataFile: DecryptedFolderMetadataFile
    ) {
        if (!metadataFile.metadata.files.containsKey(key)) {
            throw IllegalStateException("File with key $key not found in metadata!")
        }

        metadataFile.metadata.files[key]!!.filename = newName
    }

    @Throws(UploadException::class, IllegalStateException::class)
    fun retrieveMetadata(
        folder: OCFile,
        client: OwnCloudClient,
        user: User,
        context: Context
    ): Pair<Boolean, DecryptedFolderMetadataFile> {
        val getMetadataOperationResult = GetMetadataRemoteOperation(folder.localId).execute(client)

        return if (getMetadataOperationResult.isSuccess) {
            // decrypt metadata
            val serializedEncryptedMetadata = getMetadataOperationResult.resultData
            val metadata = parseAnyMetadata(
                serializedEncryptedMetadata,
                user,
                client,
                context,
                folder
            )

            Pair(true, metadata)
        } else if (getMetadataOperationResult.httpCode == HttpStatus.SC_NOT_FOUND) {
            // check parent folder
            val parentFolder = FileDataStorageManager(user, context.contentResolver).getFileById(folder.parentId)
                ?: throw IllegalStateException("Cannot retrieve metadata!")

            val metadata = if (parentFolder.isEncrypted) {
                // new metadata but without sharing part
                createDecryptedFolderMetadataFile()
            } else {
                // new metadata
                val arbitraryDataProvider: ArbitraryDataProvider = ArbitraryDataProviderImpl(context)
                val publicKey: String = arbitraryDataProvider.getValue(user.accountName, EncryptionUtils.PUBLIC_KEY)

                createDecryptedFolderMetadataFile().apply {
                    users = mutableListOf(DecryptedUser(client.userId, publicKey))
                }
            }

            Pair(false, metadata)
        } else {
            // TODO error
            throw UploadException("something wrong")
        }
    }

    @Throws(IllegalStateException::class)
    fun parseAnyMetadata(
        serializedEncryptedMetadata: String,
        user: User,
        client: OwnCloudClient,
        context: Context,
        folder: OCFile
    ): DecryptedFolderMetadataFile {
        val arbitraryDataProvider: ArbitraryDataProvider = ArbitraryDataProviderImpl(context)
        val privateKey: String = arbitraryDataProvider.getValue(user.accountName, EncryptionUtils.PRIVATE_KEY)
        val storageManager = FileDataStorageManager(user, context.contentResolver)

        val v2 = EncryptionUtils.deserializeJSON(
            serializedEncryptedMetadata, object : TypeToken<EncryptedFolderMetadataFile>() {}
        )

        val decryptedFolderMetadata = if (v2.version == 2) {
            val userId = AccountManager.get(context).getUserData(
                user.toPlatformAccount(),
                AccountUtils.Constants.KEY_USER_ID
            )
            decryptFolderMetadataFile(
                v2,
                userId,
                privateKey,
                folder,
                storageManager,
                client
            )
        } else {
            // try to deserialize v1
            val v1 = EncryptionUtils.deserializeJSON(
                serializedEncryptedMetadata,
                object : TypeToken<com.owncloud.android.datamodel.e2e.v1.encrypted.EncryptedFolderMetadataFile?>() {})

            // decrypt
            try {
                // decrypt metadata
                val decryptedV1 = EncryptionUtils.decryptFolderMetaData(v1, privateKey)
                val publicKey: String = arbitraryDataProvider.getValue(
                    user.accountName,
                    EncryptionUtils.PUBLIC_KEY
                )

                // migrate to v2
                migrateV1ToV2(
                    decryptedV1,
                    client.userIdPlain,
                    publicKey
                )
            } catch (e: Exception) {
                // TODO do better
                throw IllegalStateException("Cannot decrypt metadata")
            }
        }

        // verify metadata
        // if (!verifyMetadata(decryptedFolderMetadata)) {
        //     throw IllegalStateException("Metadata is corrupt!")
        // }

        return decryptedFolderMetadata

        // handle filesDrops
        // TODO re-add
//        try {
//            int filesDropCountBefore = encryptedFolderMetadata.getFiledrop().size();
//            DecryptedFolderMetadataFile decryptedFolderMetadata = new EncryptionUtilsV2().decryptFolderMetadataFile(
//                encryptedFolderMetadata,
//                privateKey);
//
//            boolean transferredFiledrop = filesDropCountBefore > 0 && decryptedFolderMetadata.getFiles().size() ==
//                encryptedFolderMetadata.getFiles().size() + filesDropCountBefore;
//
//            if (transferredFiledrop) {
//                // lock folder, only if not already locked
//                String token;
//                if (existingLockToken == null) {
//                    token = EncryptionUtils.lockFolder(folder, client);
//                } else {
//                    token = existingLockToken;
//                }
//
//                // upload metadata
//                EncryptedFolderMetadataFile encryptedFolderMetadataNew = encryptFolderMetadata(decryptedFolderMetadata,
//                                                                                               privateKey);
//
//                String serializedFolderMetadata = EncryptionUtils.serializeJSON(encryptedFolderMetadataNew);
//
//                EncryptionUtils.uploadMetadata(folder,
//                                               serializedFolderMetadata,
//                                               token,
//                                               client,
//                                               true);
//
//                // unlock folder, only if not previously locked
//                if (existingLockToken == null) {
//                    RemoteOperationResult unlockFolderResult = EncryptionUtils.unlockFolder(folder, client, token);
//
//                    if (!unlockFolderResult.isSuccess()) {
//                        Log_OC.e(TAG, unlockFolderResult.getMessage());
//
//                        return null;
//                    }
//                }
//            }
//
//            return decryptedFolderMetadata;
//        } catch (Exception e) {
//            Log_OC.e(TAG, e.getMessage());
//            return null;
//        }
    }

    @Throws(IllegalStateException::class)
    fun migrateV1ToV2(
        v1: com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFile,
        userId: String,
        cert: String
    ): DecryptedFolderMetadataFile {

        // create new metadata
        val metadataV2 = DecryptedMetadata(
            mutableListOf(),
            false,
            0,
            mutableMapOf(),
            v1.files.mapValues { migrateDecryptedFileV1ToV2(it.value) }.toMutableMap(),
            v1.metadata.metadataKeys[0] ?: throw IllegalStateException("Metadata key not found!")
        )

        // upon migration there can only be one user, as there is no sharing yet in place
        val users = mutableListOf(DecryptedUser(userId, cert))

        // TODO
        val filedrop = emptyMap<String, DecryptedFile>()

        return DecryptedFolderMetadataFile(metadataV2, users, filedrop)
    }

    @VisibleForTesting
    fun migrateDecryptedFileV1ToV2(v1: com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFile): DecryptedFile {
        return DecryptedFile(
            v1.encrypted.filename,
            v1.encrypted.mimetype,
            v1.initializationVector,
            v1.authenticationTag ?: "",
            v1.encrypted.key
        )
    }

    @Throws(UploadException::class)
    fun serializeAndUploadMetadata(
        parentFile: OCFile,
        metadata: DecryptedFolderMetadataFile,
        token: String,
        client: OwnCloudClient,
        storageManager: FileDataStorageManager,
        metadataExists: Boolean,
        context: Context,
        user: User
    ) {
        val arbitraryDataProvider: ArbitraryDataProvider = ArbitraryDataProviderImpl(context)
        val privateKey: String = arbitraryDataProvider.getValue(user.accountName, EncryptionUtils.PRIVATE_KEY)

        val encryptedFolderMetadata = encryptFolderMetadataFile(
            metadata,
            parentFile,
            storageManager,
            client,
            client.userId,
            privateKey
        )
        val serializedFolderMetadata = EncryptionUtils.serializeJSON(encryptedFolderMetadata)
        val uploadMetadataOperationResult = if (metadataExists) {
            // update metadata
            UpdateMetadataRemoteOperation(
                parentFile.localId,
                serializedFolderMetadata,
                token
            )
                .execute(client)
        } else {
            // store metadata
            StoreMetadataRemoteOperation(
                parentFile.localId,
                serializedFolderMetadata
            )
                .execute(client)
        }
        if (!uploadMetadataOperationResult.isSuccess) {
            throw UploadException("Storing/updating metadata was not successful")
        }
    }

    @Throws(IllegalStateException::class)
    @VisibleForTesting
    fun verifyMetadata(metadata: DecryptedFolderMetadataFile): Boolean {
        // check counter
        //metadata.metadata.counter

        // check signature

        // check hash of keys
        val hashedMetadataKey = hashMetadataKey(metadata.metadata.metadataKey)
        if (!metadata.metadata.keyChecksums.contains(hashedMetadataKey)) {
            throw IllegalStateException("Hash not found")
        }

        // check certs

        return true
    }

    fun createDecryptedFolderMetadataFile(): DecryptedFolderMetadataFile {
        val metadata = DecryptedMetadata().apply {
            keyChecksums.add(hashMetadataKey(metadataKey))
        }

        return DecryptedFolderMetadataFile(metadata)
    }

    /**
     * SHA-256 hash of metadata-key
     */
    fun hashMetadataKey(metadataKey: String): String {
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(metadataKey.toByteArray())

        return BigInteger(1, bytes).toString(16).padStart(32, '0')
    }

    fun signMessage(cert: X509Certificate, key: PrivateKey, data: ByteArray): CMSSignedData {
        /*
         * Sign the data with key, and embed the certificate associated within the CMSSignedData
         */
        val content: CMSTypedData = CMSProcessableByteArray(data)
        val certList = ArrayList<Any>()
        certList.add(cert)
        val certs = JcaCertStore(certList)
        val signGen = CMSSignedDataGenerator()
        val sha1signer = JcaContentSignerBuilder("SHA1withRSA").setProvider("BC").build(key)
        signGen.addSignerInfoGenerator(
            JcaSignerInfoGeneratorBuilder(JcaDigestCalculatorProviderBuilder().build()).build(
                sha1signer,
                cert
            )
        )
        signGen.addCertificates(certs)
        return signGen.generate(
            content,
            true
        ) //content could have been the content of File zip = new File("fichier.zip"); CMSProcessableFile content = new CMSProcessableFile(zip);
    }

    fun signMessageSimple(cert: X509Certificate, key: PrivateKey, data: ByteArray): CMSSignedData {
        /*
         * Sign data with the private key, but does not embed the certificate
         * Still need the certificate to identify signer
         */
        Security.addProvider(BouncyCastleProvider())
        val content: CMSTypedData = CMSProcessableByteArray(data)

        val signGen = CMSSignedDataGenerator()
        val sha1signer: ContentSigner = JcaContentSignerBuilder("SHA1withRSA").setProvider("BC").build(key)
        signGen.addSignerInfoGenerator(
            JcaSignerInfoGeneratorBuilder(JcaDigestCalculatorProviderBuilder().build())
                .build(
                    sha1signer,
                    cert
                )
        )
        return signGen.generate(
            content,
            true
        )
    }

    fun verifySignedMessage(data: CMSSignedData, cert: X509Certificate?): Boolean {
        /*
         * Verify the signature but does not use the certificate in the signed object
         */

        val signer: SignerInformation = data.signerInfos.signers.iterator().next() as SignerInformation
        if (signer.verify(JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(cert))) {
            //return data.signedContent.content
            return true
        }
        return false
    }
}
