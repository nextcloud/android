/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFileV1
import com.owncloud.android.datamodel.e2e.v1.encrypted.EncryptedFolderMetadataFileV1
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedMetadata
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedUser
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedFiledrop
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedFolderMetadataFile
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedMetadata
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedUser
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.e2ee.GetMetadataRemoteOperation
import com.owncloud.android.lib.resources.e2ee.MetadataResponse
import com.owncloud.android.lib.resources.e2ee.StoreMetadataV2RemoteOperation
import com.owncloud.android.lib.resources.e2ee.UpdateMetadataV2RemoteOperation
import com.owncloud.android.operations.UploadException
import org.apache.commons.httpclient.HttpStatus
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.cms.ContentInfo
import org.bouncycastle.cert.jcajce.JcaCertStore
import org.bouncycastle.cms.CMSProcessableByteArray
import org.bouncycastle.cms.CMSSignedData
import org.bouncycastle.cms.CMSSignedDataGenerator
import org.bouncycastle.cms.SignerInformation
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.math.BigInteger
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

@Suppress("TooManyFunctions", "LargeClass")
class EncryptionUtilsV2 {
    @VisibleForTesting
    fun encryptMetadata(metadata: DecryptedMetadata, metadataKey: ByteArray): EncryptedMetadata {
        val json = EncryptionUtils.serializeJSON(metadata, true)
        val gzip = gZipCompress(json)

        return EncryptionUtils.encryptStringSymmetric(
            gzip,
            metadataKey,
            EncryptionUtils.ivDelimiter
        )
    }

    @VisibleForTesting
    fun decryptMetadata(metadata: EncryptedMetadata, metadataKey: ByteArray): DecryptedMetadata {
        val decrypted = EncryptionUtils.decryptStringSymmetric(
            metadata.ciphertext,
            metadataKey,
            metadata.authenticationTag,
            metadata.nonce
        )
        val json = gZipDecompress(decrypted)

        val decryptedMetadata = EncryptionUtils.deserializeJSON(json, object : TypeToken<DecryptedMetadata>() {})
        decryptedMetadata.metadataKey = metadataKey

        return decryptedMetadata
    }

