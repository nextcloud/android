/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.util

import android.text.TextUtils
import androidx.test.runner.AndroidJUnit4
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.nextcloud.test.RandomStringGenerator.make
import com.nextcloud.test.RetryTestRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.DecryptedFolderMetadata
import com.owncloud.android.datamodel.DecryptedFolderMetadata.DecryptedFile
import com.owncloud.android.datamodel.DecryptedFolderMetadata.Encrypted
import com.owncloud.android.datamodel.EncryptedFolderMetadata
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.CsrHelper
import com.owncloud.android.utils.EncryptionUtils
import junit.framework.Assert
import org.apache.commons.codec.binary.Hex
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.interfaces.RSAPrivateCrtKey
import java.security.interfaces.RSAPublicKey
import java.util.Arrays
import java.util.Random
import javax.crypto.BadPaddingException

@RunWith(AndroidJUnit4::class)
class EncryptionTestIT : AbstractIT() {
    @Rule
    var retryTestRule = RetryTestRule()
    private val privateKey = "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAo" +
        "IBAQDsn0JKS/THu328z1IgN0VzYU53HjSX03WJIgWkmyTaxbiKpoJaKbksXmfSpgzV" +
        "GzKFvGfZ03fwFrN7Q8P8R2e8SNiell7mh1TDw9/0P7Bt/ER8PJrXORo+GviKHxaLr7" +
        "Y0BJX9i/nW/L0L/VaE8CZTAqYBdcSJGgHJjY4UMf892ZPTa9T2Dl3ggdMZ7BQ2kiCi" +
        "CC3qV99b0igRJGmmLQaGiAflhFzuDQPMifUMq75wI8RSRPdxUAtjTfkl68QHu7Umye" +
        "yy33OQgdUKaTl5zcS3VSQbNjveVCNM4RDH1RlEc+7Wf1BY8APqT6jbiBcROJD2CeoL" +
        "H2eiIJCi+61ZkSGfAgMBAAECggEBALFStCHrhBf+GL9a+qer4/8QZ/X6i91PmaBX/7" +
        "SYk2jjjWVSXRNmex+V6+Y/jBRT2mvAgm8J+7LPwFdatE+lz0aZrMRD2gCWYF6Itpda" +
        "90OlLkmQPVWWtGTgX2ta2tF5r2iSGzk0IdoL8zw98Q2UzpOcw30KnWtFMxuxWk0mHq" +
        "pgp00g80cDWg3+RPbWOhdLp5bflQ36fKDfmjq05cGlIk6unnVyC5HXpvh4d4k2EWlX" +
        "rjGsndVBPCjGkZePlLRgDHxT06r+5XdJ+1CBDZgCsmjGz3M8uOHyCfVW0WhB7ynzDT" +
        "agVgz0iqpuhAi9sPt6iWWwpAnRw8cQgqEKw9bvKKECgYEA/WPi2PJtL6u/xlysh/H7" +
        "A717CId6fPHCMDace39ZNtzUzc0nT5BemlcF0wZ74NeJSur3Q395YzB+eBMLs5p8mA" +
        "95wgGvJhM65/J+HX+k9kt6Z556zLMvtG+j1yo4D0VEwm3xahB4SUUP+1kD7dNvo4+8" +
        "xeSCyjzNllvYZZC0DrECgYEA7w8pEqhHHn0a+twkPCZJS+gQTB9Rm+FBNGJqB3XpWs" +
        "TeLUxYRbVGk0iDve+eeeZ41drxcdyWP+WcL34hnrjgI1Fo4mK88saajpwUIYMy6+qM" +
        "LY+jC2NRSBox56eH7nsVYvQQK9eKqv9wbB+PF9SwOIvuETN7fd8mAY02UnoaaU8CgY" +
        "BoHRKocXPLkpZJuuppMVQiRUi4SHJbxDo19Tp2w+y0TihiJ1lvp7I3WGpcOt3LlMQk" +
        "tEbExSvrRZGxZKH6Og/XqwQsYuTEkEIz679F/5yYVosE6GkskrOXQAfh8Mb3/04xVV" +
        "tMaVgDQw0+CWVD4wyL+BNofGwBDNqsXTCdCsfxAQKBgQCDv2EtbRw0y1HRKv21QIxo" +
        "ju5cZW4+cDfVPN+eWPdQFOs1H7wOPsc0aGRiiupV2BSEF3O1ApKziEE5U1QH+29bR4" +
        "R8L1pemeGX8qCNj5bCubKjcWOz5PpouDcEqimZ3q98p3E6GEHN15UHoaTkx0yO/V8o" +
        "j6zhQ9fYRxDHB5ACtQKBgQCOO7TJUO1IaLTjcrwS4oCfJyRnAdz49L1AbVJkIBK0fh" +
        "JLecOFu3ZlQl/RStQb69QKb5MNOIMmQhg8WOxZxHcpmIDbkDAm/J/ovJXFSoBdOr5o" +
        "uQsYsDZhsWW97zvLMzg5pH9/3/1BNz5q3Vu4HgfBSwWGt4E2NENj+XA+QAVmGA=="
    private val cert = """
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

    @Test
    @Throws(Exception::class)
    fun encryptStringAsymmetric() {
        val key1 = EncryptionUtils.generateKey()
        val base64encodedKey = EncryptionUtils.encodeBytesToBase64String(key1)
        val encryptedString = EncryptionUtils.encryptStringAsymmetric(base64encodedKey, cert)
        val decryptedString = EncryptionUtils.decryptStringAsymmetric(encryptedString, privateKey)
        val key2 = EncryptionUtils.decodeStringToBase64Bytes(decryptedString)
        Assert.assertTrue(Arrays.equals(key1, key2))
    }

    @Test
    @Throws(Exception::class)
    fun encryptStringAsymmetricCorrectPublicKey() {
        val keyPair = EncryptionUtils.generateKeyPair()
        val key1 = EncryptionUtils.generateKey()
        val base64encodedKey = EncryptionUtils.encodeBytesToBase64String(key1)
        val encryptedString = EncryptionUtils.encryptStringAsymmetric(base64encodedKey, keyPair.public)
        val decryptedString = EncryptionUtils.decryptStringAsymmetric(encryptedString, keyPair.private)
        val key2 = EncryptionUtils.decodeStringToBase64Bytes(decryptedString)
        Assert.assertTrue(Arrays.equals(key1, key2))
    }

    @Test(expected = BadPaddingException::class)
    @Throws(Exception::class)
    fun encryptStringAsymmetricWrongPublicKey() {
        val keyPair1 = EncryptionUtils.generateKeyPair()
        val keyPair2 = EncryptionUtils.generateKeyPair()
        val key1 = EncryptionUtils.generateKey()
        val base64encodedKey = EncryptionUtils.encodeBytesToBase64String(key1)
        val encryptedString = EncryptionUtils.encryptStringAsymmetric(base64encodedKey, keyPair1.public)
        EncryptionUtils.decryptStringAsymmetric(encryptedString, keyPair2.private)
    }

    @Test
    @Throws(Exception::class)
    fun testModulus() {
        val keyPair = EncryptionUtils.generateKeyPair()
        val publicKey = keyPair.public as RSAPublicKey
        val privateKey = keyPair.private as RSAPrivateCrtKey
        val modulusPublic = publicKey.modulus
        val modulusPrivate = privateKey.modulus
        org.junit.Assert.assertEquals(modulusPrivate, modulusPublic)
    }

    @Test
    @Throws(Exception::class)
    fun encryptStringSymmetricRandom() {
        val max = 500
        for (i in 0 until max) {
            Log_OC.d("EncryptionTestIT", "$i of $max")
            val key = EncryptionUtils.generateKey()
            var encryptedString: String
            if (Random().nextBoolean()) {
                encryptedString = EncryptionUtils.encryptStringSymmetric(privateKey, key)
            } else {
                encryptedString = EncryptionUtils.encryptStringSymmetricOld(privateKey, key)
                if (encryptedString.indexOf(EncryptionUtils.ivDelimiterOld) != encryptedString.lastIndexOf(
                        EncryptionUtils.ivDelimiterOld
                    )
                ) {
                    Log_OC.d("EncryptionTestIT", "skip due to duplicated iv (old system) -> ignoring")
                    continue
                }
            }
            val decryptedString = EncryptionUtils.decryptStringSymmetric(encryptedString, key)
            org.junit.Assert.assertEquals(privateKey, decryptedString)
        }
    }

    @Test
    @Throws(Exception::class)
    fun encryptStringSymmetric() {
        val max = 5000
        val key = EncryptionUtils.generateKey()
        for (i in 0 until max) {
            Log_OC.d("EncryptionTestIT", "$i of $max")
            val encryptedString = EncryptionUtils.encryptStringSymmetric(privateKey, key)
            var delimiterPosition = encryptedString.indexOf(EncryptionUtils.ivDelimiter)
            if (delimiterPosition == -1) {
                throw RuntimeException("IV not found!")
            }
            var ivString = encryptedString.substring(delimiterPosition + EncryptionUtils.ivDelimiter.length)
            if (TextUtils.isEmpty(ivString)) {
                delimiterPosition = encryptedString.lastIndexOf(EncryptionUtils.ivDelimiter)
                ivString = encryptedString.substring(delimiterPosition + EncryptionUtils.ivDelimiter.length)
                if (TextUtils.isEmpty(ivString)) {
                    throw RuntimeException("IV string is empty")
                }
            }
            val decryptedString = EncryptionUtils.decryptStringSymmetric(encryptedString, key)
            org.junit.Assert.assertEquals(privateKey, decryptedString)
        }
    }

    @Test
    @Throws(Exception::class)
    fun encryptPrivateKey() {
        val max = 10
        for (i in 0 until max) {
            Log_OC.d("EncryptionTestIT", "$i of $max")
            val keyPhrase = "moreovertelevisionfactorytendencyindependenceinternationalintellectualimpress" +
                "interestvolunteer"
            val keyGen = KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(4096, SecureRandom())
            val keyPair = keyGen.generateKeyPair()
            val privateKey = keyPair.private
            val privateKeyBytes = privateKey.encoded
            val privateKeyString = EncryptionUtils.encodeBytesToBase64String(privateKeyBytes)
            var encryptedString: String?
            encryptedString = if (Random().nextBoolean()) {
                EncryptionUtils.encryptPrivateKey(privateKeyString, keyPhrase)
            } else {
                EncryptionUtils.encryptPrivateKeyOld(privateKeyString, keyPhrase)
            }
            val decryptedString = EncryptionUtils.decryptPrivateKey(encryptedString, keyPhrase)
            org.junit.Assert.assertEquals(privateKeyString, decryptedString)
        }
    }

    @Test
    @Throws(Exception::class)
    fun generateCSR() {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048, SecureRandom())
        val keyPair = keyGen.generateKeyPair()
        Assert.assertFalse(CsrHelper.generateCsrPemEncodedString(keyPair, "").isEmpty())
        Assert.assertFalse(EncryptionUtils.encodeBytesToBase64String(keyPair.public.encoded).isEmpty())
    }

    /**
     * DecryptedFolderMetadata -> EncryptedFolderMetadata -> JSON -> encrypt -> decrypt -> JSON ->
     * EncryptedFolderMetadata -> DecryptedFolderMetadata
     */
    @Test
    @Throws(Exception::class)
    fun encryptionMetadata() {
        val decryptedFolderMetadata1 = generateFolderMetadata()
        val arbitraryDataProvider: ArbitraryDataProvider = ArbitraryDataProviderImpl(targetContext)
        val folderID: Long = 1

        // encrypt
        val encryptedFolderMetadata1 = EncryptionUtils.encryptFolderMetadata(
            decryptedFolderMetadata1,
            cert,
            arbitraryDataProvider,
            user,
            folderID
        )

        // serialize
        val encryptedJson = EncryptionUtils.serializeJSON(encryptedFolderMetadata1)

        // de-serialize
        val encryptedFolderMetadata2 = EncryptionUtils.deserializeJSON(encryptedJson,
            object : TypeToken<EncryptedFolderMetadata?>() {})

        // decrypt
        val decryptedFolderMetadata2 = EncryptionUtils.decryptFolderMetaData(
            encryptedFolderMetadata2,
            privateKey,
            arbitraryDataProvider,
            user,
            folderID
        )

        // compare
        Assert.assertTrue(
            compareJsonStrings(
                EncryptionUtils.serializeJSON(decryptedFolderMetadata1),
                EncryptionUtils.serializeJSON(decryptedFolderMetadata2)
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun testChangedMetadataKey() {
        val decryptedFolderMetadata1 = generateFolderMetadata()
        val arbitraryDataProvider: ArbitraryDataProvider = ArbitraryDataProviderImpl(targetContext)
        val folderID: Long = 1

        // encrypt
        val encryptedFolderMetadata1 = EncryptionUtils.encryptFolderMetadata(
            decryptedFolderMetadata1,
            cert,
            arbitraryDataProvider,
            user,
            folderID
        )

        // store metadata key
        val oldMetadataKey = encryptedFolderMetadata1.metadata.metadataKey

        // do it again
        // encrypt
        val encryptedFolderMetadata2 = EncryptionUtils.encryptFolderMetadata(
            decryptedFolderMetadata1,
            cert,
            arbitraryDataProvider,
            user,
            folderID
        )
        val newMetadataKey = encryptedFolderMetadata2.metadata.metadataKey
        org.junit.Assert.assertNotEquals(oldMetadataKey, newMetadataKey)
    }

    @Test
    @Throws(Exception::class)
    fun testMigrateMetadataKey() {
        val decryptedFolderMetadata1 = generateFolderMetadata()
        val arbitraryDataProvider: ArbitraryDataProvider = ArbitraryDataProviderImpl(targetContext)
        val folderID: Long = 1

        // encrypt
        val encryptedFolderMetadata1 = EncryptionUtils.encryptFolderMetadata(
            decryptedFolderMetadata1,
            cert,
            arbitraryDataProvider,
            user,
            folderID
        )

        // reset new metadata key, to mimic old version
        encryptedFolderMetadata1.metadata.metadataKey = null
        val oldMetadataKey = encryptedFolderMetadata1.metadata.metadataKey

        // do it again
        // encrypt
        val encryptedFolderMetadata2 = EncryptionUtils.encryptFolderMetadata(
            decryptedFolderMetadata1,
            cert,
            arbitraryDataProvider,
            user,
            folderID
        )
        val newMetadataKey = encryptedFolderMetadata2.metadata.metadataKey
        org.junit.Assert.assertNotEquals(oldMetadataKey, newMetadataKey)
    }

    @Test
    @Throws(Exception::class)
    fun testCryptFileWithoutMetadata() {
        val key = EncryptionUtils.decodeStringToBase64Bytes("WANM0gRv+DhaexIsI0T3Lg==")
        val iv = EncryptionUtils.decodeStringToBase64Bytes("gKm3n+mJzeY26q4OfuZEqg==")
        val authTag = EncryptionUtils.decodeStringToBase64Bytes("PboI9tqHHX3QeAA22PIu4w==")
        Assert.assertTrue(cryptFile("ia7OEEEyXMoRa1QWQk8r", "78f42172166f9dc8fd1a7156b1753353", key, iv, authTag))
    }

    @Test
    @Throws(Exception::class)
    fun cryptFileWithMetadata() {
        val metadata = generateFolderMetadata()

        // n9WXAIXO2wRY4R8nXwmo
        Assert.assertTrue(
            cryptFile(
                "ia7OEEEyXMoRa1QWQk8r",
                "78f42172166f9dc8fd1a7156b1753353",
                EncryptionUtils.decodeStringToBase64Bytes(
                    metadata.files["ia7OEEEyXMoRa1QWQk8r"]
                        !!.getEncrypted().key
                ),
                EncryptionUtils.decodeStringToBase64Bytes(
                    metadata.files["ia7OEEEyXMoRa1QWQk8r"]
                        !!.getInitializationVector()
                ),
                EncryptionUtils.decodeStringToBase64Bytes(
                    metadata.files["ia7OEEEyXMoRa1QWQk8r"]
                        !!.getAuthenticationTag()
                )
            )
        )

        // n9WXAIXO2wRY4R8nXwmo
        Assert.assertTrue(
            cryptFile(
                "n9WXAIXO2wRY4R8nXwmo",
                "825143ed1f21ebb0c3b3c3f005b2f5db",
                EncryptionUtils.decodeStringToBase64Bytes(
                    metadata.files["n9WXAIXO2wRY4R8nXwmo"]
                        !!.getEncrypted().key
                ),
                EncryptionUtils.decodeStringToBase64Bytes(
                    metadata.files["n9WXAIXO2wRY4R8nXwmo"]
                        !!.getInitializationVector()
                ),
                EncryptionUtils.decodeStringToBase64Bytes(
                    metadata.files["n9WXAIXO2wRY4R8nXwmo"]
                        !!.getAuthenticationTag()
                )
            )
        )
    }

    @Test
    @Throws(Exception::class)
    fun bigMetadata() {
        val decryptedFolderMetadata1 = generateFolderMetadata()
        val arbitraryDataProvider: ArbitraryDataProvider = ArbitraryDataProviderImpl(targetContext)
        val folderID: Long = 1

        // encrypt
        var encryptedFolderMetadata1 = EncryptionUtils.encryptFolderMetadata(
            decryptedFolderMetadata1,
            cert,
            arbitraryDataProvider,
            user,
            folderID
        )

        // serialize
        var encryptedJson = EncryptionUtils.serializeJSON(encryptedFolderMetadata1)

        // de-serialize
        var encryptedFolderMetadata2 = EncryptionUtils.deserializeJSON(encryptedJson,
            object : TypeToken<EncryptedFolderMetadata?>() {})

        // decrypt
        var decryptedFolderMetadata2 = EncryptionUtils.decryptFolderMetaData(
            encryptedFolderMetadata2,
            privateKey,
            arbitraryDataProvider,
            user,
            folderID
        )

        // compare
        Assert.assertTrue(
            compareJsonStrings(
                EncryptionUtils.serializeJSON(decryptedFolderMetadata1),
                EncryptionUtils.serializeJSON(decryptedFolderMetadata2)
            )
        )

        // prefill with 500
        for (i in 0..499) {
            addFile(decryptedFolderMetadata1, i)
        }
        val max = 505
        for (i in 500 until max) {
            Log_OC.d(this, "Big metadata: $i of $max")
            addFile(decryptedFolderMetadata1, i)

            // encrypt
            encryptedFolderMetadata1 = EncryptionUtils.encryptFolderMetadata(
                decryptedFolderMetadata1,
                cert,
                arbitraryDataProvider,
                user,
                folderID
            )

            // serialize
            encryptedJson = EncryptionUtils.serializeJSON(encryptedFolderMetadata1)

            // de-serialize
            encryptedFolderMetadata2 = EncryptionUtils.deserializeJSON(encryptedJson,
                object : TypeToken<EncryptedFolderMetadata?>() {})

            // decrypt
            decryptedFolderMetadata2 = EncryptionUtils.decryptFolderMetaData(
                encryptedFolderMetadata2,
                privateKey,
                arbitraryDataProvider,
                user,
                folderID
            )

            // compare
            Assert.assertTrue(
                compareJsonStrings(
                    EncryptionUtils.serializeJSON(decryptedFolderMetadata1),
                    EncryptionUtils.serializeJSON(decryptedFolderMetadata2)
                )
            )
            org.junit.Assert.assertEquals((i + 3).toLong(), decryptedFolderMetadata1.files.size.toLong())
            org.junit.Assert.assertEquals((i + 3).toLong(), decryptedFolderMetadata2.files.size.toLong())
        }
    }

    @Test
    @Throws(Exception::class)
    fun filedrop() {
        val decryptedFolderMetadata1 = generateFolderMetadata()
        val arbitraryDataProvider: ArbitraryDataProvider = ArbitraryDataProviderImpl(targetContext)
        val folderID: Long = 1

        // add filedrop
        val filesdrop: MutableMap<String, DecryptedFile> = HashMap()
        val data = DecryptedFolderMetadata.Data()
        data.key = "9dfzbIYDt28zTyZfbcll+g=="
        data.filename = "test2.txt"
        data.setVersion(1)
        val file = DecryptedFile()
        file.initializationVector = "hnJLF8uhDvDoFK4ajuvwrg=="
        file.encrypted = data
        file.metadataKey = 0
        file.authenticationTag = "qOQZdu5soFO77Y7y4rAOVA=="
        filesdrop["eie8iaeiaes8e87td6"] = file
        decryptedFolderMetadata1.filedrop = filesdrop

        // encrypt
        val encryptedFolderMetadata1 = EncryptionUtils.encryptFolderMetadata(
            decryptedFolderMetadata1,
            cert,
            arbitraryDataProvider,
            user,
            folderID
        )
        EncryptionUtils.encryptFileDropFiles(decryptedFolderMetadata1, encryptedFolderMetadata1, cert)

        // serialize
        val encryptedJson = EncryptionUtils.serializeJSON(encryptedFolderMetadata1)

        // de-serialize
        val encryptedFolderMetadata2 = EncryptionUtils.deserializeJSON(encryptedJson,
            object : TypeToken<EncryptedFolderMetadata?>() {})

        // decrypt
        val decryptedFolderMetadata2 = EncryptionUtils.decryptFolderMetaData(
            encryptedFolderMetadata2,
            privateKey,
            arbitraryDataProvider,
            user,
            folderID
        )

        // compare
        Assert.assertFalse(
            compareJsonStrings(
                EncryptionUtils.serializeJSON(decryptedFolderMetadata1),
                EncryptionUtils.serializeJSON(decryptedFolderMetadata2)
            )
        )
        org.junit.Assert.assertEquals(
            (decryptedFolderMetadata1.files.size + decryptedFolderMetadata1.filedrop.size).toLong(),
            decryptedFolderMetadata2.files.size.toLong()
        )

        // no filedrop content means null
        org.junit.Assert.assertNull(decryptedFolderMetadata2.filedrop)
    }

    private fun addFile(decryptedFolderMetadata: DecryptedFolderMetadata, counter: Int) {
        // Add new file
        // Always generate new
        val key = EncryptionUtils.generateKey()
        val iv = EncryptionUtils.randomBytes(EncryptionUtils.ivLength)
        val authTag = EncryptionUtils.randomBytes(128 / 8)
        val data = DecryptedFolderMetadata.Data()
        data.key = EncryptionUtils.encodeBytesToBase64String(key)
        data.filename = "$counter.txt"
        data.setVersion(1)
        val file = DecryptedFile()
        file.initializationVector = EncryptionUtils.encodeBytesToBase64String(iv)
        file.encrypted = data
        file.metadataKey = 0
        file.authenticationTag = EncryptionUtils.encodeBytesToBase64String(authTag)
        decryptedFolderMetadata.files[make(20)] = file
    }

    /**
     * generates new keys and tests if they are unique
     */
    @Test
    fun testKey() {
        val keys: MutableSet<String> = HashSet()
        for (i in 0..49) {
            Assert.assertTrue(keys.add(EncryptionUtils.encodeBytesToBase64String(EncryptionUtils.generateKey())))
        }
    }

    /**
     * generates new ivs and tests if they are unique
     */
    @Test
    fun testIV() {
        val ivs: MutableSet<String> = HashSet()
        for (i in 0..49) {
            Assert.assertTrue(
                ivs.add(
                    EncryptionUtils.encodeBytesToBase64String(
                        EncryptionUtils.randomBytes(EncryptionUtils.ivLength)
                    )
                )
            )
        }
    }

    /**
     * generates new salt and tests if they are unique
     */
    @Test
    fun testSalt() {
        val ivs: MutableSet<String> = HashSet()
        for (i in 0..49) {
            Assert.assertTrue(
                ivs.add(
                    EncryptionUtils.encodeBytesToBase64String(
                        EncryptionUtils.randomBytes(EncryptionUtils.saltLength)
                    )
                )
            )
        }
    }

    @Test
    fun testSHA512() {
        // sent to 3rd party app in cleartext
        val token = "4ae5978bf5354cd284b539015d442141"
        val salt = EncryptionUtils.encodeBytesToBase64String(EncryptionUtils.randomBytes(EncryptionUtils.saltLength))

        // stored in database
        val hashedToken = EncryptionUtils.generateSHA512(token, salt)

        // check: use passed cleartext and salt to verify hashed token
        Assert.assertTrue(EncryptionUtils.verifySHA512(hashedToken, token))
    }

    @Test
    @Throws(Exception::class)
    fun testExcludeGSON() {
        val metadata = generateFolderMetadata()
        val jsonWithKeys = EncryptionUtils.serializeJSON(metadata)
        val jsonWithoutKeys = EncryptionUtils.serializeJSON(metadata, true)
        Assert.assertTrue(jsonWithKeys.contains("metadataKeys"))
        Assert.assertFalse(jsonWithoutKeys.contains("metadataKeys"))
    }

    @Test
    @Throws(Exception::class)
    fun testChecksum() {
        val metadata = DecryptedFolderMetadata()
        val mnemonic = "chimney potato joke science ridge trophy result estate spare vapor much room"
        metadata.files["n9WXAIXO2wRY4R8nXwmo"] = DecryptedFile()
        metadata.files["ia7OEEEyXMoRa1QWQk8r"] = DecryptedFile()
        val encryptedMetadataKey = "GuFPAULudgD49S4+VDFck3LiqQ8sx4zmbrBtdpCSGcT+T0W0z4F5gYQYPlzTG6WOkdW5LJZK/"
        metadata.metadata.metadataKey = encryptedMetadataKey
        val checksum = EncryptionUtils.generateChecksum(metadata, mnemonic)
        val expectedChecksum = "002cefa6493f2efb0192247a34bb1b16d391aefee968fd3d4225c4ec3cd56436"
        org.junit.Assert.assertEquals(expectedChecksum, checksum)

        // change something
        val newMnemonic = mnemonic + "1"
        var newChecksum = EncryptionUtils.generateChecksum(metadata, newMnemonic)
        org.junit.Assert.assertNotEquals(expectedChecksum, newChecksum)
        metadata.files["aeb34yXMoRa1QWQk8r"] = DecryptedFile()
        newChecksum = EncryptionUtils.generateChecksum(metadata, mnemonic)
        org.junit.Assert.assertNotEquals(expectedChecksum, newChecksum)
    }

    @Test
    fun testAddIdToMigratedIds() {
        val arbitraryDataProvider: ArbitraryDataProvider = ArbitraryDataProviderImpl(targetContext)

        // delete ids
        arbitraryDataProvider.deleteKeyForAccount(user.accountName, EncryptionUtils.MIGRATED_FOLDER_IDS)
        val id: Long = 1
        EncryptionUtils.addIdToMigratedIds(id, user, arbitraryDataProvider)
        Assert.assertTrue(EncryptionUtils.isFolderMigrated(id, user, arbitraryDataProvider))
    }

    // Helper
    private fun compareJsonStrings(expected: String, actual: String): Boolean {
        val parser = JsonParser()
        val o1 = parser.parse(expected)
        val o2 = parser.parse(actual)
        return if (o1 == o2) {
            true
        } else {
            println("expected: $o1")
            println("actual: $o2")
            false
        }
    }

    @Throws(Exception::class)
    private fun generateFolderMetadata(): DecryptedFolderMetadata {
        val metadataKey0 = EncryptionUtils.encodeBytesToBase64String(EncryptionUtils.generateKey())
        val metadataKey1 = EncryptionUtils.encodeBytesToBase64String(EncryptionUtils.generateKey())
        val metadataKey2 = EncryptionUtils.encodeBytesToBase64String(EncryptionUtils.generateKey())
        val metadataKeys = HashMap<Int, String>()
        metadataKeys[0] = EncryptionUtils.encryptStringAsymmetric(metadataKey0, cert)
        metadataKeys[1] = EncryptionUtils.encryptStringAsymmetric(metadataKey1, cert)
        metadataKeys[2] = EncryptionUtils.encryptStringAsymmetric(metadataKey2, cert)
        val encrypted = Encrypted()
        encrypted.metadataKeys = metadataKeys
        val metadata1 = DecryptedFolderMetadata.Metadata()
        metadata1.metadataKeys = metadataKeys
        metadata1.version = 1.1
        val files = HashMap<String, DecryptedFile>()
        val data1 = DecryptedFolderMetadata.Data()
        data1.key = "WANM0gRv+DhaexIsI0T3Lg=="
        data1.filename = "test.txt"
        data1.setVersion(1)
        val file1 = DecryptedFile()
        file1.initializationVector = "gKm3n+mJzeY26q4OfuZEqg=="
        file1.encrypted = data1
        file1.metadataKey = 0
        file1.authenticationTag = "PboI9tqHHX3QeAA22PIu4w=="
        files["ia7OEEEyXMoRa1QWQk8r"] = file1
        val data2 = DecryptedFolderMetadata.Data()
        data2.key = "9dfzbIYDt28zTyZfbcll+g=="
        data2.filename = "test2.txt"
        data2.setVersion(1)
        val file2 = DecryptedFile()
        file2.initializationVector = "hnJLF8uhDvDoFK4ajuvwrg=="
        file2.encrypted = data2
        file2.metadataKey = 0
        file2.authenticationTag = "qOQZdu5soFO77Y7y4rAOVA=="
        files["n9WXAIXO2wRY4R8nXwmo"] = file2
        return DecryptedFolderMetadata(metadata1, files)
    }

    @Throws(Exception::class)
    private fun cryptFile(
        fileName: String,
        md5: String,
        key: ByteArray,
        iv: ByteArray,
        expectedAuthTag: ByteArray
    ): Boolean {
        val file = getFile(fileName)
        org.junit.Assert.assertEquals(md5, getMD5Sum(file))
        val encryptedFile = EncryptionUtils.encryptFile(file, key, iv)
        val encryptedTempFile = File.createTempFile("file", "tmp")
        val fileOutputStream = FileOutputStream(encryptedTempFile)
        fileOutputStream.write(encryptedFile.encryptedBytes)
        fileOutputStream.close()
        val authenticationTag = EncryptionUtils.decodeStringToBase64Bytes(encryptedFile.authenticationTag)

        // verify authentication tag
        Assert.assertTrue(Arrays.equals(expectedAuthTag, authenticationTag))
        val decryptedBytes = EncryptionUtils.decryptFile(encryptedTempFile, key, iv, authenticationTag)
        val decryptedFile = File.createTempFile("file", "dec")
        val fileOutputStream1 = FileOutputStream(decryptedFile)
        fileOutputStream1.write(decryptedBytes)
        fileOutputStream1.close()
        return md5.compareTo(getMD5Sum(decryptedFile)) == 0
    }

    private fun getMD5Sum(file: File): String {
        var fileInputStream: FileInputStream? = null
        try {
            fileInputStream = FileInputStream(file)
            val md5 = MessageDigest.getInstance("MD5")
            val bytes = ByteArray(2048)
            var readBytes: Int
            while (fileInputStream.read(bytes).also { readBytes = it } != -1) {
                md5.update(bytes, 0, readBytes)
            }
            return kotlin.String()
        } catch (e: Exception) {
            Log_OC.e(this, e.message)
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close()
                } catch (e: IOException) {
                    Log_OC.e(this, "Error getting MD5 checksum for file", e)
                }
            }
        }
        return ""
    }
}