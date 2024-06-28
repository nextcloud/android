/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import com.google.gson.reflect.TypeToken
import com.nextcloud.client.account.MockUser
import com.nextcloud.common.User
import com.owncloud.android.EncryptionIT
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.e2e.v1.decrypted.Data
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFileV1
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedMetadata
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedUser
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedFiledrop
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedFiledropUser
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedFolderMetadataFile
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.util.EncryptionTestIT
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertNotEquals
import org.junit.Test

@Suppress("TooManyFunctions", "LargeClass")
class EncryptionUtilsV2IT : EncryptionIT() {
    private val encryptionTestUtils = EncryptionTestUtils()
    private val encryptionUtilsV2 = EncryptionUtilsV2()

    private val enc1UserId = "enc1"
    private val enc1PrivateKey = """
        MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAo
        IBAQDsn0JKS/THu328z1IgN0VzYU53HjSX03WJIgWkmyTaxbiKpoJaKbksXmfSpgzV
        GzKFvGfZ03fwFrN7Q8P8R2e8SNiell7mh1TDw9/0P7Bt/ER8PJrXORo+GviKHxaLr7
        Y0BJX9i/nW/L0L/VaE8CZTAqYBdcSJGgHJjY4UMf892ZPTa9T2Dl3ggdMZ7BQ2kiCi
        CC3qV99b0igRJGmmLQaGiAflhFzuDQPMifUMq75wI8RSRPdxUAtjTfkl68QHu7Umye
        yy33OQgdUKaTl5zcS3VSQbNjveVCNM4RDH1RlEc+7Wf1BY8APqT6jbiBcROJD2CeoL
        H2eiIJCi+61ZkSGfAgMBAAECggEBALFStCHrhBf+GL9a+qer4/8QZ/X6i91PmaBX/7
        SYk2jjjWVSXRNmex+V6+Y/jBRT2mvAgm8J+7LPwFdatE+lz0aZrMRD2gCWYF6Itpda
        90OlLkmQPVWWtGTgX2ta2tF5r2iSGzk0IdoL8zw98Q2UzpOcw30KnWtFMxuxWk0mHq
        pgp00g80cDWg3+RPbWOhdLp5bflQ36fKDfmjq05cGlIk6unnVyC5HXpvh4d4k2EWlX
        rjGsndVBPCjGkZePlLRgDHxT06r+5XdJ+1CBDZgCsmjGz3M8uOHyCfVW0WhB7ynzDT
        agVgz0iqpuhAi9sPt6iWWwpAnRw8cQgqEKw9bvKKECgYEA/WPi2PJtL6u/xlysh/H7
        A717CId6fPHCMDace39ZNtzUzc0nT5BemlcF0wZ74NeJSur3Q395YzB+eBMLs5p8mA
        95wgGvJhM65/J+HX+k9kt6Z556zLMvtG+j1yo4D0VEwm3xahB4SUUP+1kD7dNvo4+8
        xeSCyjzNllvYZZC0DrECgYEA7w8pEqhHHn0a+twkPCZJS+gQTB9Rm+FBNGJqB3XpWs
        TeLUxYRbVGk0iDve+eeeZ41drxcdyWP+WcL34hnrjgI1Fo4mK88saajpwUIYMy6+qM
        LY+jC2NRSBox56eH7nsVYvQQK9eKqv9wbB+PF9SwOIvuETN7fd8mAY02UnoaaU8CgY
        BoHRKocXPLkpZJuuppMVQiRUi4SHJbxDo19Tp2w+y0TihiJ1lvp7I3WGpcOt3LlMQk
        tEbExSvrRZGxZKH6Og/XqwQsYuTEkEIz679F/5yYVosE6GkskrOXQAfh8Mb3/04xVV
        tMaVgDQw0+CWVD4wyL+BNofGwBDNqsXTCdCsfxAQKBgQCDv2EtbRw0y1HRKv21QIxo
        ju5cZW4+cDfVPN+eWPdQFOs1H7wOPsc0aGRiiupV2BSEF3O1ApKziEE5U1QH+29bR4
        R8L1pemeGX8qCNj5bCubKjcWOz5PpouDcEqimZ3q98p3E6GEHN15UHoaTkx0yO/V8o
        j6zhQ9fYRxDHB5ACtQKBgQCOO7TJUO1IaLTjcrwS4oCfJyRnAdz49L1AbVJkIBK0fh
        JLecOFu3ZlQl/RStQb69QKb5MNOIMmQhg8WOxZxHcpmIDbkDAm/J/ovJXFSoBdOr5o
        uQsYsDZhsWW97zvLMzg5pH9/3/1BNz5q3Vu4HgfBSwWGt4E2NENj+XA+QAVmGA==
    """.trimIndent()

    private val enc1Cert = """
        -----BEGIN CERTIFICATE-----
        MIIDpzCCAo+gAwIBAgIBADANBgkqhkiG9w0BAQUFADBuMRowGAYDVQQDDBF3d3cu
        bmV4dGNsb3VkLmNvbTESMBAGA1UECgwJTmV4dGNsb3VkMRIwEAYDVQQHDAlTdHV0
        dGdhcnQxGzAZBgNVBAgMEkJhZGVuLVd1ZXJ0dGVtYmVyZzELMAkGA1UEBhMCREUw
        HhcNMTcwOTI2MTAwNDMwWhcNMzcwOTIxMTAwNDMwWjBuMRowGAYDVQQDDBF3d3cu
        bmV4dGNsb3VkLmNvbTESMBAGA1UECgwJTmV4dGNsb3VkMRIwEAYDVQQHDAlTdHV0
        dGdhcnQxGzAZBgNVBAgMEkJhZGVuLVd1ZXJ0dGVtYmVyZzELMAkGA1UEBhMCREUw
        ggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDsn0JKS/THu328z1IgN0Vz
        YU53HjSX03WJIgWkmyTaxbiKpoJaKbksXmfSpgzVGzKFvGfZ03fwFrN7Q8P8R2e8
        SNiell7mh1TDw9/0P7Bt/ER8PJrXORo+GviKHxaLr7Y0BJX9i/nW/L0L/VaE8CZT
        AqYBdcSJGgHJjY4UMf892ZPTa9T2Dl3ggdMZ7BQ2kiCiCC3qV99b0igRJGmmLQaG
        iAflhFzuDQPMifUMq75wI8RSRPdxUAtjTfkl68QHu7Umyeyy33OQgdUKaTl5zcS3
        VSQbNjveVCNM4RDH1RlEc+7Wf1BY8APqT6jbiBcROJD2CeoLH2eiIJCi+61ZkSGf
        AgMBAAGjUDBOMB0GA1UdDgQWBBTFrXz2tk1HivD9rQ75qeoyHrAgIjAfBgNVHSME
        GDAWgBTFrXz2tk1HivD9rQ75qeoyHrAgIjAMBgNVHRMEBTADAQH/MA0GCSqGSIb3
        DQEBBQUAA4IBAQARQTX21QKO77gAzBszFJ6xVnjfa23YZF26Z4X1KaM8uV8TGzuN
        JA95XmReeP2iO3r8EWXS9djVCD64m2xx6FOsrUI8HZaw1JErU8mmOaLAe8q9RsOm
        9Eq37e4vFp2YUEInYUqs87ByUcA4/8g3lEYeIUnRsRsWsA45S3wD7wy07t+KAn7j
        yMmfxdma6hFfG9iN/egN6QXUAyIPXvUvlUuZ7/BhWBj/3sHMrF9quy9Q2DOI8F3t
        1wdQrkq4BtStKhciY5AIXz9SqsctFHTv4Lwgtkapoel4izJnO0ZqYTXVe7THwri9
        H/gua6uJDWH9jk2/CiZDWfsyFuNUuXvDSp05
        -----END CERTIFICATE-----
    """.trimIndent()