    @Suppress("LongParameterList")
    fun encryptFolderMetadataFile(
        metadataFile: DecryptedFolderMetadataFile,
        userId: String,
        folder: OCFile,
        storageManager: FileDataStorageManager,
        client: OwnCloudClient,
        privateKey: String,
        user: User,
        context: Context,
        arbitraryDataProvider: ArbitraryDataProvider
    ): EncryptedFolderMetadataFile {
        val encryptedUsers: List<EncryptedUser>
        val encryptedMetadata: EncryptedMetadata
        if (metadataFile.users.isEmpty()) {
            // we are in a subfolder, re-use users array
            val key = retrieveTopMostMetadataKey(
                folder,
                storageManager,
                client,
                userId,
                privateKey,
                user,
                context,
                arbitraryDataProvider
            )

            // do not store metadata key
            metadataFile.metadata.metadataKey = ByteArray(0)
            metadataFile.metadata.keyChecksums.clear()

            encryptedUsers = emptyList()
            encryptedMetadata = encryptMetadata(metadataFile.metadata, key)
        } else {
            encryptedUsers = metadataFile.users.map {
                encryptUser(
                    it,
                    metadataFile.metadata.metadataKey
                )
            }
            encryptedMetadata = encryptMetadata(metadataFile.metadata, metadataFile.metadata.metadataKey)
        }

        return EncryptedFolderMetadataFile(
            encryptedMetadata,
            encryptedUsers,
            mutableMapOf()
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

    @Throws(IllegalStateException::class, UploadException::class, Throwable::class)
    @Suppress("LongParameterList", "LongMethod", "ThrowsCount")
    fun decryptFolderMetadataFile(
        metadataFile: EncryptedFolderMetadataFile,
        userId: String,
        privateKey: String,
        ocFile: OCFile,
        storageManager: FileDataStorageManager,
        client: OwnCloudClient,
        oldCounter: Long,
        signature: String,
        user: User,
        context: Context,
        arbitraryDataProvider: ArbitraryDataProvider
    ): DecryptedFolderMetadataFile {
        val parent =
            storageManager.getFileById(ocFile.parentId) ?: throw IllegalStateException("Cannot retrieve metadata")

        var filesDropCountBefore = 0
        var filesBefore = 0
        val decryptedFolderMetadataFile = if (parent.isEncrypted) {
            // we are in a subfolder, decrypt information is in top most encrypted folder
            val topMostMetadata = retrieveTopMostMetadata(
                ocFile,
                storageManager,
                client,
                userId,
                privateKey,
                user,
                context,
                arbitraryDataProvider
            )

            val decryptedMetadata = decryptMetadata(metadataFile.metadata, topMostMetadata.metadata.metadataKey)
            decryptedMetadata.metadataKey = topMostMetadata.metadata.metadataKey
            decryptedMetadata.keyChecksums.addAll(topMostMetadata.metadata.keyChecksums)

            DecryptedFolderMetadataFile(
                decryptedMetadata,
                // subfolder do not store user array
                mutableListOf(),
                mutableMapOf()
            )
        } else {
            // Top folder
            val encryptedUser = metadataFile.users.find { it.userId == userId }
                ?: throw IllegalStateException("Cannot find current user in metadata")

            val decryptedMetadataKey = decryptMetadataKey(encryptedUser, privateKey)

            val users = metadataFile.users.map { transformUser(it) }.toMutableList()

            val decryptedMetadata = decryptMetadata(
                metadataFile.metadata,
                decryptedMetadataKey
            )

            // only top folder can have files drop
            filesBefore = decryptedMetadata.files.size
            if (metadataFile.filedrop != null) {
                filesDropCountBefore = metadataFile.filedrop.size
            }

            val fileDrop = metadataFile.filedrop
            if (fileDrop != null) {
                for (entry in fileDrop) {
                    val key = entry.key
                    val encryptedFile = entry.value

                    val decryptedFile = decryptFiledrop(
                        encryptedFile,
                        privateKey,
                        arbitraryDataProvider,
                        user
                    )

                    decryptedMetadata.files[key] = decryptedFile
                }
            }

            DecryptedFolderMetadataFile(
                decryptedMetadata,
                users,
                mutableMapOf()
            )
        }

        verifyMetadata(metadataFile, decryptedFolderMetadataFile, oldCounter, signature)

        val transferredFiledrop = filesDropCountBefore > 0 &&
            decryptedFolderMetadataFile.metadata.files.size == filesBefore + filesDropCountBefore

        if (transferredFiledrop) {
            // lock folder
            val token = EncryptionUtils.lockFolder(ocFile, client)

            serializeAndUploadMetadata(
                ocFile,
                decryptedFolderMetadataFile,
                token,
                client,
                true,
                context,
                user,
                storageManager
            )

            // unlock folder
            val unlockFolderResult: RemoteOperationResult<*> = EncryptionUtils.unlockFolder(ocFile, client, token)
            if (!unlockFolderResult.isSuccess) {
                Log_OC.e(TAG, unlockFolderResult.message)
                throw IllegalStateException()
            }
        }

        return decryptedFolderMetadataFile
    }

    @Throws(Throwable::class)
    fun decryptFiledrop(
        filedrop: EncryptedFiledrop,
        privateKey: String,
        arbitraryDataProvider: ArbitraryDataProvider,
        user: User
    ): DecryptedFile {
        // decrypt key
        val encryptedKey = EncryptionUtils.decryptStringAsymmetricAsBytes(
            filedrop.users[0].encryptedFiledropKey,
            privateKey
        )

        // decrypt encrypted blob with key
        val decryptedData = EncryptionUtils.decryptStringSymmetricAsString(
            filedrop.ciphertext,
            encryptedKey,
            EncryptionUtils.decodeStringToBase64Bytes(filedrop.nonce),
            EncryptionUtils.decodeStringToBase64Bytes(filedrop.authenticationTag),
            true,
            arbitraryDataProvider,
            user
        )

        return EncryptionUtils.deserializeJSON(
            decryptedData,
            object : TypeToken<DecryptedFile>() {}
        )
    }

    @Throws(IllegalStateException::class)
    @Suppress("ThrowsCount", "LongParameterList")
    fun retrieveTopMostMetadata(
        folder: OCFile,
        storageManager: FileDataStorageManager,
        client: OwnCloudClient,
        userId: String,
        privateKey: String,
        user: User,
        context: Context,
        arbitraryDataProvider: ArbitraryDataProvider
    ): DecryptedFolderMetadataFile {
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
                result.resultData.metadata,
                object : TypeToken<EncryptedFolderMetadataFile>() {}
            )

            return decryptFolderMetadataFile(
                v2,
                userId,
                privateKey,
                topMost,
                storageManager,
                client,
                topMost.e2eCounter,
                result.resultData.signature,
                user,
                context,
                arbitraryDataProvider
            )
        } else {
            throw IllegalStateException("Cannot retrieve metadata")
        }
    }

