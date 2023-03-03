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

import com.nextcloud.client.account.MockUser
import com.nextcloud.common.User
import com.owncloud.android.datamodel.e2e.v1.decrypted.Data
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedMetadata
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedUser
import junit.framework.TestCase.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class EncryptionUtilsV2IT {
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
        val encryptionUtilsV2 = EncryptionUtilsV2()
        val metadataKey = EncryptionUtils.generateKeyString()

        val metadata = DecryptedMetadata(
            listOf("hash1", "hash of key 2"),
            false,
            1,
            mapOf(
                Pair(EncryptionUtils.generateUid(), "Folder 1"),
                Pair(EncryptionUtils.generateUid(), "Folder 2"),
                Pair(EncryptionUtils.generateUid(), "Folder 3")
            ),
            mapOf(
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
        val encrypted = encryptionUtilsV2.encryptMetadata(metadata)
        val decrypted = encryptionUtilsV2.decryptMetadata(encrypted, metadataKey)

        assertEquals(metadata, decrypted)
    }

    @Test
    fun testEncryptDecryptUser() {
        val encryptionUtilsV2 = EncryptionUtilsV2()
        val metadataKey = EncryptionUtils.generateKeyString()
        val user = DecryptedUser("enc1", enc1Cert)

        val encryptedUser = encryptionUtilsV2.encryptUser(user, metadataKey)
        assertNotEquals(encryptedUser.encryptedKey, metadataKey)

        val decryptedMetadataKey = encryptionUtilsV2.decryptMetadataKey(encryptedUser, enc1PrivateKey)

        assertEquals(metadataKey, decryptedMetadataKey)
    }

    @Test
    fun testEncryptDecryptMetadataFile() {
        val encryptionUtilsV2 = EncryptionUtilsV2()

        val enc1 = MockUser("enc1", "Nextcloud")
        val metadataFile = generateDecryptedFolderMetadataFile(enc1, enc1Cert)

        val encrypted = encryptionUtilsV2.encryptFolderMetadataFile(metadataFile)
        val decrypted = encryptionUtilsV2.decryptFolderMetadataFile(encrypted, enc1, enc1PrivateKey)

        assertEquals(metadataFile, decrypted)
    }

    @Test
    fun addFile() {
        throw NotImplementedError()
    }

    @Test
    fun removeFile() {
        throw NotImplementedError()
    }

    @Test
    fun addFolder() {
        throw NotImplementedError()
    }

    @Test
    fun removeFolder() {
        throw NotImplementedError()
    }

    @Test
    fun renameFile() {
        throw NotImplementedError()
    }

    @Test
    fun updateFile() {
        throw NotImplementedError()
    }

    @Test
    fun signMetadata() {
        throw NotImplementedError()
    }

    @Test
    fun verifyMetadata() {
        throw NotImplementedError()
    }

    private fun generateDecryptedFileV1(): com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFile {
        return com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFile().apply {
            encrypted = Data().apply {
                key = EncryptionUtils.generateKeyString()
                filename = "Random filename.jpg"
                mimetype = MimeType.JPEG
                version = 1
            }
            initializationVector = EncryptionUtils.generateKeyString()
            authenticationTag = EncryptionUtils.generateKeyString()
        }
    }

    @Test
    fun testMigrateDecryptedV1ToV2() {
        val v1 = generateDecryptedFileV1()
        val v2 = EncryptionUtilsV2().migrateDecryptedFileV1ToV2(v1)

        assertEquals(v1.encrypted.filename, v2.filename)
        assertEquals(v1.encrypted.mimetype, v2.mimetype)
        assertEquals(v1.authenticationTag, v2.authenticationTag)
        assertEquals(v1.initializationVector, v2.initializationVector)
        assertEquals(v1.encrypted.key, v2.key)
    }

    @Test
    fun testMigrateMetadataV1ToV2() {
        val v1 = com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFile().apply {
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
        val v2 = EncryptionUtilsV2().migrateV1ToV2(v1, enc1UserId, enc1Cert)

        assertEquals(v1.files.size, v2.metadata.files.size)
        assertEquals(1, v2.users.size) // only one user upon migration
    }

    @Test
    fun addSharee() {
        val encryptionUtilsV2 = EncryptionUtilsV2()

        val enc1 = MockUser("enc1", "Nextcloud")
        val enc2 = MockUser("enc2", "Nextcloud")
        var metadataFile = generateDecryptedFolderMetadataFile(enc1, enc1Cert)

        metadataFile = encryptionUtilsV2.addShareeToMetadata(metadataFile, enc2, enc2Cert)

        val encryptedMetadataFile = encryptionUtilsV2.encryptFolderMetadataFile(metadataFile)

        val decryptedByEnc1 = encryptionUtilsV2.decryptFolderMetadataFile(encryptedMetadataFile, enc1, enc1PrivateKey)
        assertEquals(metadataFile.metadata, decryptedByEnc1.metadata)

        val decryptedByEnc2 = encryptionUtilsV2.decryptFolderMetadataFile(encryptedMetadataFile, enc2, enc2PrivateKey)
        assertEquals(metadataFile.metadata, decryptedByEnc2.metadata)
    }

    private fun generateDecryptedFolderMetadataFile(user: User, cert: String): DecryptedFolderMetadataFile {
        val metadata = DecryptedMetadata(
            listOf("hash1", "hash of key 2"),
            false,
            1,
            mapOf(
                Pair(EncryptionUtils.generateUid(), "Folder 1"),
                Pair(EncryptionUtils.generateUid(), "Folder 2"),
                Pair(EncryptionUtils.generateUid(), "Folder 3")
            ),
            mapOf(
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
            EncryptionUtils.generateKeyString()
        )

        val users = mutableListOf(
            DecryptedUser(user.accountName, cert)
        )

        return DecryptedFolderMetadataFile(metadata, users, emptyMap())
    }
}