    private val enc2Cert = """
        -----BEGIN CERTIFICATE-----
        MIIC7DCCAdSgAwIBAgIBADANBgkqhkiG9w0BAQUFADAPMQ0wCwYDVQQDDARlbmMz
        MB4XDTIwMDcwODA3MzE1OFoXDTQwMDcwMzA3MzE1OFowDzENMAsGA1UEAwwEZW5j
        MzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAI/83eC/EF3xOocwjO+Z
        ZkPc1TFxt3aUgjEvrpZu45LOqesG67kkkVDYgjeg3Biz9XRUQXqtXaAyxRZH8GiH
        PFyKUiP1bUlCptd8X+hk9vxeN25YS5OS2RrxU9tDQ/dVOHr20427UvVCighotQnR
        /6+md1FQMV92PFxji7OP5TWOE1y389X6eb7kSPLs8Tu+2PpqaNVQ9C/89Y8KNYWs
        x9Zo+kbQhjfFFUikEpkuzMgT9QLaeq6xuXIPP+y1tzNmF6NTL0a2GoYULuxYWnCe
        joFyXj77LuLmK+KXfPdhvlxa5Kl9XHSxKPHBVVQpwPqNMT+b2T1VLE2l7M9NfImy
        iLcCAwEAAaNTMFEwHQYDVR0OBBYEFBKubDeR2lXwuyTrdyv6O7euPS4PMB8GA1Ud
        IwQYMBaAFBKubDeR2lXwuyTrdyv6O7euPS4PMA8GA1UdEwEB/wQFMAMBAf8wDQYJ
        KoZIhvcNAQEFBQADggEBAChCOIH8CkEpm1eqjsuuNPa93aduLjtnZXat5eIKsKCl
        rL9nFslpg/DO5SeU5ynPY9F2QjX5CN/3RxDXum9vFfpXhTJphOv8N0uHU4ucmQxE
        DN388Vt5VtN3V2pzNUL3JSiG6qeYG047/r/zhGFVpcgb2465G5mEwFT0qnkEseCC
        VVZ63GN8hZgUobyRXxMIhkfWlbO1dgABB4VNyudq0CW8urmewkkbUBwCslvtUvPM
        WuzpQjq2A80bvbrAqO5VUfvMcqRiUWkDgfa6cHXyV0o4N11mMIoxsMgh+PFYr6lR
        BHkuQHqKEwP8kkWugIFj3TMcy9dYtXfMXWvzFaDoE4s=
        -----END CERTIFICATE-----
    """.trimIndent()

    private val enc2PrivateKey = """
        MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCP/N3gvxBd8TqH
        MIzvmWZD3NUxcbd2lIIxL66WbuOSzqnrBuu5JJFQ2II3oNwYs/V0VEF6rV2gMsUW
        R/BohzxcilIj9W1JQqbXfF/oZPb8XjduWEuTktka8VPbQ0P3VTh69tONu1L1QooI
        aLUJ0f+vpndRUDFfdjxcY4uzj+U1jhNct/PV+nm+5Ejy7PE7vtj6amjVUPQv/PWP
        CjWFrMfWaPpG0IY3xRVIpBKZLszIE/UC2nqusblyDz/stbczZhejUy9GthqGFC7s
        WFpwno6Bcl4++y7i5ivil3z3Yb5cWuSpfVx0sSjxwVVUKcD6jTE/m9k9VSxNpezP
        TXyJsoi3AgMBAAECggEACWwKFtlZ2FPfORZ3unwGwZ0TRFOFJljMdiyBF6307Vfh
        rZP729clPS2Vw88eZ+1qu+yBhmYO0NtRo0Yc2LI0xHd2rYyzVI5sfYBRhFMLCHOf
        2/QiKet7knRFQP1TVr14Xy+Eo2slIBB1GNzFL/nSaeuSNjtxp6YEiCUpcJwTayAi
        Squ5QWMxhlciLKvwUkraFRBqkugvMz3jXzuk/i+DcYlOgoj+tytweNn/azOMH9MH
        mWI+3owYspjzE1rVpbrcWImvlnbInd0z9KaQPpBf7Njj7wtyBMaYww4K4GCMhboD
        SQCYgpnznWkPIN3jyXtmNVSsZ1nvD+Laod+0p7giOQKBgQDA6KEKctYpbt051yTe
        2UP8hpq+MUSS7FIXiHlUc8s0PSujouypUyzfrPeL6yquI0GtKHkMVCWwfT+otMZR
        VnklofrmPTPovvsUQFM4Di411NZwzfxEbBFyVXAUWcLd9NxJ1hZW7w+hLk/N5Bej
        DOa2CncZmifyMNIlvIn7T1vDyQKBgQC/FE8HaDBoN98m/3rEjx7/rVtX8dCei5By
        Fzg/yQ2u4ELbf/Qk/n4k75sy0690EwnFdJxVn2gdNgS1YDv8YP/N5Wfq8xnX9V9B
        irWY/W24cN2qDNXm5i8o5wklyt+fDVqMcEHFfONUpLC+RYmOdc1rrFxPaQOYYYpp
        dWsnuG0ofwKBgBm6rUf8ew35qG3/gP5sEgJLXbZCUfgapvRWkoAuFYs5IWno4BHR
        cym+IyI5Um75atgSjtqTGpfIjMYOnmjY1L2tNg6hWRwQ5OIVlkPiuE0bvyI6hwwF
        MeqC9LjyI+iAsSTz9fTQW9BOofw/ENwBa4AaMzpp8iv+UPkRhYHMWtvpAoGAX6As
        RMqxnxaHCR9GM2Rk4RPC6OpNu2qhKVfRgKp/vIrjKrKIXpM2UgnPo8oovnBgrX7E
        Vl1mX2gPRy4YFx/8JPCv5vcucdOMjmJ6q0v5QxrI9DdkPR/pbhDhlRZIf3LRZAMy
        B0GPC2c4RKDMTI1L9pzVvbASaoo2GLz4mXJEvsUCgYEAibwFNXz1H52sZtL6/1zQ
        1rHCTS8qkryBhxl5eYa6MV5YkbLJZZstF0w2nLxkPba8NttS/nJqjX/iJobD5uLb
        UzeD8jMeAWPNt4DZCtA4ossNYcXIMKqBVFKOANMvAAvLMpVdlNYSucNnTSQcLwI6
        2J9mW5WvAAaG+j28Q/GKSuE=
    """.trimIndent()

    @Test
    fun testEncryptDecryptMetadata() {
        val metadataKey = EncryptionUtils.generateKey()

        val metadata = DecryptedMetadata(
            mutableListOf("hash1", "hash of key 2"),
            false,
            1,
            mutableMapOf(
                Pair(EncryptionUtils.generateUid(), "Folder 1"),
                Pair(EncryptionUtils.generateUid(), "Folder 2"),
                Pair(EncryptionUtils.generateUid(), "Folder 3")
            ),
            mutableMapOf(
                Pair(
                    EncryptionUtils.generateUid(),
                    DecryptedFile(
                        "file 1.png",
                        "image/png",
                        "initializationVector",
                        "authenticationTag",
                        "key 1"
                    )
                ),
                Pair(
                    EncryptionUtils.generateUid(),
                    DecryptedFile(
                        "file 2.png",
                        "image/png",
                        "initializationVector 2",
                        "authenticationTag 2",
                        "key 2"
                    )
                )
            ),
            metadataKey
        )
        val encrypted = encryptionUtilsV2.encryptMetadata(metadata, metadataKey)
        val decrypted = encryptionUtilsV2.decryptMetadata(encrypted, metadataKey)

        assertEquals(metadata, decrypted)
    }

    @Throws(Throwable::class)
    @Test
    fun encryptDecryptSymmetric() {
        val string = "123"
        val metadataKey = EncryptionUtils.generateKeyString()

        val e = EncryptionUtils.encryptStringSymmetricAsString(
            string,
            metadataKey.toByteArray()
        )

        val d = EncryptionUtils.decryptStringSymmetric(e, metadataKey.toByteArray())
        assertEquals(string, d)

        val encryptedMetadata = EncryptionUtils.encryptStringSymmetric(
            string,
            metadataKey.toByteArray(),
            EncryptionUtils.ivDelimiter
        )

        val d2 = EncryptionUtils.decryptStringSymmetric(
            encryptedMetadata.ciphertext,
            metadataKey.toByteArray()
        )
        assertEquals(string, d2)

        val decrypted = EncryptionUtils.decryptStringSymmetric(
            encryptedMetadata.ciphertext,
            metadataKey.toByteArray(),
            encryptedMetadata.authenticationTag,
            encryptedMetadata.nonce
        )

        assertEquals(string, EncryptionUtils.decodeBase64BytesToString(decrypted))
    }

    @Test
    fun testEncryptDecryptUser() {
        val metadataKeyBase64 = EncryptionUtils.generateKeyString()
        val metadataKey = EncryptionUtils.decodeStringToBase64Bytes(metadataKeyBase64)

        val user = DecryptedUser("t1", encryptionTestUtils.t1PublicKey)

        val encryptedUser = encryptionUtilsV2.encryptUser(user, metadataKey)
        assertNotEquals(encryptedUser.encryptedMetadataKey, metadataKeyBase64)

        val decryptedMetadataKey = encryptionUtilsV2.decryptMetadataKey(encryptedUser, encryptionTestUtils.t1PrivateKey)
        val decryptedMetadataKeyBase64 = EncryptionUtils.encodeBytesToBase64String(decryptedMetadataKey)

        assertEquals(metadataKeyBase64, decryptedMetadataKeyBase64)
    }