    @Throws(IllegalStateException::class)
    @Suppress("ThrowsCount", "LongParameterList")
    fun retrieveTopMostMetadataKey(
        folder: OCFile,
        storageManager: FileDataStorageManager,
        client: OwnCloudClient,
        userId: String,
        privateKey: String,
        user: User,
        context: Context,
        arbitraryDataProvider: ArbitraryDataProvider
    ): ByteArray {
        return retrieveTopMostMetadata(
            folder,
            storageManager,
            client,
            userId,
            privateKey,
            user,
            context,
            arbitraryDataProvider
        )
            .metadata.metadataKey
    }

    @VisibleForTesting
    fun encryptUser(user: DecryptedUser, metadataKey: ByteArray): EncryptedUser {
        val encryptedKey = EncryptionUtils.encryptStringAsymmetricV2(
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
            user.certificate
        )
    }

    @VisibleForTesting
    fun decryptMetadataKey(user: EncryptedUser, privateKey: String): ByteArray {
        return EncryptionUtils.decryptStringAsymmetricV2(
            user.encryptedMetadataKey,
            privateKey
        )
    }

    fun gZipCompress(string: String): ByteArray {
        val outputStream = ByteArrayOutputStream()
        GZIPOutputStream(outputStream).apply {
            write(string.toByteArray())
            flush()
            close()
        }

        return outputStream.toByteArray()
    }

    fun gZipDecompress(compressed: String): String {
        return gZipDecompress(compressed.byteInputStream())
    }

    fun gZipDecompress(compressed: ByteArray): String {
        return gZipDecompress(compressed.inputStream())
    }