    @Throws(com.owncloud.android.operations.UploadException::class, Throwable::class)
    @Test
    fun testEncryptDecryptMetadataFile() {
        val enc1 = MockUser("enc1", "Nextcloud")

        val root = OCFile("/")
        storageManager.saveFile(root)

        val folder = OCFile("/enc/").apply {
            parentId = storageManager.getFileByDecryptedRemotePath("/")?.fileId ?: throw IllegalStateException()
        }

        val metadataFile = generateDecryptedFolderMetadataFile(enc1, enc1Cert)

        val encrypted = encryptionUtilsV2.encryptFolderMetadataFile(
            metadataFile,
            enc1.accountName,
            folder,
            storageManager,
            client,
            enc1PrivateKey,
            user,
            targetContext,
            arbitraryDataProvider
        )

        val signature = encryptionUtilsV2.getMessageSignature(enc1Cert, enc1PrivateKey, encrypted)

        val decrypted = encryptionUtilsV2.decryptFolderMetadataFile(
            encrypted,
            enc1.accountName,
            enc1PrivateKey,
            folder,
            storageManager,
            client,
            0,
            signature,
            user,
            targetContext,
            arbitraryDataProvider
        )

        assertEquals(metadataFile, decrypted)
    }

    @Test
    fun addFile() {
        val enc1 = MockUser("enc1", "Nextcloud")
        val metadataFile = generateDecryptedFolderMetadataFile(enc1, enc1Cert)
        assertEquals(2, metadataFile.metadata.files.size)
        assertEquals(1, metadataFile.metadata.counter)

        val updatedMetadata = encryptionUtilsV2.addFileToMetadata(
            EncryptionUtils.generateUid(),
            OCFile("/test.jpg").apply {
                mimeType = MimeType.JPEG
            },
            EncryptionUtils.generateIV(),
            // random string, not real tag
            EncryptionUtils.generateUid(),
            EncryptionUtils.generateKey(),
            metadataFile,
            storageManager
        )

        assertEquals(3, updatedMetadata.metadata.files.size)
        assertEquals(2, updatedMetadata.metadata.counter)
    }

    @Test
    fun removeFile() {
        val enc1 = MockUser("enc1", "Nextcloud")
        val metadataFile = generateDecryptedFolderMetadataFile(enc1, enc1Cert)
        assertEquals(2, metadataFile.metadata.files.size)

        val filename = metadataFile.metadata.files.keys.first()

        encryptionUtilsV2.removeFileFromMetadata(filename, metadataFile)

        assertEquals(1, metadataFile.metadata.files.size)
    }

    @Test
    fun renameFile() {
        val enc1 = MockUser("enc1", "Nextcloud")
        val metadataFile = generateDecryptedFolderMetadataFile(enc1, enc1Cert)
        assertEquals(2, metadataFile.metadata.files.size)

        val key = metadataFile.metadata.files.keys.first()
        val decryptedFile = metadataFile.metadata.files[key]
        val filename = decryptedFile?.filename
        val newFilename = "New File 1"

        encryptionUtilsV2.renameFile(key, newFilename, metadataFile)

        assertEquals(newFilename, metadataFile.metadata.files[key]?.filename)
        assertNotEquals(filename, newFilename)
        assertNotEquals(filename, metadataFile.metadata.files[key]?.filename)
    }

    @Test
    fun addFolder() {
        val folder = OCFile("/e/")
        val enc1 = MockUser("enc1", "Nextcloud")
        val metadataFile = generateDecryptedFolderMetadataFile(enc1, enc1Cert)
        assertEquals(2, metadataFile.metadata.files.size)
        assertEquals(3, metadataFile.metadata.folders.size)

        val updatedMetadata = encryptionUtilsV2.addFolderToMetadata(
            EncryptionUtils.generateUid(),
            "new subfolder",
            metadataFile,
            folder,
            storageManager
        )

        assertEquals(2, updatedMetadata.metadata.files.size)
        assertEquals(4, updatedMetadata.metadata.folders.size)
    }

    @Test
    fun removeFolder() {
        val folder = OCFile("/e/")
        val enc1 = MockUser("enc1", "Nextcloud")
        val metadataFile = generateDecryptedFolderMetadataFile(enc1, enc1Cert)
        assertEquals(2, metadataFile.metadata.files.size)
        assertEquals(3, metadataFile.metadata.folders.size)

        val encryptedFileName = EncryptionUtils.generateUid()
        var updatedMetadata = encryptionUtilsV2.addFolderToMetadata(
            encryptedFileName,
            "new subfolder",
            metadataFile,
            folder,
            storageManager
        )

        assertEquals(2, updatedMetadata.metadata.files.size)
        assertEquals(4, updatedMetadata.metadata.folders.size)

        updatedMetadata = encryptionUtilsV2.removeFolderFromMetadata(
            encryptedFileName,
            updatedMetadata
        )

        assertEquals(2, updatedMetadata.metadata.files.size)
        assertEquals(3, updatedMetadata.metadata.folders.size)
    }

    @Test
    fun verifyMetadata() {
        val folder = OCFile("/e/")
        val enc1 = MockUser("enc1", "Nextcloud")
        val metadataFile = generateDecryptedFolderMetadataFile(enc1, enc1Cert)
        val encrypted = encryptionUtilsV2.encryptFolderMetadataFile(
            metadataFile,
            enc1UserId,
            folder,
            storageManager,
            client,
            enc1PrivateKey,
            user,
            targetContext,
            arbitraryDataProvider
        )

        val signature = encryptionUtilsV2.getMessageSignature(enc1Cert, enc1PrivateKey, encrypted)

        encryptionUtilsV2.verifyMetadata(encrypted, metadataFile, 0, signature)

        assertTrue(true) // if we reach this, test is successful
    }

    private fun generateDecryptedFileV1(): com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFile {
        return com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFile().apply {
            encrypted = Data().apply {
                key = EncryptionUtils.generateKeyString()
                filename = "Random filename.jpg"
                mimetype = MimeType.JPEG
                version = 1.0
            }
            initializationVector = EncryptionUtils.generateKeyString()
            authenticationTag = EncryptionUtils.generateKeyString()
        }
    }

    @Test
    fun testMigrateDecryptedV1ToV2() {
        val v1 = generateDecryptedFileV1()
        val v2 = encryptionUtilsV2.migrateDecryptedFileV1ToV2(v1)

        assertEquals(v1.encrypted.filename, v2.filename)
        assertEquals(v1.encrypted.mimetype, v2.mimetype)
        assertEquals(v1.authenticationTag, v2.authenticationTag)
        assertEquals(v1.initializationVector, v2.nonce)
        assertEquals(v1.encrypted.key, v2.key)
    }

    @Test
    fun testMigrateMetadataV1ToV2() {
        OCFile("/").apply {
            storageManager.saveFile(this)
        }

        val folder = OCFile("/enc/").apply {
            parentId = storageManager.getFileByDecryptedRemotePath("/")?.fileId ?: throw IllegalStateException()
        }

        val v1 = DecryptedFolderMetadataFileV1().apply {
            metadata = com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedMetadata().apply {
                metadataKeys = mapOf(Pair(0, EncryptionUtils.generateKeyString()))
            }
            files = mapOf(
                Pair(EncryptionUtils.generateUid(), generateDecryptedFileV1()),
                Pair(EncryptionUtils.generateUid(), generateDecryptedFileV1()),
                Pair(
                    EncryptionUtils.generateUid(),
                    com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFile().apply {
                        encrypted = Data().apply {
                            key = EncryptionUtils.generateKeyString()
                            filename = "subFolder"
                            mimetype = MimeType.WEBDAV_FOLDER
                        }
                        initializationVector = EncryptionUtils.generateKeyString()
                        authenticationTag = null
                    }
                )
            )
        }
        val v2 = encryptionUtilsV2.migrateV1ToV2(
            v1,
            enc1UserId,
            enc1Cert,
            folder,
            storageManager
        )

        assertEquals(2, v2.metadata.files.size)
        assertEquals(1, v2.metadata.folders.size)
        assertEquals(1, v2.users.size) // only one user upon migration
    }

    @Throws(com.owncloud.android.operations.UploadException::class, Throwable::class)
    @Test
    fun addSharee() {
        val enc1 = MockUser("enc1", "Nextcloud")
        val enc2 = MockUser("enc2", "Nextcloud")

        val root = OCFile("/")
        storageManager.saveFile(root)

        val folder = OCFile("/enc/").apply {
            parentId = storageManager.getFileByDecryptedRemotePath("/")?.fileId ?: throw IllegalStateException()
        }

        var metadataFile = generateDecryptedFolderMetadataFile(enc1, enc1Cert)

        metadataFile = encryptionUtilsV2.addShareeToMetadata(metadataFile, enc2.accountName, enc2Cert)

        val encryptedMetadataFile = encryptionUtilsV2.encryptFolderMetadataFile(
            metadataFile,
            client.userId,
            folder,
            storageManager,
            client,
            enc1PrivateKey,
            user,
            targetContext,
            arbitraryDataProvider
        )

        val signature = encryptionUtilsV2.getMessageSignature(enc1Cert, enc1PrivateKey, encryptedMetadataFile)

        val decryptedByEnc1 = encryptionUtilsV2.decryptFolderMetadataFile(
            encryptedMetadataFile,
            enc1.accountName,
            enc1PrivateKey,
            folder,
            storageManager,
            client,
            metadataFile.metadata.counter,
            signature,
            user,
            targetContext,
            arbitraryDataProvider
        )
        assertEquals(metadataFile.metadata, decryptedByEnc1.metadata)

        val decryptedByEnc2 = encryptionUtilsV2.decryptFolderMetadataFile(
            encryptedMetadataFile,
            enc2.accountName,
            enc2PrivateKey,
            folder,
            storageManager,
            client,
            metadataFile.metadata.counter,
            signature,
            user,
            targetContext,
            arbitraryDataProvider
        )
        assertEquals(metadataFile.metadata, decryptedByEnc2.metadata)
    }