    @VisibleForTesting
    fun gZipDecompress(inputStream: InputStream): String {
        val stringBuilder = StringBuilder()
        val inputStream = GZIPInputStream(inputStream)
        // val inputStream = compressed.inputStream()
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))

        // val sb = java.lang.StringBuilder()
        // for (b in compressed) {
        //     sb.append(String.format("%02X ", b))
        // }
        // val out = sb.toString()

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
        metadataFile.metadata.metadataKey = EncryptionUtils.generateKey()
        metadataFile.metadata.keyChecksums.add(hashMetadataKey(metadataFile.metadata.metadataKey))

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

        metadataFile.metadata.metadataKey = EncryptionUtils.generateKey()
        metadataFile.metadata.keyChecksums.add(hashMetadataKey(metadataFile.metadata.metadataKey))

        return metadataFile
    }

    @Suppress("LongParameterList")
    fun addFileToMetadata(
        encryptedFileName: String,
        ocFile: OCFile,
        initializationVector: ByteArray,
        authenticationTag: String,
        key: ByteArray,
        metadataFile: DecryptedFolderMetadataFile,
        fileDataStorageManager: FileDataStorageManager
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
        ocFile.setE2eCounter(metadataFile.metadata.counter)
        fileDataStorageManager.saveFile(ocFile)

        return metadataFile
    }

    fun addFolderToMetadata(
        encryptedFileName: String,
        fileName: String,
        metadataFile: DecryptedFolderMetadataFile,
        ocFile: OCFile,
        fileDataStorageManager: FileDataStorageManager
    ): DecryptedFolderMetadataFile {
        metadataFile.metadata.folders[encryptedFileName] = fileName
        metadataFile.metadata.counter++
        ocFile.setE2eCounter(metadataFile.metadata.counter)
        fileDataStorageManager.saveFile(ocFile)

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
    fun removeFileFromMetadata(fileName: String, metadata: DecryptedFolderMetadataFile) {
        metadata.metadata.files.remove(fileName)
            ?: throw IllegalStateException("File $fileName not found in metadata!")
    }

    @Throws(IllegalStateException::class)
    fun renameFile(key: String, newName: String, metadataFile: DecryptedFolderMetadataFile) {
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
            val metadataResponse = getMetadataOperationResult.resultData
            val metadata = parseAnyMetadata(
                metadataResponse,
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
    @Suppress("TooGenericExceptionCaught")
    fun parseAnyMetadata(
        metadataResponse: MetadataResponse,
        user: User,
        client: OwnCloudClient,
        context: Context,
        folder: OCFile
    ): DecryptedFolderMetadataFile {
        val arbitraryDataProvider: ArbitraryDataProvider = ArbitraryDataProviderImpl(context)
        val privateKey: String = arbitraryDataProvider.getValue(user.accountName, EncryptionUtils.PRIVATE_KEY)
        val storageManager = FileDataStorageManager(user, context.contentResolver)

        val v2 = EncryptionUtils.deserializeJSON(
            metadataResponse.metadata,
            object : TypeToken<EncryptedFolderMetadataFile>() {}
        )

        val decryptedFolderMetadata = if (v2.version == "2.0" || v2.version == "2") {
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
                client,
                folder.e2eCounter,
                metadataResponse.signature,
                user,
                context,
                arbitraryDataProvider
            )
        } else {
            // try to deserialize v1
            val v1 = EncryptionUtils.deserializeJSON(
                metadataResponse.metadata,
                object : TypeToken<EncryptedFolderMetadataFileV1?>() {}
            )

            // decrypt
            try {
                // decrypt metadata
                val decryptedV1 = EncryptionUtils.decryptFolderMetaData(
                    v1,
                    privateKey,
                    arbitraryDataProvider,
                    user,
                    folder.localId
                )
                val publicKey: String = arbitraryDataProvider.getValue(
                    user.accountName,
                    EncryptionUtils.PUBLIC_KEY
                )

                // migrate to v2
                migrateV1ToV2andUpload(
                    decryptedV1,
                    client.userIdPlain,
                    publicKey,
                    folder,
                    storageManager,
                    client,
                    user,
                    context
                )
            } catch (e: Exception) {
                // TODO do better
                throw IllegalStateException("Cannot decrypt metadata")
            }
        }

        // TODO verify metadata
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
//                EncryptedFolderMetadataFile encryptedFolderMetadataNew =
//                encryptFolderMetadata(decryptedFolderMetadata, privateKey);
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

        // TODO to check
//                try {
//                    int filesDropCountBefore = 0;
//                    if (encryptedFolderMetadata.getFiledrop() != null) {
//                        filesDropCountBefore = encryptedFolderMetadata.getFiledrop().size();
//                    }
//                    DecryptedFolderMetadataFile decryptedFolderMetadata = EncryptionUtils.decryptFolderMetaData(
//                        encryptedFolderMetadata,
//                        privateKey,
//                        arbitraryDataProvider,
//                        user,
//                        folder.getLocalId());
//
//                    boolean transferredFiledrop = filesDropCountBefore > 0 &&
//                    decryptedFolderMetadata.getFiles().size() ==
//                        encryptedFolderMetadata.getFiles().size() + filesDropCountBefore;
//
//                    if (transferredFiledrop) {
//                        // lock folder
//                        String token = EncryptionUtils.lockFolder(folder, client);
//
//                        // upload metadata
//                        EncryptedFolderMetadata encryptedFolderMetadataNew =
//                        encryptFolderMetadata(decryptedFolderMetadata,
//                                              publicKey,
//                                              arbitraryDataProvider,
//                                              user,
//                                              folder.getLocalId());
//
    }

    @Throws(UploadException::class)
    @Suppress("LongParameterList")
    fun migrateV1ToV2andUpload(
        v1: DecryptedFolderMetadataFileV1,
        userId: String,
        cert: String,
        folder: OCFile,
        storageManager: FileDataStorageManager,
        client: OwnCloudClient,
        user: User,
        context: Context
    ): DecryptedFolderMetadataFile {
        val newMetadata = migrateV1ToV2(
            v1,
            userId,
            cert,
            folder,
            storageManager
        )
        // lock
        val token = EncryptionUtils.lockFolder(folder, client)

        // upload
        serializeAndUploadMetadata(
            folder,
            newMetadata,
            token,
            client,
            true,
            context,
            user,
            storageManager
        )

        // unlock
        EncryptionUtils.unlockFolder(folder, client, token)

        return newMetadata
    }

    @Throws(IllegalStateException::class)
    fun migrateV1ToV2(
        v1: DecryptedFolderMetadataFileV1,
        userId: String,
        cert: String,
        folder: OCFile,
        storageManager: FileDataStorageManager
    ): DecryptedFolderMetadataFile {
        // key
        val key = if (v1.metadata.metadataKeys != null && v1.metadata.metadataKeys.size > 1) {
            v1.metadata.metadataKeys[0]
        } else {
            v1.metadata.metadataKey
        }

        // create new metadata
        val metadataV2 = DecryptedMetadata(
            mutableListOf(),
            false,
            0,
            v1
                .files
                .filter { it.value.encrypted.mimetype == MimeType.WEBDAV_FOLDER }
                .mapValues { it.value.encrypted.filename }
                .toMutableMap(),
            v1
                .files
                .filter { it.value.encrypted.mimetype != MimeType.WEBDAV_FOLDER }
                .mapValues { migrateDecryptedFileV1ToV2(it.value) }
                .toMutableMap(),
            EncryptionUtils.decodeStringToBase64Bytes(key) ?: throw IllegalStateException("Metadata key not found!")
        )

        // upon migration there can only be one user, as there is no sharing yet in place
        val users = if (storageManager.getFileById(folder.parentId)?.isEncrypted == false) {
            mutableListOf(DecryptedUser(userId, cert))
        } else {
            mutableListOf()
        }

        // TODO
        val filedrop = mutableMapOf<String, DecryptedFile>()

        val newMetadata = DecryptedFolderMetadataFile(metadataV2, users, filedrop)
        val metadataKey = EncryptionUtils.generateKey() ?: throw UploadException("Could not encrypt folder!")

        newMetadata.metadata.metadataKey = metadataKey
        newMetadata.metadata.keyChecksums.add(EncryptionUtilsV2().hashMetadataKey(metadataKey))

        return newMetadata
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
    @Suppress("LongParameterList")
    fun serializeAndUploadMetadata(
        folder: OCFile,
        metadata: DecryptedFolderMetadataFile,
        token: String,
        client: OwnCloudClient,
        metadataExists: Boolean,
        context: Context,
        user: User,
        storageManager: FileDataStorageManager
    ) {
        serializeAndUploadMetadata(
            folder.remoteId,
            metadata,
            token,
            client,
            metadataExists,
            context,
            user,
            folder,
            storageManager
        )
    }

    @Throws(UploadException::class)
    @Suppress("LongParameterList")
    fun serializeAndUploadMetadata(
        remoteId: String,
        metadata: DecryptedFolderMetadataFile,
        token: String,
        client: OwnCloudClient,
        metadataExists: Boolean,
        context: Context,
        user: User,
        folder: OCFile,
        storageManager: FileDataStorageManager
    ) {
        val arbitraryDataProvider: ArbitraryDataProvider = ArbitraryDataProviderImpl(context)
        val privateKeyString: String = arbitraryDataProvider.getValue(user.accountName, EncryptionUtils.PRIVATE_KEY)
        val publicKeyString: String = arbitraryDataProvider.getValue(user.accountName, EncryptionUtils.PUBLIC_KEY)

        val encryptedFolderMetadata = encryptFolderMetadataFile(
            metadata,
            client.userId,
            folder,
            storageManager,
            client,
            privateKeyString,
            user,
            context,
            arbitraryDataProvider
        )
        val serializedFolderMetadata = EncryptionUtils.serializeJSON(encryptedFolderMetadata, true)
        val cert = EncryptionUtils.convertCertFromString(publicKeyString)
        val privateKey = EncryptionUtils.PEMtoPrivateKey(privateKeyString)

        val signature = getMessageSignature(cert, privateKey, encryptedFolderMetadata)
        val uploadMetadataOperationResult = if (metadataExists) {
            // update metadata
            UpdateMetadataV2RemoteOperation(
                remoteId,
                serializedFolderMetadata,
                token,
                signature
            )
                .execute(client)
        } else {
            // store metadata
            StoreMetadataV2RemoteOperation(
                remoteId,
                serializedFolderMetadata,
                token,
                signature
            )
                .execute(client)
        }
        if (!uploadMetadataOperationResult.isSuccess) {
            if (metadataExists) {
                throw UploadException("Updating metadata was not successful")
            } else {
                throw UploadException("Storing metadata was not successful")
            }
        }
    }

    @Throws(IllegalStateException::class)
    @Suppress("ThrowsCount")
    @VisibleForTesting
    fun verifyMetadata(
        encryptedFolderMetadataFile: EncryptedFolderMetadataFile,
        decryptedFolderMetadataFile: DecryptedFolderMetadataFile,
        oldCounter: Long,
        // base 64 encoded BER
        ans: String
    ) {
        // check counter
        if (decryptedFolderMetadataFile.metadata.counter < oldCounter) {
            throw IllegalStateException("Counter is too old")
        }

        // check signature
        val json = EncryptionUtils.serializeJSON(encryptedFolderMetadataFile, true)
        val certs = decryptedFolderMetadataFile.users.map { EncryptionUtils.convertCertFromString(it.certificate) }

        val base64 = EncryptionUtils.encodeStringToBase64String(json)

        // if (!verifySignedMessage(ans, base64, certs)) {
        //     throw IllegalStateException("Signature does not match")
        // }

        val hashedMetadataKey = hashMetadataKey(decryptedFolderMetadataFile.metadata.metadataKey)
        if (!decryptedFolderMetadataFile.metadata.keyChecksums.contains(hashedMetadataKey)) {
            throw IllegalStateException("Hash not found")
            // TODO E2E: fake this to present problem to user
        }

        // TODO E2E: check certs
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
    @Suppress("MagicNumber")
    fun hashMetadataKey(metadataKey: ByteArray): String {
        val bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(metadataKey)

        return BigInteger(1, bytes).toString(16).padStart(32, '0')
    }

    fun signMessage(cert: X509Certificate, key: PrivateKey, data: ByteArray): CMSSignedData {
        val content = CMSProcessableByteArray(data)
        val certs = JcaCertStore(listOf(cert))

        val sha1signer = JcaContentSignerBuilder("SHA256withRSA").build(key)
        val signGen = CMSSignedDataGenerator().apply {
            addSignerInfoGenerator(
                JcaSignerInfoGeneratorBuilder(JcaDigestCalculatorProviderBuilder().build()).build(
                    sha1signer,
                    cert
                )
            )
            addCertificates(certs)
        }
        return signGen.generate(
            content,
            false
        )
    }

    /**
     * Sign the data with key, embed the certificate associated within the CMSSignedData
     * detached data not possible, as to restore asn.1
     */
    fun signMessage(cert: X509Certificate, key: PrivateKey, message: EncryptedFolderMetadataFile): CMSSignedData {
        val json = EncryptionUtils.serializeJSON(message, true)
        val base64 = EncryptionUtils.encodeStringToBase64String(json)
        val data = base64.toByteArray()

        return signMessage(cert, key, data)
    }

    fun signMessage(cert: X509Certificate, key: PrivateKey, string: String): CMSSignedData {
        val base64 = EncryptionUtils.encodeStringToBase64String(string)
        val data = base64.toByteArray()

        return signMessage(cert, key, data)
    }

    fun extractSignedString(signedData: CMSSignedData): String {
        val ans = signedData.getEncoded("BER")
        return EncryptionUtils.encodeBytesToBase64String(ans)
    }

    fun getMessageSignature(cert: String, privateKey: String, metadataFile: EncryptedFolderMetadataFile): String {
        return getMessageSignature(
            EncryptionUtils.convertCertFromString(cert),
            EncryptionUtils.PEMtoPrivateKey(privateKey),
            metadataFile
        )
    }

    fun getMessageSignature(cert: X509Certificate, key: PrivateKey, message: EncryptedFolderMetadataFile): String {
        val signedMessage = signMessage(cert, key, message)
        return extractSignedString(signedMessage)
    }

    fun getMessageSignature(cert: X509Certificate, key: PrivateKey, string: String): String {
        val signedMessage = signMessage(cert, key, string)
        return extractSignedString(signedMessage)
    }

    /**
     * Verify the signature but does not use the certificate in the signed object
     */
    fun verifySignedMessage(data: CMSSignedData, certs: List<X509Certificate>): Boolean {
        val signer: SignerInformation = data.signerInfos.signers.iterator().next() as SignerInformation

        certs.forEach {
            try {
                if (signer.verify(JcaSimpleSignerInfoVerifierBuilder().build(it))) {
                    return true
                }
            } catch (e: java.lang.Exception) {
                Log_OC.e("Encryption", "error", e)
            }
        }

        return false
    }

    /**
     * Verify the signature but does not use the certificate in the signed object
     */
    fun verifySignedMessage(base64encodedAns: String, originalMessage: String, certs: List<X509Certificate>): Boolean {
        val ans = EncryptionUtils.decodeStringToBase64Bytes(base64encodedAns)
        val contentInfo = ContentInfo.getInstance(ASN1Sequence.fromByteArray(ans))
        val content = CMSProcessableByteArray(originalMessage.toByteArray())
        val sig = CMSSignedData(content, contentInfo)

        return verifySignedMessage(sig, certs)
    }

    companion object {
        private val TAG = EncryptionUtils::class.java.simpleName
    }
}