    @Test
    fun removeSharee() {
        val enc1 = MockUser("enc1", "Nextcloud")
        val enc2 = MockUser("enc2", "Nextcloud")
        var metadataFile = generateDecryptedFolderMetadataFile(enc1, enc1Cert)
        metadataFile = encryptionUtilsV2.addShareeToMetadata(metadataFile, enc2.accountName, enc2Cert)

        assertEquals(2, metadataFile.users.size)

        metadataFile = encryptionUtilsV2.removeShareeFromMetadata(metadataFile, enc2.accountName)

        assertEquals(1, metadataFile.users.size)
    }

    private fun generateDecryptedFolderMetadataFile(user: User, cert: String): DecryptedFolderMetadataFile {
        val metadata = DecryptedMetadata(
            mutableListOf("hash1", "hash of key 2"),
            false,
            1,
            mutableMapOf(
                Pair(EncryptionUtils.generateUid(), "Folder 1"),
                Pair(EncryptionUtils.generateUid(), "Folder 2"),
                Pair(EncryptionUtils.generateUid(), "Folder 3")
            ),
            mutableMapOf(
                Pair(
                    EncryptionUtils.generateUid(),
                    DecryptedFile(
                        "file 1.png",
                        "image/png",
                        "initializationVector",
                        "authenticationTag",
                        "key 1"
                    )
                ),
                Pair(
                    EncryptionUtils.generateUid(),
                    DecryptedFile(
                        "file 2.png",
                        "image/png",
                        "initializationVector 2",
                        "authenticationTag 2",
                        "key 2"
                    )
                )
            ),
            EncryptionUtils.generateKey()
        )

        val users = mutableListOf(
            DecryptedUser(user.accountName, cert)
        )

        metadata.keyChecksums.add(encryptionUtilsV2.hashMetadataKey(metadata.metadataKey))

        return DecryptedFolderMetadataFile(metadata, users, mutableMapOf())
    }

    @Test
    fun testGZip() {
        val string = """
            This is a test.
            This is a test.
            This is a test.
            This is a test.
            This is a test.
            This is a test.
            This is a test.
            This is a test.
            This is a test.
            This is a test.
            This is a test.
            This is a test.
            This is a test.
            It contains linewraps and special characters:
            $$|²›³¥!’‘‘

        """.trimIndent()

        val gzipped = encryptionUtilsV2.gZipCompress(string)

        val result = encryptionUtilsV2.gZipDecompress(gzipped)

        assertEquals(string, result)
    }

    @Test
    fun gunzip() {
        val string = "H4sICNVkD2QAAwArycgsVgCiRIWS1OISPQDD9wZODwAAAA=="
        val decoded = EncryptionUtils.decodeStringToBase64Bytes(string)
        val gunzip = encryptionUtilsV2.gZipDecompress(decoded)

        assertEquals("this is a test.\n", gunzip)
    }

//     @Test
//     fun validate() {
//         // ALEX
//         val metadata1 = """{
// "metadata": {
// "authenticationTag": "zMozev5R09UopLrq7Je1lw==",
// "ciphertext": "j0OBtUrEt4IveGiexjmGK7eKEaWrY70ZkteA5KxHDaZT/t2wwGy9j2FPQGpqXnW6OO3iAYPNgwFikI1smnfNvqdxzVDvhavl/IXa9Kg2niWyqK3D9zpz0YD6mDvl0XsOgTNVyGXNVREdWgzGEERCQoyHI1xowt/swe3KCXw+lf+XPF/t1PfHv0DiDVk70AeWGpPPPu6yggAIxB4Az6PEZhaQWweTC0an48l2FHj2MtB2PiMHtW2v7RMuE8Al3PtE4gOA8CMFrB+Npy6rKcFCXOgTZm5bp7q+J1qkhBDbiBYtvdsYujJ52Xa5SifTpEhGeWWLFnLLgPAQ8o6bXcWOyCoYfLfp4Jpft/Y7H8qzHbPewNSyD6maEv+xljjfU7hxibbszz5A4JjMdQy2BDGoTmJx7Mas+g6l6ZuHLVbdmgQOvD3waJBy6rOg0euux0Cn4bB4bIFEF2KvbhdGbY1Uiq9DYa7kEmSEnlcAYaHyroTkDg4ew7ER0vIBBMzKM3r+UdPVKKS66uyXtZc=",
// "nonce": "W+lxQJeGq7XAJiGfcDohkg=="
// },
// "users": [{
// "certificate": "-----BEGIN CERTIFICATE-----\nMIIDkDCCAnigAwIBAgIBADANBgkqhkiG9w0BAQUFADBhMQswCQYDVQQGEwJERTEb\nMBkGA1UECAwSQmFkZW4tV3VlcnR0ZW1iZXJnMRIwEAYDVQQHDAlTdHV0dGdhcnQx\nEjAQBgNVBAoMCU5leHRjbG91ZDENMAsGA1UEAwwEam9objAeFw0yMzA3MTQwNzM0\nNTZaFw00MzA3MDkwNzM0NTZaMGExCzAJBgNVBAYTAkRFMRswGQYDVQQIDBJCYWRl\nbi1XdWVydHRlbWJlcmcxEjAQBgNVBAcMCVN0dXR0Z2FydDESMBAGA1UECgwJTmV4\ndGNsb3VkMQ0wCwYDVQQDDARqb2huMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIB\nCgKCAQEA7j3Er5YahJT0LAnSRLhpqbRo+E1AVnt98rvp3DmEfBHNzNB+DS9IBDkS\nSXM/YtfAci6Tcw8ujVBjrZX/WEmrf8ynQHxYmSaJSnP8uAT306/MceZpdpruEc9/\nS10a7vp54Zbld4NYdmfS71oVFVKgM7c/Vthx+rgu48fuxzbWAvVYLFcx47hz0DJT\nnjz2Za/R68uXpxfz7J9uEXYiqsAs/FobDsLZluT3RyywVRwKBed1EZxUeLIJiyxp\nUthhGfIb8b3Vf9jZoUVi3m5gmc4spJQHvYAkfZYHzd9ras8jBu1abQRxcu2CYnVo\n6Y0mTxhKhQS/n5gjv3ExiQF3wp/XYwIDAQABo1MwUTAdBgNVHQ4EFgQUmTeILVuB\ntv70fTGkXWGAueDp5kAwHwYDVR0jBBgwFoAUmTeILVuBtv70fTGkXWGAueDp5kAw\nDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQUFAAOCAQEAyVtq9XAvW7nxSW/8\nhp30z6xbzGiuviXhy/Jo91VEa8IRsWCCn3OmDFiVduTEowx76tf8clJP0gk7Pozi\n6dg/7Fin+FqQGXfCk8bLAh9gXKAikQ2GK8yRN3slRFwYC2mm23HrLdKXZHUqJcpB\nMz2zsSrOGPj1YsYOl/U8FU6KA7Yj7U3q7kDMYTAgzUPZAH+d1DISGWpZsMa0RYid\nvigCCLByiccmS/Co4Sb1esF58H+YtV5+nFBRwx881U2g2TgDKF1lPMK/y3d8B8mh\nUtW+lFxRpvyNUDpsMjOErOrtNFEYbgoUJLtqwBMmyGR+nmmh6xna331QWcRAmw0P\nnDO4ew==\n-----END CERTIFICATE-----\n",
// "encryptedMetadataKey": "HVT49bYmwXbGs/dJ2avgU9unrKnPf03MYUI5ZysSR1Bz5pqz64gzH2GBAuUJ+Q4VmHtEfcMaWW7VXgzfCQv5xLBrk+RSgcLOKnlIya8jaDlfttWxbe8jJK+/0+QVPOc6ycA/t5HNCPg09hzj+gnb2L89UHxL5accZD0iEzb5cQbGrc/N6GthjgGrgFKtFf0HhDVplUr+DL9aTyKuKLBPjrjuZbv8M6ZfXO93mOMwSZH3c3rwDUHb/KEaTR/Og4pWQmrqr1VxGLqeV/+GKWhzMYThrOZAUz+5gsbckU2M5V9i+ph0yBI5BjOZVhNuDwW8yP8WtyRJwQc+UBRei/RGBQ==",
// "userId": "john"
// }],
// "version": "2"
// }
//
// """
//
//         val signature1 =
//             "ewogICAgIm1ldGFkYXRhIjogewogICAgICAgICJhdXRoZW50aWNhdGlvblRhZyI6ICJ6TW96ZXY1UjA5VW9wTHJxN0plMWx3PT0iLAogICAgICAgICJjaXBoZXJ0ZXh0IjogImowT0J0VXJFdDRJdmVHaWV4am1HSzdlS0VhV3JZNzBaa3RlQTVLeEhEYVpUL3Qyd3dHeTlqMkZQUUdwcVhuVzZPTzNpQVlQTmd3RmlrSTFzbW5mTnZxZHh6VkR2aGF2bC9JWGE5S2cybmlXeXFLM0Q5enB6MFlENm1EdmwwWHNPZ1ROVnlHWE5WUkVkV2d6R0VFUkNRb3lISTF4b3d0L3N3ZTNLQ1h3K2xmK1hQRi90MVBmSHYwRGlEVms3MEFlV0dwUFBQdTZ5Z2dBSXhCNEF6NlBFWmhhUVd3ZVRDMGFuNDhsMkZIajJNdEIyUGlNSHRXMnY3Uk11RThBbDNQdEU0Z09BOENNRnJCK05weTZyS2NGQ1hPZ1RabTVicDdxK0oxcWtoQkRiaUJZdHZkc1l1ako1MlhhNVNpZlRwRWhHZVdXTEZuTExnUEFROG82YlhjV095Q29ZZkxmcDRKcGZ0L1k3SDhxekhiUGV3TlN5RDZtYUV2K3hsampmVTdoeGliYnN6ejVBNEpqTWRReTJCREdvVG1KeDdNYXMrZzZsNlp1SExWYmRtZ1FPdkQzd2FKQnk2ck9nMGV1dXgwQ240YkI0YklGRUYyS3ZiaGRHYlkxVWlxOURZYTdrRW1TRW5sY0FZYUh5cm9Ua0RnNGV3N0VSMHZJQkJNektNM3IrVWRQVktLUzY2dXlYdFpjPSIsCiAgICAgICAgIm5vbmNlIjogIlcrbHhRSmVHcTdYQUppR2ZjRG9oa2c9PSIKICAgIH0sCiAgICAidXNlcnMiOiB7CiAgICAgICAgImNlcnRpZmljYXRlIjogIi0tLS0tQkVHSU4gQ0VSVElGSUNBVEUtLS0tLVxuTUlJRGtEQ0NBbmlnQXdJQkFnSUJBREFOQmdrcWhraUc5dzBCQVFVRkFEQmhNUXN3Q1FZRFZRUUdFd0pFUlRFYlxuTUJrR0ExVUVDQXdTUW1Ga1pXNHRWM1ZsY25SMFpXMWlaWEpuTVJJd0VBWURWUVFIREFsVGRIVjBkR2RoY25ReFxuRWpBUUJnTlZCQW9NQ1U1bGVIUmpiRzkxWkRFTk1Bc0dBMVVFQXd3RWFtOW9iakFlRncweU16QTNNVFF3TnpNMFxuTlRaYUZ3MDBNekEzTURrd056TTBOVFphTUdFeEN6QUpCZ05WQkFZVEFrUkZNUnN3R1FZRFZRUUlEQkpDWVdSbFxuYmkxWGRXVnlkSFJsYldKbGNtY3hFakFRQmdOVkJBY01DVk4wZFhSMFoyRnlkREVTTUJBR0ExVUVDZ3dKVG1WNFxuZEdOc2IzVmtNUTB3Q3dZRFZRUUREQVJxYjJodU1JSUJJakFOQmdrcWhraUc5dzBCQVFFRkFBT0NBUThBTUlJQlxuQ2dLQ0FRRUE3ajNFcjVZYWhKVDBMQW5TUkxocHFiUm8rRTFBVm50OThydnAzRG1FZkJITnpOQitEUzlJQkRrU1xuU1hNL1l0ZkFjaTZUY3c4dWpWQmpyWlgvV0VtcmY4eW5RSHhZbVNhSlNuUDh1QVQzMDYvTWNlWnBkcHJ1RWM5L1xuUzEwYTd2cDU0WmJsZDROWWRtZlM3MW9WRlZLZ003Yy9WdGh4K3JndTQ4ZnV4emJXQXZWWUxGY3g0N2h6MERKVFxubmp6MlphL1I2OHVYcHhmejdKOXVFWFlpcXNBcy9Gb2JEc0xabHVUM1J5eXdWUndLQmVkMUVaeFVlTElKaXl4cFxuVXRoaEdmSWI4YjNWZjlqWm9VVmkzbTVnbWM0c3BKUUh2WUFrZlpZSHpkOXJhczhqQnUxYWJRUnhjdTJDWW5Wb1xuNlkwbVR4aEtoUVMvbjVnanYzRXhpUUYzd3AvWFl3SURBUUFCbzFNd1VUQWRCZ05WSFE0RUZnUVVtVGVJTFZ1QlxudHY3MGZUR2tYV0dBdWVEcDVrQXdId1lEVlIwakJCZ3dGb0FVbVRlSUxWdUJ0djcwZlRHa1hXR0F1ZURwNWtBd1xuRHdZRFZSMFRBUUgvQkFVd0F3RUIvekFOQmdrcWhraUc5dzBCQVFVRkFBT0NBUUVBeVZ0cTlYQXZXN254U1cvOFxuaHAzMHo2eGJ6R2l1dmlYaHkvSm85MVZFYThJUnNXQ0NuM09tREZpVmR1VEVvd3g3NnRmOGNsSlAwZ2s3UG96aVxuNmRnLzdGaW4rRnFRR1hmQ2s4YkxBaDlnWEtBaWtRMkdLOHlSTjNzbFJGd1lDMm1tMjNIckxkS1haSFVxSmNwQlxuTXoyenNTck9HUGoxWXNZT2wvVThGVTZLQTdZajdVM3E3a0RNWVRBZ3pVUFpBSCtkMURJU0dXcFpzTWEwUllpZFxudmlnQ0NMQnlpY2NtUy9DbzRTYjFlc0Y1OEgrWXRWNStuRkJSd3g4ODFVMmcyVGdES0YxbFBNSy95M2Q4QjhtaFxuVXRXK2xGeFJwdnlOVURwc01qT0VyT3J0TkZFWWJnb1VKTHRxd0JNbXlHUitubW1oNnhuYTMzMVFXY1JBbXcwUFxubkRPNGV3PT1cbi0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS1cbiIsCiAgICAgICAgImVuY3J5cHRlZE1ldGFkYXRhS2V5IjogIkhWVDQ5Ylltd1hiR3MvZEoyYXZnVTl1bnJLblBmMDNNWVVJNVp5c1NSMUJ6NXBxejY0Z3pIMkdCQXVVSitRNFZtSHRFZmNNYVdXN1ZYZ3pmQ1F2NXhMQnJrK1JTZ2NMT0tubEl5YThqYURsZnR0V3hiZThqSksrLzArUVZQT2M2eWNBL3Q1SE5DUGcwOWh6aitnbmIyTDg5VUh4TDVhY2NaRDBpRXpiNWNRYkdyYy9ONkd0aGpnR3JnRkt0RmYwSGhEVnBsVXIrREw5YVR5S3VLTEJQanJqdVpidjhNNlpmWE85M21PTXdTWkgzYzNyd0RVSGIvS0VhVFIvT2c0cFdRbXJxcjFWeEdMcWVWLytHS1doek1ZVGhyT1pBVXorNWdzYmNrVTJNNVY5aStwaDB5Qkk1QmpPWlZoTnVEd1c4eVA4V3R5Ukp3UWMrVUJSZWkvUkdCUT09IiwKICAgICAgICAidXNlcklkIjogImpvaG4iCiAgICB9LAogICAgInZlcnNpb24iOiAiMiIKfQo="
//
//         // TOBI
//         val metadata =
//             """{"metadata":{"authenticationTag":"qDcJnAAGtGDlHWiQMBfXgw\u003d\u003d","ciphertext":"3zUhwIgJWMB7DvrbsDaMvh8MbJdoTxL0OMPCCdYSfBt7gB+V/hwqelL1IOaLto3avhHGSebnrotF06iEP/jZwWg9hApIPTHc8B4XTOY0/kezqYyVqTyquTUZpDpqgVAheQskZZ8I4Ir0seajUkt4KtVRfzO6v8CePRrEg6uKwdYsqDcJnAAGtGDlHWiQMBfXgw\u003d\u003d|4hbOyn1ykQL+9D6SnPY3cQ\u003d\u003d","nonce":"4hbOyn1ykQL+9D6SnPY3cQ\u003d\u003d"},"users":[{"certificate":"-----BEGIN CERTIFICATE-----\nMIIC6DCCAdCgAwIBAgIBADANBgkqhkiG9w0BAQUFADANMQswCQYDVQQDDAJ0MTAe\nFw0yMzA3MjUwNzU3MTJaFw00MzA3MjAwNzU3MTJaMA0xCzAJBgNVBAMMAnQxMIIB\nIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtafHmDBcBqIu4HmMxMDW3j0S\ny+S0YaKwHnBRt85KSwcEov0B5FOLuLknoBGx4Dn3u93ilThXXxacPMHeXL7WPuAs\n21/G7vsqwvrRRnCduf+FUO/AZeDCNErzpsQ8LmTa4PUloLPUcImpSjrHwhMs9Ekv\nEbLRjbeSmSp9XvM+1fV/3jkT5jkOSnCFx5TGwGN5uHqwUir4UWXasvg253NK2XmW\nipKCDCR9TmH1baP3pNdoiChdmErT1c6E4DbBXpTw8XgP5ZbYH+qg1UQ/hC8nRJ3D\nyCcHL+dg/GYraBMhDn4w2Vvq77xNNoNWQ9cT5Ay6cJbQLBQoJQirygQFrobYRQID\nAQABo1MwUTAdBgNVHQ4EFgQUE9zCeA9/QMAtVgLxD23X6ZcodhMwHwYDVR0jBBgw\nFoAUE9zCeA9/QMAtVgLxD23X6ZcodhMwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG\n9w0BAQUFAAOCAQEAZdy/YjJlvnz3FQwxp6oVtMJccpdxveEPfLzgaverhtd/vP8O\nAvDzOLgQJHmrDS91SG503eU4cYGyuNKwd77OyTnqMg+GUEmJhGfPpSVrEIdh65jv\nq61T4oqBdehevVmBq54rGiwL0DGv1DlXQlwiJZP4qni2KnOEFcnvL3gVtRnQjXQ+\nkHvlMshkK6w021EMV5NfjG2zg67wC65rLaej5f6Ssp2S7g2VtmE4aXq1bjAuEbqk\n4TiyZHLDdsJuqzyGyyOpMV7i9ucXDoaZt9cGS9hT2vRxTrSH63vKR8Xeig9+stLw\nt9ONcUqCKP7hd8rajtxM4JIIRExwD8OkgARWGg\u003d\u003d\n-----END CERTIFICATE-----\n","encryptedMetadataKey":"s4kDkkLpk1mSmXedP7huiCNC4DYmDAmA2VYGem5M8jIGPC6miVQoo4WXZrEBhdsLw7Msf5iT3A3fTaHhwsI8Jf4McsFyM9/FXT1mCEaGOEpNjbKOlJY1uPUFNOhLqUfFiBos6oBT53hWwoXWjytYvLBbXuXY5YLOysjgBh6URrgFUZAJAmcOJ6OFKgfIIthoqkQc7CQUY97VsRzAXzeYTANBc2yW1pSN51HqftvMzvewFRsJQLcu7a9NjpTdG9LiLhn5eLXOLymXEE/aaPHKXeprlXLzrdWU1xwZRJqV+to2FEiH6CQNsO4+9h5m0VjXekiNeAFrsXB5cJgUipGuzQ\u003d\u003d","userId":"t1"}],"version":"2.0"}"""
//
//         val base = EncryptionUtils.encodeStringToBase64String(metadata)
//
//         val signature =
//             "MIAGCSqGSIb3DQEHAqCAMIACAQExDTALBglghkgBZQMEAgEwCwYJKoZIhvcNAQcBoIAwggLoMIIB0KADAgECAgEAMA0GCSqGSIb3DQEBBQUAMA0xCzAJBgNVBAMMAnQxMB4XDTIzMDcyNTA3NTcxMloXDTQzMDcyMDA3NTcxMlowDTELMAkGA1UEAwwCdDEwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQC1p8eYMFwGoi7geYzEwNbePRLL5LRhorAecFG3zkpLBwSi/QHkU4u4uSegEbHgOfe73eKVOFdfFpw8wd5cvtY+4CzbX8bu+yrC+tFGcJ25/4VQ78Bl4MI0SvOmxDwuZNrg9SWgs9RwialKOsfCEyz0SS8RstGNt5KZKn1e8z7V9X/eORPmOQ5KcIXHlMbAY3m4erBSKvhRZdqy+Dbnc0rZeZaKkoIMJH1OYfVto/ek12iIKF2YStPVzoTgNsFelPDxeA/lltgf6qDVRD+ELydEncPIJwcv52D8ZitoEyEOfjDZW+rvvE02g1ZD1xPkDLpwltAsFCglCKvKBAWuhthFAgMBAAGjUzBRMB0GA1UdDgQWBBQT3MJ4D39AwC1WAvEPbdfplyh2EzAfBgNVHSMEGDAWgBQT3MJ4D39AwC1WAvEPbdfplyh2EzAPBgNVHRMBAf8EBTADAQH/MA0GCSqGSIb3DQEBBQUAA4IBAQBl3L9iMmW+fPcVDDGnqhW0wlxyl3G94Q98vOBq96uG13+8/w4C8PM4uBAkeasNL3VIbnTd5ThxgbK40rB3vs7JOeoyD4ZQSYmEZ8+lJWsQh2HrmO+rrVPiioF16F69WYGrnisaLAvQMa/UOVdCXCIlk/iqeLYqc4QVye8veBW1GdCNdD6Qe+UyyGQrrDTbUQxXk1+MbbODrvALrmstp6Pl/pKynZLuDZW2YThperVuMC4RuqThOLJkcsN2wm6rPIbLI6kxXuL25xcOhpm31wZL2FPa9HFOtIfre8pHxd6KD36y0vC3041xSoIo/uF3ytqO3EzgkghETHAPw6SABFYaAAAxggHUMIIB0AIBATASMA0xCzAJBgNVBAMMAnQxAgEAMAsGCWCGSAFlAwQCAaCBljAYBgkqhkiG9w0BCQMxCwYJKoZIhvcNAQcBMBwGCSqGSIb3DQEJBTEPFw0yMzA3MjgwNzMwMTJaMCsGCSqGSIb3DQEJNDEeMBwwCwYJYIZIAWUDBAIBoQ0GCSqGSIb3DQEBCwUAMC8GCSqGSIb3DQEJBDEiBCAx7RTJg7hbY5Mkzjw3f6qhX7k/J0FdVz2cL3ow0AmyYjANBgkqhkiG9w0BAQsFAASCAQAbUmb9e7eoIcPNzDSmnzbrueBzgT8YszNGEI+1YCq8XdWN4kDztvP1ZNV21VCO6BvcbfUAnXXgcX5BPeLZNsgXPj3c8TbD59GQl3oT/tIchgMsA20RdAtIwvItlZKh+X6sp0OHkRPYSk/mEYKCKPqrKdJicRWex8ItCwpDR91KSOiKJrN/+DKOGG0sVI9gjzbtrHsN8HmVKxOoNV+wwipcLsWsEmuV+wvPCQ9HJidLX9Q17Bgfc+qJg19aB6iKLWPhjgnfpKGbK5VJuQTdDWPUJ2O4G3W/iwxJ0hAJ7tks4zIATmgGzhgTWYx5LVXbKcuL04xhIOjqwedHeCSBZSSaAAAAAAAA"
//
//         val metadataFile = EncryptionUtils.deserializeJSON(
//             metadata,
//             object : TypeToken<EncryptedFolderMetadataFile>() {}
//         )
//         assertNotNull(metadataFile)
//
//         val certJohnString = metadataFile.users[0].certificate
//         val certJohn = EncryptionUtils.convertCertFromString(certJohnString)
//
//         val t1String = """-----BEGIN CERTIFICATE-----
// MIIC6DCCAdCgAwIBAgIBADANBgkqhkiG9w0BAQUFADANMQswCQYDVQQDDAJ0MTAe
// Fw0yMzA3MjUwNzU3MTJaFw00MzA3MjAwNzU3MTJaMA0xCzAJBgNVBAMMAnQxMIIB
// IjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtafHmDBcBqIu4HmMxMDW3j0S
// y+S0YaKwHnBRt85KSwcEov0B5FOLuLknoBGx4Dn3u93ilThXXxacPMHeXL7WPuAs
// 21/G7vsqwvrRRnCduf+FUO/AZeDCNErzpsQ8LmTa4PUloLPUcImpSjrHwhMs9Ekv
// EbLRjbeSmSp9XvM+1fV/3jkT5jkOSnCFx5TGwGN5uHqwUir4UWXasvg253NK2XmW
// ipKCDCR9TmH1baP3pNdoiChdmErT1c6E4DbBXpTw8XgP5ZbYH+qg1UQ/hC8nRJ3D
// yCcHL+dg/GYraBMhDn4w2Vvq77xNNoNWQ9cT5Ay6cJbQLBQoJQirygQFrobYRQID
// AQABo1MwUTAdBgNVHQ4EFgQUE9zCeA9/QMAtVgLxD23X6ZcodhMwHwYDVR0jBBgw
// FoAUE9zCeA9/QMAtVgLxD23X6ZcodhMwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG
// 9w0BAQUFAAOCAQEAZdy/YjJlvnz3FQwxp6oVtMJccpdxveEPfLzgaverhtd/vP8O
// AvDzOLgQJHmrDS91SG503eU4cYGyuNKwd77OyTnqMg+GUEmJhGfPpSVrEIdh65jv
// q61T4oqBdehevVmBq54rGiwL0DGv1DlXQlwiJZP4qni2KnOEFcnvL3gVtRnQjXQ+
// kHvlMshkK6w021EMV5NfjG2zg67wC65rLaej5f6Ssp2S7g2VtmE4aXq1bjAuEbqk
// 4TiyZHLDdsJuqzyGyyOpMV7i9ucXDoaZt9cGS9hT2vRxTrSH63vKR8Xeig9+stLw
// t9ONcUqCKP7hd8rajtxM4JIIRExwD8OkgARWGg==
// -----END CERTIFICATE-----"""
//
//         val t1cert = EncryptionUtils.convertCertFromString(t1String)
//         val t1PrivateKeyKey = EncryptionUtils.PEMtoPrivateKey(encryptionTestUtils.t1PrivateKey)
//
//         // val signed = encryptionUtilsV2.getMessageSignature(
//         //     t1cert,
//         //     t1PrivateKeyKey,
//         //     metadataFile
//         // )
//
//         assertTrue(encryptionUtilsV2.verifySignedMessage(signature1, metadata1, listOf(certJohn, t1cert)))
//     }

    @Throws(Throwable::class)
    @Test
    fun testSigning() {
        val metadata =
            """{"metadata": {"authenticationTag": "zMozev5R09UopLrq7Je1lw==","ciphertext": "j0OBtUrEt4IveGiexjm
                |GK7eKEaWrY70ZkteA5KxHDaZT/t2wwGy9j2FPQGpqXnW6OO3iAYPNgwFikI1smnfNvqdxzVDvhavl/IXa9Kg2niWyqK3D9
                |zpz0YD6mDvl0XsOgTNVyGXNVREdWgzGEERCQoyHI1xowt/swe3KCXw+lf+XPF/t1PfHv0DiDVk70AeWGpPPPu6yggAIxB4
                |Az6PEZhaQWweTC0an48l2FHj2MtB2PiMHtW2v7RMuE8Al3PtE4gOA8CMFrB+Npy6rKcFCXOgTZm5bp7q+J1qkhBDbiBYtv
                |dsYujJ52Xa5SifTpEhGeWWLFnLLgPAQ8o6bXcWOyCoYfLfp4Jpft/Y7H8qzHbPewNSyD6maEv+xljjfU7hxibbszz5A4Jj
                |MdQy2BDGoTmJx7Mas+g6l6ZuHLVbdmgQOvD3waJBy6rOg0euux0Cn4bB4bIFEF2KvbhdGbY1Uiq9DYa7kEmSEnlcAYaHyr
                |oTkDg4ew7ER0vIBBMzKM3r+UdPVKKS66uyXtZc=","nonce": "W+lxQJeGq7XAJiGfcDohkg=="},"users": [{"cert
                |ificate": "-----BEGIN CERTIFICATE-----\nMIIDkDCCAnigAwIBAgIBADANBgkqhkiG9w0BAQUFADBhMQswCQYDVQ
                |QGEwJERTEb\nMBkGA1UECAwSQmFkZW4tV3VlcnR0ZW1iZXJnMRIwEAYDVQQHDAlTdHV0dGdhcnQx\nEjAQBgNVBAoMCU5l
                |eHRjbG91ZDENMAsGA1UEAwwEam9objAeFw0yMzA3MTQwNzM0\nNTZaFw00MzA3MDkwNzM0NTZaMGExCzAJBgNVBAYTAkRF
                |MRswGQYDVQQIDBJCYWRl\nbi1XdWVydHRlbWJlcmcxEjAQBgNVBAcMCVN0dXR0Z2FydDESMBAGA1UECgwJTmV4\ndGNsb3
                |VkMQ0wCwYDVQQDDARqb2huMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIB\nCgKCAQEA7j3Er5YahJT0LAnSRLhpqbRo+E
                |1AVnt98rvp3DmEfBHNzNB+DS9IBDkS\nSXM/YtfAci6Tcw8ujVBjrZX/WEmrf8ynQHxYmSaJSnP8uAT306/MceZpdpruEc
                |9/\nS10a7vp54Zbld4NYdmfS71oVFVKgM7c/Vthx+rgu48fuxzbWAvVYLFcx47hz0DJT\nnjz2Za/R68uXpxfz7J9uEXYi
                |qsAs/FobDsLZluT3RyywVRwKBed1EZxUeLIJiyxp\nUthhGfIb8b3Vf9jZoUVi3m5gmc4spJQHvYAkfZYHzd9ras8jBu1a
                |bQRxcu2CYnVo\n6Y0mTxhKhQS/n5gjv3ExiQF3wp/XYwIDAQABo1MwUTAdBgNVHQ4EFgQUmTeILVuB\ntv70fTGkXWGAue
                |Dp5kAwHwYDVR0jBBgwFoAUmTeILVuBtv70fTGkXWGAueDp5kAw\nDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQUFAA
                |OCAQEAyVtq9XAvW7nxSW/8\nhp30z6xbzGiuviXhy/Jo91VEa8IRsWCCn3OmDFiVduTEowx76tf8clJP0gk7Pozi\n6dg/
                |7Fin+FqQGXfCk8bLAh9gXKAikQ2GK8yRN3slRFwYC2mm23HrLdKXZHUqJcpB\nMz2zsSrOGPj1YsYOl/U8FU6KA7Yj7U3q
                |7kDMYTAgzUPZAH+d1DISGWpZsMa0RYid\nvigCCLByiccmS/Co4Sb1esF58H+YtV5+nFBRwx881U2g2TgDKF1lPMK/y3d8
                |B8mh\nUtW+lFxRpvyNUDpsMjOErOrtNFEYbgoUJLtqwBMmyGR+nmmh6xna331QWcRAmw0P\nnDO4ew==\n-----END CER
                |TIFICATE-----\n","encryptedMetadataKey": "HVT49bYmwXbGs/dJ2avgU9unrKnPf03MYUI5ZysSR1Bz5pqz64gz
                |H2GBAuUJ+Q4VmHtEfcMaWW7VXgzfCQv5xLBrk+RSgcLOKnlIya8jaDlfttWxbe8jJK+/0+QVPOc6ycA/t5HNCPg09hzj+g
                |nb2L89UHxL5accZD0iEzb5cQbGrc/N6GthjgGrgFKtFf0HhDVplUr+DL9aTyKuKLBPjrjuZbv8M6ZfXO93mOMwSZH3c3rw
                |DUHb/KEaTR/Og4pWQmrqr1VxGLqeV/+GKWhzMYThrOZAUz+5gsbckU2M5V9i+ph0yBI5BjOZVhNuDwW8yP8WtyRJwQc+UB
                |Rei/RGBQ==","userId": "john"}],"version": "2"}
            """.trimMargin()

        val base64Metadata = EncryptionUtils.encodeStringToBase64String(metadata)

        val privateKey = EncryptionUtils.PEMtoPrivateKey(encryptionTestUtils.t1PrivateKey)
        val certificateT1 = EncryptionUtils.convertCertFromString(encryptionTestUtils.t1PublicKey)
        val certificateEnc2 = EncryptionUtils.convertCertFromString(enc2Cert)

        val signed = encryptionUtilsV2.signMessage(
            certificateT1,
            privateKey,
            metadata
        )

        val base64Ans = encryptionUtilsV2.extractSignedString(signed)

        // verify
        val certs = listOf(
            certificateEnc2,
            certificateT1
        )
        assertTrue(encryptionUtilsV2.verifySignedMessage(signed, certs))
        assertTrue(encryptionUtilsV2.verifySignedMessage(base64Ans, base64Metadata, certs))
    }

    @Throws(Throwable::class)
    @Test
    fun sign() {
        val sut = "randomstring123"
        val json = "randomstring123"
        val jsonBase64 = EncryptionUtils.encodeStringToBase64String(json)

        val privateKey = EncryptionUtils.PEMtoPrivateKey(encryptionTestUtils.t1PrivateKey)
        val certificate = EncryptionUtils.convertCertFromString(encryptionTestUtils.t1PublicKey)

        val signed = encryptionUtilsV2.signMessage(
            certificate,
            privateKey,
            sut
        )

        val base64Ans = encryptionUtilsV2.extractSignedString(signed)

        // verify
        val certs = listOf(
            EncryptionUtils.convertCertFromString(enc2Cert),
            certificate
        )
        assertTrue(encryptionUtilsV2.verifySignedMessage(signed, certs))
        assertTrue(encryptionUtilsV2.verifySignedMessage(base64Ans, jsonBase64, certs))
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateFileNameForEncryptedFile() {
        val folder = testFolder()

        val metadata = EncryptionTestUtils().generateFolderMetadataV2(
            client.userId,
            EncryptionTestIT.publicKey
        )

        RefreshFolderOperation.updateFileNameForEncryptedFile(storageManager, metadata, folder)

        assertEquals(folder.decryptedRemotePath.contains("null"), false)
    }

    /**
     * DecryptedFolderMetadata -> EncryptedFolderMetadata -> JSON -> encrypt -> decrypt -> JSON ->
     * EncryptedFolderMetadata -> DecryptedFolderMetadata
     */
    @Test
    @Throws(Exception::class, Throwable::class)
    fun encryptionMetadataV2() {
        val decryptedFolderMetadata1: DecryptedFolderMetadataFile =
            EncryptionTestUtils().generateFolderMetadataV2(client.userId, EncryptionTestIT.publicKey)
        val root = OCFile("/")
        storageManager.saveFile(root)

        val folder = OCFile("/enc")
        folder.parentId = storageManager.getFileByDecryptedRemotePath("/")?.fileId ?: throw IllegalStateException()

        storageManager.saveFile(folder)

        decryptedFolderMetadata1.filedrop.clear()

        // encrypt
        val encryptedFolderMetadata1 = encryptionUtilsV2.encryptFolderMetadataFile(
            decryptedFolderMetadata1,
            client.userId,
            folder,
            storageManager,
            client,
            EncryptionTestIT.publicKey,
            user,
            targetContext,
            arbitraryDataProvider
        )

        val signature = encryptionUtilsV2.getMessageSignature(enc1Cert, enc1PrivateKey, encryptedFolderMetadata1)

        // serialize
        val encryptedJson = EncryptionUtils.serializeJSON(encryptedFolderMetadata1, true)

        // de-serialize
        val encryptedFolderMetadata2 = EncryptionUtils.deserializeJSON(
            encryptedJson,
            object : TypeToken<EncryptedFolderMetadataFile?>() {}
        )

        // decrypt
        val decryptedFolderMetadata2 = encryptionUtilsV2.decryptFolderMetadataFile(
            encryptedFolderMetadata2!!,
            getUserId(user),
            EncryptionTestIT.privateKey,
            folder,
            fileDataStorageManager,
            client,
            decryptedFolderMetadata1.metadata.counter,
            signature,
            user,
            targetContext,
            arbitraryDataProvider
        )

        // compare
        assertTrue(
            EncryptionTestIT.compareJsonStrings(
                EncryptionUtils.serializeJSON(decryptedFolderMetadata1),
                EncryptionUtils.serializeJSON(decryptedFolderMetadata2)
            )
        )
    }

    @Throws(Throwable::class)
    @Test
    fun decryptFiledropV2() {
        val sut = EncryptedFiledrop(
            """QE5nJmA8QC3rBJxbpsZu6MvkomwHMKTYf/3dEz9Zq3ITHLK/wNAIqWTbDehBJ7SlTfXakkKR9o0sOkUDI7PD8qJyv5hW7LzifszYGe
                |xE0V1daFcCFApKrIEBABHVOq+ZHJd8IzNSz3hdA9bWd2eiaEGyQzgdTPELE6Ie84IwFANJHcaRB5B43aaDdbUXNJ4/oMboOReKTJ
                |/vT6ZGhve4DRPEsez0quyDZDNlin5hD6UaUzw=
            """.trimMargin(),
            "HC87OgVzbR2CXdWp7rKI5A==",
            "7PSq7INkM2WKfmEPpRpTPA==",
            listOf(
                EncryptedFiledropUser(
                    "android3",
                    """cNzk8cNyoTJ49Cj/x2WPlsMAnUWlZsfnKJ3VIRiczASeUYUFhaJpD8HDWE0uhkXSD7i9nzpe6pR7zllE7UE/QniDd+BQiF
                        |80E5fSO1KVfFkLZRT+2pX5oPnl4CVtMnxb4xG7J1nAUqMhfS8PtQIr0+S7NKDdrUc41aNOB/4kH0D9LSo/bSC38L7ewv
                        |mISM6ZFi1bfI1505kZV0HqcW12nZwHwe3s6rYkoSPBOPX1oPkvMYTVLkYuU+7DNL4HW7D9dc9X4bsSGLdj4joRi9FURi
                        |mMv6MOrWOnYlX2zmMKAF3nEjLlhngKG7pUi/qMIlft2AhRM4cJuuIQ29vvTGFFDQ==
                    """.trimMargin()
                )
            )
        )

        val privateKey =
            """MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDPNCnYcPgGQwCzL8sxLTiE0atn5tiW1nfPQc1aY/+aXvkpF4h2vT
                |S/hQg2SCNFUlw8aYKksk5IH5FFcPv9QFG/TQDQOnZhi7moVPnVwLkx+cDfWQQs1EOhI/ZPdSo7MdaRLttbZZs/GJfnr1ziYZTxLO
                |UUxT541cnSpqGTKmUXhGMoX+jQTmcn1NyBD537NdetOxSdMfvBIobjRQ70/9c1HFGQSrJa+DmPiis6iFkd1LH6WWRbreC6DsRSqK
                |ne3sD1ujx39k+VxtBe035c2L9PbTMMW3kBdZxlRkV1tUQhDAys0K+CyvNIFsOjqvQKTnXNfWO+kVnpOkpbTK4imuPbAgMBAAECgf
                |9T537U/6TuwJLSj4bfYev8qYaakfVIpyMkL33e4YBQnUzhlCPBVYgpHkDPwznk2XhjQQiVcRAycmUHBmy4aPkcOjuBmd87aTj03k
                |niDk+doFDNU8myuwWTw/1fHdElRnLyZxEKrb391HD4SVVQMuxnw8UoC4iNcPnYneY/GTiTtB3dVcRKdabX3Oak2TFiJyJBtTz4RN
                |sRYVXM3jyCbxj8uV+XNr+3OuQe5u7cV5gkXOXHqcNczOrxGzSXVGULuw8FiHIlhId7tot3dGdyVvWD9YIwwGA/9/3g8JixqpQHKZ
                |6YJAeqltydisGa3CIIEzBAh52GJC7yzMKSC2ZAtW0CgYEA6B/O+EgtZthiXOwivqZmKKGgWGLSOGjVsExSa1iiTTz3EFwcdD54mU
                |TKc6hw787NFlfN1m7B7EDQxIldRDI3One1q2dj87taco/qFqKsHuAuC3gmZIp2F4l2P8NpdHHFMzUzsfs+grY/wLHZiJdfOTdulA
                |s9go5mDloMC96n0/UCgYEA5IQo7c4ZxwhlssIn89XaOlKGoIct07wsBMu47HZYFqgG2/NUN8zRfSdSvot+6zinAb6Z3iGZ2FBL+C
                |MmoEMGwuXSQjWxeD//UU6V5AZqlgis5s9WakKWmkTkVV3bPSwW0DuNcqbMk7BxAXcQ6QGIiBtzeaPuL/3gzA9e9vm8xo8CgYEAqL
                |I9S6nA/UZzLg8bLS1nf03/Z1ziZMajzk2ZdJRk1/dfow8eSskAAnvBGo8nDNFhsUQ8vwOdgeKVFtCx7JcGFkLbz+cC+CaIFExNFw
                |hASOwp6oH2fQk3y+FGBA8ze8IXTCD1IftzMbHb4WIfsyo3tTB497S3jkOJHhMJQDMgC2UCgYEAzjUgRe98vWkrdFLWAKfSxFxiFg
                |vF49JjGnTHy8HDHbbEccizD6NoyvooJb/1aMd3lRBtAtDpZhSXaTQ3D9lMCaWfxZV0LyH5AGLcyaasmfT8KU+iGEM8abuPHCWUyC
                |+36nJC4tn3s7I9V2gdP1Xd4Yx7+KFgN7huGVYpiM61dasCgYAQs5mPHRBeU+BHtPRyaLHhYq+jjYeocwyOpfw5wkiH3jsyUWTK9+
                |GlAoV75SYvQVIQS0VH1C1/ajz9yV02frAaUXbGtZJbyeAcyy3DjCc7iF0swJ4slP3gGVJipVF4aQ0d9wMoJ7SBaaTR0ohXeUWmTT
                |X+VGf+cZQ2IefKVnz9mg==
            """.trimMargin()

        val decryptedFile = EncryptionUtilsV2().decryptFiledrop(sut, privateKey, arbitraryDataProvider, user)
        assertEquals("test.txt", decryptedFile.filename)
    }
}
