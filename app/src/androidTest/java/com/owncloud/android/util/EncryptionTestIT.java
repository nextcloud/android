/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.util;

import android.text.TextUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.nextcloud.test.RandomStringGenerator;
import com.nextcloud.test.RetryTestRule;
import com.owncloud.android.AbstractIT;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl;
import com.owncloud.android.datamodel.e2e.v1.decrypted.Data;
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFile;
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFileV1;
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedMetadata;
import com.owncloud.android.datamodel.e2e.v1.decrypted.Encrypted;
import com.owncloud.android.datamodel.e2e.v1.encrypted.EncryptedFolderMetadataFileV1;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.e2ee.CsrHelper;
import com.owncloud.android.utils.EncryptionUtils;
import com.owncloud.android.utils.crypto.CryptoHelper;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;

import static com.owncloud.android.utils.EncryptionUtils.decodeStringToBase64Bytes;
import static com.owncloud.android.utils.EncryptionUtils.decryptFile;
import static com.owncloud.android.utils.EncryptionUtils.decryptFolderMetaData;
import static com.owncloud.android.utils.EncryptionUtils.decryptStringAsymmetric;
import static com.owncloud.android.utils.EncryptionUtils.decryptStringSymmetric;
import static com.owncloud.android.utils.EncryptionUtils.deserializeJSON;
import static com.owncloud.android.utils.EncryptionUtils.encodeBytesToBase64String;
import static com.owncloud.android.utils.EncryptionUtils.encryptFolderMetadata;
import static com.owncloud.android.utils.EncryptionUtils.generateChecksum;
import static com.owncloud.android.utils.EncryptionUtils.generateKey;
import static com.owncloud.android.utils.EncryptionUtils.generateSHA512;
import static com.owncloud.android.utils.EncryptionUtils.isFolderMigrated;
import static com.owncloud.android.utils.EncryptionUtils.ivDelimiter;
import static com.owncloud.android.utils.EncryptionUtils.ivDelimiterOld;
import static com.owncloud.android.utils.EncryptionUtils.ivLength;
import static com.owncloud.android.utils.EncryptionUtils.randomBytes;
import static com.owncloud.android.utils.EncryptionUtils.saltLength;
import static com.owncloud.android.utils.EncryptionUtils.serializeJSON;
import static com.owncloud.android.utils.EncryptionUtils.verifySHA512;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

public class EncryptionTestIT extends AbstractIT {
    @Rule public RetryTestRule retryTestRule = new RetryTestRule();

    ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProviderImpl(targetContext);

    private static final String MD5_ALGORITHM = "MD5";

    private static final String filename = "ia7OEEEyXMoRa1QWQk8r";
    private static final String secondFilename = "n9WXAIXO2wRY4R8nXwmo";

    public static final String privateKey = "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAo" +
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
        "uQsYsDZhsWW97zvLMzg5pH9/3/1BNz5q3Vu4HgfBSwWGt4E2NENj+XA+QAVmGA==";

    public static final String publicKey = "-----BEGIN CERTIFICATE-----\n" +
        "MIIDpzCCAo+gAwIBAgIBADANBgkqhkiG9w0BAQUFADBuMRowGAYDVQQDDBF3d3cu\n" +
        "bmV4dGNsb3VkLmNvbTESMBAGA1UECgwJTmV4dGNsb3VkMRIwEAYDVQQHDAlTdHV0\n" +
        "dGdhcnQxGzAZBgNVBAgMEkJhZGVuLVd1ZXJ0dGVtYmVyZzELMAkGA1UEBhMCREUw\n" +
        "HhcNMTcwOTI2MTAwNDMwWhcNMzcwOTIxMTAwNDMwWjBuMRowGAYDVQQDDBF3d3cu\n" +
        "bmV4dGNsb3VkLmNvbTESMBAGA1UECgwJTmV4dGNsb3VkMRIwEAYDVQQHDAlTdHV0\n" +
        "dGdhcnQxGzAZBgNVBAgMEkJhZGVuLVd1ZXJ0dGVtYmVyZzELMAkGA1UEBhMCREUw\n" +
        "ggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDsn0JKS/THu328z1IgN0Vz\n" +
        "YU53HjSX03WJIgWkmyTaxbiKpoJaKbksXmfSpgzVGzKFvGfZ03fwFrN7Q8P8R2e8\n" +
        "SNiell7mh1TDw9/0P7Bt/ER8PJrXORo+GviKHxaLr7Y0BJX9i/nW/L0L/VaE8CZT\n" +
        "AqYBdcSJGgHJjY4UMf892ZPTa9T2Dl3ggdMZ7BQ2kiCiCC3qV99b0igRJGmmLQaG\n" +
        "iAflhFzuDQPMifUMq75wI8RSRPdxUAtjTfkl68QHu7Umyeyy33OQgdUKaTl5zcS3\n" +
        "VSQbNjveVCNM4RDH1RlEc+7Wf1BY8APqT6jbiBcROJD2CeoLH2eiIJCi+61ZkSGf\n" +
        "AgMBAAGjUDBOMB0GA1UdDgQWBBTFrXz2tk1HivD9rQ75qeoyHrAgIjAfBgNVHSME\n" +
        "GDAWgBTFrXz2tk1HivD9rQ75qeoyHrAgIjAMBgNVHRMEBTADAQH/MA0GCSqGSIb3\n" +
        "DQEBBQUAA4IBAQARQTX21QKO77gAzBszFJ6xVnjfa23YZF26Z4X1KaM8uV8TGzuN\n" +
        "JA95XmReeP2iO3r8EWXS9djVCD64m2xx6FOsrUI8HZaw1JErU8mmOaLAe8q9RsOm\n" +
        "9Eq37e4vFp2YUEInYUqs87ByUcA4/8g3lEYeIUnRsRsWsA45S3wD7wy07t+KAn7j\n" +
        "yMmfxdma6hFfG9iN/egN6QXUAyIPXvUvlUuZ7/BhWBj/3sHMrF9quy9Q2DOI8F3t\n" +
        "1wdQrkq4BtStKhciY5AIXz9SqsctFHTv4Lwgtkapoel4izJnO0ZqYTXVe7THwri9\n" +
        "H/gua6uJDWH9jk2/CiZDWfsyFuNUuXvDSp05\n" +
        "-----END CERTIFICATE-----";

    @Test
    public void encryptStringAsymmetric() throws Exception {
        byte[] key1 = generateKey();
        String base64encodedKey = encodeBytesToBase64String(key1);

        String encryptedString = EncryptionUtils.encryptStringAsymmetric(base64encodedKey, publicKey);
        String decryptedString = decryptStringAsymmetric(encryptedString, privateKey);

        byte[] key2 = decodeStringToBase64Bytes(decryptedString);

        assertArrayEquals(key1, key2);
    }

    @Test
    public void encryptStringAsymmetricCorrectPublicKey() throws Exception {
        KeyPair keyPair = EncryptionUtils.generateKeyPair();

        byte[] key1 = generateKey();
        String base64encodedKey = encodeBytesToBase64String(key1);

        String encryptedString = EncryptionUtils.encryptStringAsymmetric(base64encodedKey, keyPair.getPublic());
        String decryptedString = decryptStringAsymmetric(encryptedString, keyPair.getPrivate());

        byte[] key2 = decodeStringToBase64Bytes(decryptedString);

        assertArrayEquals(key1, key2);
    }

    @Test(expected = BadPaddingException.class)
    public void encryptStringAsymmetricWrongPublicKey() throws Exception {
        KeyPair keyPair1 = EncryptionUtils.generateKeyPair();
        KeyPair keyPair2 = EncryptionUtils.generateKeyPair();

        byte[] key1 = generateKey();
        String base64encodedKey = encodeBytesToBase64String(key1);

        String encryptedString = EncryptionUtils.encryptStringAsymmetric(base64encodedKey, keyPair1.getPublic());
        decryptStringAsymmetric(encryptedString, keyPair2.getPrivate());
    }

    @Test
    public void testModulus() throws Exception {
        KeyPair keyPair = EncryptionUtils.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) keyPair.getPrivate();

        BigInteger modulusPublic = publicKey.getModulus();
        BigInteger modulusPrivate = privateKey.getModulus();

        assertEquals(modulusPrivate, modulusPublic);
    }

    @Test
    public void encryptStringSymmetricRandom() throws Exception {
        int max = 500;
        for (int i = 0; i < max; i++) {
            Log_OC.d("EncryptionTestIT", i + " of " + max);
            byte[] key = generateKey();

            String encryptedString;
            if (new Random().nextBoolean()) {
                encryptedString = EncryptionUtils.encryptStringSymmetricAsString(privateKey, key);
            } else {
                encryptedString = EncryptionUtils.encryptStringSymmetricAsStringOld(privateKey, key);

                if (encryptedString.indexOf(ivDelimiterOld) != encryptedString.lastIndexOf(ivDelimiterOld)) {
                    Log_OC.d("EncryptionTestIT", "skip due to duplicated iv (old system) -> ignoring");
                    continue;
                }
            }
            String decryptedString = decryptStringSymmetric(encryptedString, key);

            assertEquals(privateKey, decryptedString);
        }
    }

    @Test
    public void encryptStringSymmetric() throws Exception {
        int max = 5000;
        byte[] key = generateKey();

        for (int i = 0; i < max; i++) {
            Log_OC.d("EncryptionTestIT", i + " of " + max);

            String encryptedString = EncryptionUtils.encryptStringSymmetricAsString(privateKey, key);

            int delimiterPosition = encryptedString.indexOf(ivDelimiter);
            if (delimiterPosition == -1) {
                throw new RuntimeException("IV not found!");
            }

            String ivString = encryptedString.substring(delimiterPosition + ivDelimiter.length());
            if (TextUtils.isEmpty(ivString)) {
                delimiterPosition = encryptedString.lastIndexOf(ivDelimiter);
                ivString = encryptedString.substring(delimiterPosition + ivDelimiter.length());

                if (TextUtils.isEmpty(ivString)) {
                    throw new RuntimeException("IV string is empty");
                }
            }

            String decryptedString = decryptStringSymmetric(encryptedString, key);

            assertEquals(privateKey, decryptedString);
        }
    }

    @Test
    public void encryptPrivateKey() throws Exception {
        int max = 10;
        for (int i = 0; i < max; i++) {
            Log_OC.d("EncryptionTestIT", i + " of " + max);

            String keyPhrase = "moreovertelevisionfactorytendencyindependenceinternationalintellectualimpress" +
                "interestvolunteer";
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(4096, new SecureRandom());
            KeyPair keyPair = keyGen.generateKeyPair();
            PrivateKey privateKey = keyPair.getPrivate();
            byte[] privateKeyBytes = privateKey.getEncoded();
            String privateKeyString = encodeBytesToBase64String(privateKeyBytes);

            String encryptedString = CryptoHelper.INSTANCE.encryptPrivateKey(privateKeyString, keyPhrase);
            String decryptedString = CryptoHelper.INSTANCE.decryptPrivateKey(encryptedString, keyPhrase);

            assertEquals(privateKeyString, decryptedString);
        }
    }

    @Test
    public void generateCSR() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048, new SecureRandom());
        KeyPair keyPair = keyGen.generateKeyPair();

        assertFalse(new CsrHelper().generateCsrPemEncodedString(keyPair, "").isEmpty());
        assertFalse(encodeBytesToBase64String(keyPair.getPublic().getEncoded()).isEmpty());
    }


    /**
     * DecryptedFolderMetadataFile -> EncryptedFolderMetadataFile -> JSON -> encrypt -> decrypt -> JSON ->
     * EncryptedFolderMetadataFile -> DecryptedFolderMetadataFile
     */
    @Test
    public void encryptionMetadataV1() throws Exception {
        DecryptedFolderMetadataFileV1 decryptedFolderMetadata1 = generateFolderMetadataV1_1();

        // encrypt
        EncryptedFolderMetadataFileV1 encryptedFolderMetadata1 = encryptFolderMetadata(
            decryptedFolderMetadata1,
            publicKey,
            1,
            user,
            arbitraryDataProvider);

        // serialize
        String encryptedJson = serializeJSON(encryptedFolderMetadata1);

        // de-serialize
        EncryptedFolderMetadataFileV1 encryptedFolderMetadata2 = deserializeJSON(encryptedJson,
                                                                                 new TypeToken<>() {
                                                                                 });

        // decrypt
        DecryptedFolderMetadataFileV1 decryptedFolderMetadata2 = decryptFolderMetaData(
            encryptedFolderMetadata2,
            privateKey,
            arbitraryDataProvider,
            user,
            1);

        // compare
        assertTrue(compareJsonStrings(serializeJSON(decryptedFolderMetadata1),
                                      serializeJSON(decryptedFolderMetadata2)));
    }

    @Test
    public void testChangedMetadataKey() throws Exception {
        DecryptedFolderMetadataFileV1 decryptedFolderMetadata1 = generateFolderMetadataV1_1();
        long folderID = 1;

        // encrypt
        EncryptedFolderMetadataFileV1 encryptedFolderMetadata1 = encryptFolderMetadata(
            decryptedFolderMetadata1,
            publicKey,
            folderID,
            user,
            arbitraryDataProvider);

        // store metadata key
        String oldMetadataKey = encryptedFolderMetadata1.getMetadata().getMetadataKey();

        // do it again
        // encrypt
        EncryptedFolderMetadataFileV1 encryptedFolderMetadata2 = encryptFolderMetadata(
            decryptedFolderMetadata1,
            publicKey,
            folderID,
            user,
            arbitraryDataProvider);

        String newMetadataKey = encryptedFolderMetadata2.getMetadata().getMetadataKey();

        assertNotEquals(oldMetadataKey, newMetadataKey);
    }

    @Test
    public void testMigrateMetadataKey() throws Exception {
        DecryptedFolderMetadataFileV1 decryptedFolderMetadata1 = generateFolderMetadataV1_1();
        long folderID = 1;

        // encrypt
        EncryptedFolderMetadataFileV1 encryptedFolderMetadata1 = encryptFolderMetadata(
            decryptedFolderMetadata1,
            publicKey,
            folderID,
            user,
            arbitraryDataProvider);

        // reset new metadata key, to mimic old version
        encryptedFolderMetadata1.getMetadata().setMetadataKey(null);
        String oldMetadataKey = encryptedFolderMetadata1.getMetadata().getMetadataKey();

        // do it again
        // encrypt
        EncryptedFolderMetadataFileV1 encryptedFolderMetadata2 = encryptFolderMetadata(
            decryptedFolderMetadata1,
            publicKey,
            folderID,
            user,
            arbitraryDataProvider);

        String newMetadataKey = encryptedFolderMetadata2.getMetadata().getMetadataKey();

        assertNotEquals(oldMetadataKey, newMetadataKey);
    }

    @Test
    public void testCryptFileWithoutMetadata() throws Exception {
        byte[] key = decodeStringToBase64Bytes("WANM0gRv+DhaexIsI0T3Lg==");
        byte[] iv = decodeStringToBase64Bytes("gKm3n+mJzeY26q4OfuZEqg==");

        assertTrue(cryptFile(filename, "78f42172166f9dc8fd1a7156b1753353", key, iv));
    }

    @Test
    public void cryptFileWithMetadata() throws Exception {
        DecryptedFolderMetadataFileV1 metadata = generateFolderMetadataV1_1();

        assertTrue(cryptFile(filename,
                             "78f42172166f9dc8fd1a7156b1753353",
                             decodeStringToBase64Bytes(metadata.getFiles().get(filename)
                                                           .getEncrypted().getKey()),
                             decodeStringToBase64Bytes(metadata.getFiles().get(filename)
                                                           .getInitializationVector())));

        assertTrue(cryptFile(secondFilename,
                             "825143ed1f21ebb0c3b3c3f005b2f5db",
                             decodeStringToBase64Bytes(metadata.getFiles().get(secondFilename)
                                                           .getEncrypted().getKey()),
                             decodeStringToBase64Bytes(metadata.getFiles().get(secondFilename)
                                                           .getInitializationVector())));
    }

    @Test
    public void bigMetadata() throws Exception {
        DecryptedFolderMetadataFileV1 decryptedFolderMetadata1 = generateFolderMetadataV1_1();
        long folderID = 1;

        // encrypt
        EncryptedFolderMetadataFileV1 encryptedFolderMetadata1 = encryptFolderMetadata(
            decryptedFolderMetadata1,
            publicKey,
            folderID,
            user,
            arbitraryDataProvider);

        // serialize
        String encryptedJson = serializeJSON(encryptedFolderMetadata1);

        // de-serialize
        EncryptedFolderMetadataFileV1 encryptedFolderMetadata2 = deserializeJSON(encryptedJson,
                                                                                 new TypeToken<>() {
                                                                                 });

        // decrypt
        DecryptedFolderMetadataFileV1 decryptedFolderMetadata2 = decryptFolderMetaData(
            encryptedFolderMetadata2,
            privateKey,
            arbitraryDataProvider,
            user,
            folderID);

        // compare
        assertTrue(compareJsonStrings(serializeJSON(decryptedFolderMetadata1),
                                      serializeJSON(decryptedFolderMetadata2)));

        // prefill with 500
        for (int i = 0; i < 500; i++) {
            addFile(decryptedFolderMetadata1, i);
        }

        int max = 505;
        for (int i = 500; i < max; i++) {
            Log_OC.d(this, "Big metadata: " + i + " of " + max);

            addFile(decryptedFolderMetadata1, i);

            // encrypt
            encryptedFolderMetadata1 = encryptFolderMetadata(decryptedFolderMetadata1,
                                                             publicKey,
                                                             folderID,
                                                             user,
                                                             arbitraryDataProvider);

            // serialize
            encryptedJson = serializeJSON(encryptedFolderMetadata1);

            // de-serialize
            encryptedFolderMetadata2 = deserializeJSON(encryptedJson,
                                                       new TypeToken<>() {
                                                       });

            // decrypt
            decryptedFolderMetadata2 = decryptFolderMetaData(encryptedFolderMetadata2,
                                                             privateKey,
                                                             arbitraryDataProvider,
                                                             user,
                                                             folderID);

            // compare
            assertTrue(compareJsonStrings(serializeJSON(decryptedFolderMetadata1),
                                          serializeJSON(decryptedFolderMetadata2)));

            assertEquals(i + 3, decryptedFolderMetadata1.getFiles().size());
            assertEquals(i + 3, decryptedFolderMetadata2.getFiles().size());
        }
    }

    @Test
    public void bigMetadata2() throws Exception {
        long folderID = 1;
        DecryptedFolderMetadataFileV1 decryptedFolderMetadata1 = generateFolderMetadataV1_1();

        // encrypt
        EncryptedFolderMetadataFileV1 encryptedFolderMetadata1 = encryptFolderMetadata(
            decryptedFolderMetadata1,
            publicKey,
            folderID,
            user,
            arbitraryDataProvider);

        // serialize
        String encryptedJson = serializeJSON(encryptedFolderMetadata1);

        // de-serialize
        EncryptedFolderMetadataFileV1 encryptedFolderMetadata2 = deserializeJSON(encryptedJson,
                                                                                 new TypeToken<>() {
                                                                                 });

        // decrypt
        DecryptedFolderMetadataFileV1 decryptedFolderMetadata2 = decryptFolderMetaData(
            encryptedFolderMetadata2,
            privateKey,
            arbitraryDataProvider,
            user,
            folderID);

        // compare
        assertTrue(compareJsonStrings(serializeJSON(decryptedFolderMetadata1),
                                      serializeJSON(decryptedFolderMetadata2)));

        // prefill with 500
        for (int i = 0; i < 500; i++) {
            addFile(decryptedFolderMetadata1, i);
        }

        int max = 505;
        for (int i = 500; i < max; i++) {
            Log_OC.d(this, "Big metadata: " + i + " of " + max);

            addFile(decryptedFolderMetadata1, i);

            // encrypt
            encryptedFolderMetadata1 = encryptFolderMetadata(
                decryptedFolderMetadata1,
                publicKey,
                folderID,
                user,
                arbitraryDataProvider);

            // serialize
            encryptedJson = serializeJSON(encryptedFolderMetadata1);

            // de-serialize
            encryptedFolderMetadata2 = deserializeJSON(encryptedJson,
                                                       new TypeToken<>() {
                                                       });

            // decrypt
            decryptedFolderMetadata2 = decryptFolderMetaData(
                encryptedFolderMetadata2,
                privateKey,
                arbitraryDataProvider,
                user,
                folderID);

            // compare
            assertTrue(compareJsonStrings(serializeJSON(decryptedFolderMetadata1),
                                          serializeJSON(decryptedFolderMetadata2)));

            assertEquals(i + 3, decryptedFolderMetadata1.getFiles().size());
            assertEquals(i + 3, decryptedFolderMetadata2.getFiles().size());
        }
    }

    @Test
    public void filedrop() throws Exception {
        DecryptedFolderMetadataFileV1 decryptedFolderMetadata1 = generateFolderMetadataV1_1();
        long folderID = 1;

        // add filedrop
        Map<String, DecryptedFile> filesdrop = new HashMap<>();

        Data data = new Data();
        data.setKey("9dfzbIYDt28zTyZfbcll+g==");
        data.setFilename("test2.txt");
        data.setVersion(1);

        DecryptedFile file = new DecryptedFile();
        file.setInitializationVector("hnJLF8uhDvDoFK4ajuvwrg==");
        file.setEncrypted(data);
        file.setMetadataKey(0);
        file.setAuthenticationTag("qOQZdu5soFO77Y7y4rAOVA==");

        filesdrop.put("eie8iaeiaes8e87td6", file);

        decryptedFolderMetadata1.setFiledrop(filesdrop);

        // encrypt
        EncryptedFolderMetadataFileV1 encryptedFolderMetadata1 = encryptFolderMetadata(
            decryptedFolderMetadata1,
            publicKey,
            folderID,
            user,
            arbitraryDataProvider);
        EncryptionUtils.encryptFileDropFiles(decryptedFolderMetadata1, encryptedFolderMetadata1, publicKey);

        // serialize
        String encryptedJson = serializeJSON(encryptedFolderMetadata1, true);

        // de-serialize
        EncryptedFolderMetadataFileV1 encryptedFolderMetadata2 = deserializeJSON(encryptedJson,
                                                                                 new TypeToken<>() {
                                                                                 });

        // decrypt
        DecryptedFolderMetadataFileV1 decryptedFolderMetadata2 = decryptFolderMetaData(
            encryptedFolderMetadata2,
            privateKey,
            arbitraryDataProvider,
            user,
            folderID);

        // compare
        assertFalse(compareJsonStrings(serializeJSON(decryptedFolderMetadata1, true),
                                       serializeJSON(decryptedFolderMetadata2, true)));

        assertEquals(decryptedFolderMetadata1.getFiles().size() + decryptedFolderMetadata1.getFiledrop().size(),
                     decryptedFolderMetadata2.getFiles().size());

        // no filedrop content means null
        assertNull(decryptedFolderMetadata2.getFiledrop());
    }

    private void addFile(DecryptedFolderMetadataFileV1 decryptedFolderMetadata, int counter) {
        // Add new file
        // Always generate new
        byte[] key = generateKey();
        byte[] iv = randomBytes(ivLength);
        byte[] authTag = randomBytes((128 / 8));

        Data data = new Data();
        data.setKey(EncryptionUtils.encodeBytesToBase64String(key));
        data.setFilename(counter + ".txt");
        data.setVersion(1);

        DecryptedFile file = new DecryptedFile();
        file.setInitializationVector(EncryptionUtils.encodeBytesToBase64String(iv));
        file.setEncrypted(data);
        file.setMetadataKey(0);
        file.setAuthenticationTag(EncryptionUtils.encodeBytesToBase64String(authTag));

        decryptedFolderMetadata.getFiles().put(RandomStringGenerator.make(20), file);
    }

    /**
     * generates new keys and tests if they are unique
     */
    @Test
    public void testKey() {
        Set<String> keys = new HashSet<>();

        for (int i = 0; i < 50; i++) {
            assertTrue(keys.add(encodeBytesToBase64String(generateKey())));
        }
    }

    /**
     * generates new ivs and tests if they are unique
     */
    @Test
    public void testIV() {
        Set<String> ivs = new HashSet<>();

        for (int i = 0; i < 50; i++) {
            assertTrue(ivs.add(encodeBytesToBase64String(
                randomBytes(ivLength))));
        }
    }

    /**
     * generates new salt and tests if they are unique
     */
    @Test
    public void testSalt() {
        Set<String> ivs = new HashSet<>();

        for (int i = 0; i < 50; i++) {
            assertTrue(ivs.add(encodeBytesToBase64String(
                randomBytes(saltLength))));
        }
    }

    @Test
    public void testSHA512() {
        // sent to 3rd party app in cleartext
        String token = "4ae5978bf5354cd284b539015d442141";
        String salt = encodeBytesToBase64String(randomBytes(saltLength));

        // stored in database
        String hashedToken = generateSHA512(token, salt);

        // check: use passed cleartext and salt to verify hashed token
        assertTrue(verifySHA512(hashedToken, token));
    }

    @Test
    public void testExcludeGSON() throws Exception {
        DecryptedFolderMetadataFileV1 metadata = generateFolderMetadataV1_1();

        String jsonWithKeys = serializeJSON(metadata);
        String jsonWithoutKeys = serializeJSON(metadata, true);

        assertTrue(jsonWithKeys.contains("metadataKeys"));
        assertFalse(jsonWithoutKeys.contains("metadataKeys"));
    }
    
    @Test
    public void testEqualsSign() {
        assertEquals("\"===\"", serializeJSON("==="));
    }
    
    @Test
    public void testBase64() {
        String originalString = "randomstring123";

        String encodedString = EncryptionUtils.encodeStringToBase64String(originalString);
        String compare = EncryptionUtils.decodeBase64StringToString(encodedString);
        assertEquals(originalString, compare);
    }

    @Test
    public void testChecksum() throws Exception {
        DecryptedFolderMetadataFileV1 metadata = new DecryptedFolderMetadataFileV1();
        String mnemonic = "chimney potato joke science ridge trophy result estate spare vapor much room";

        metadata.getFiles().put(secondFilename, new DecryptedFile());
        metadata.getFiles().put(filename, new DecryptedFile());

        String encryptedMetadataKey = "GuFPAULudgD49S4+VDFck3LiqQ8sx4zmbrBtdpCSGcT+T0W0z4F5gYQYPlzTG6WOkdW5LJZK/";
        metadata.getMetadata().setMetadataKey(encryptedMetadataKey);

        String checksum = generateChecksum(metadata, mnemonic);

        String expectedChecksum = "002cefa6493f2efb0192247a34bb1b16d391aefee968fd3d4225c4ec3cd56436";
        assertEquals(expectedChecksum, checksum);

        // change something
        String newMnemonic = mnemonic + "1";

        String newChecksum = generateChecksum(metadata, newMnemonic);
        assertNotEquals(expectedChecksum, newChecksum);

        metadata.getFiles().put("aeb34yXMoRa1QWQk8r", new DecryptedFile());

        newChecksum = generateChecksum(metadata, mnemonic);
        assertNotEquals(expectedChecksum, newChecksum);
    }

    @Test
    public void testAddIdToMigratedIds() {
        // delete ids
        arbitraryDataProvider.deleteKeyForAccount(user.getAccountName(), EncryptionUtils.MIGRATED_FOLDER_IDS);

        long id = 1;
        EncryptionUtils.addIdToMigratedIds(id, user, arbitraryDataProvider);

        assertTrue(isFolderMigrated(id, user, arbitraryDataProvider));
    }
    
    // TODO E2E: more tests 
    
    // more tests
    // migrate v1 -> v2
    // migrate v1 -> v2 with filedrop
    
    // migrate v1 -> v1.1
    // migrate v1 -> v1.1 with filedrop
    
    // migrate v1.1 -> v2
    // migrate v1.1 -> v2 with filedrop


    // Helper
    public static boolean compareJsonStrings(String expected, String actual) {
        JsonElement o1 = JsonParser.parseString(expected);
        JsonElement o2 = JsonParser.parseString(actual);

        if (o1.equals(o2)) {
            return true;
        } else {
            System.out.println("expected: " + o1);
            System.out.println("actual: " + o2);
            return false;
        }
    }

    private DecryptedFolderMetadataFileV1 generateFolderMetadataV1_1() throws Exception {
        String metadataKey0 = encodeBytesToBase64String(generateKey());
        String metadataKey1 = encodeBytesToBase64String(generateKey());
        String metadataKey2 = encodeBytesToBase64String(generateKey());
        HashMap<Integer, String> metadataKeys = new HashMap<>();
        metadataKeys.put(0, EncryptionUtils.encryptStringAsymmetric(metadataKey0, publicKey));
        metadataKeys.put(1, EncryptionUtils.encryptStringAsymmetric(metadataKey1, publicKey));
        metadataKeys.put(2, EncryptionUtils.encryptStringAsymmetric(metadataKey2, publicKey));
        Encrypted encrypted = new Encrypted();
        encrypted.setMetadataKeys(metadataKeys);

        DecryptedMetadata metadata1 = new DecryptedMetadata();
        metadata1.setMetadataKeys(metadataKeys);
        metadata1.setVersion(1);

        HashMap<String, DecryptedFile> files = new HashMap<>();

        Data data1 = new Data();
        data1.setKey("WANM0gRv+DhaexIsI0T3Lg==");
        data1.setFilename("test.txt");
        data1.setVersion(1);

        DecryptedFile file1 = new DecryptedFile();
        file1.setInitializationVector("gKm3n+mJzeY26q4OfuZEqg==");
        file1.setEncrypted(data1);
        file1.setMetadataKey(0);
        file1.setAuthenticationTag("PboI9tqHHX3QeAA22PIu4w==");

        files.put(filename, file1);

        Data data2 = new Data();
        data2.setKey("9dfzbIYDt28zTyZfbcll+g==");
        data2.setFilename("test2.txt");
        data2.setVersion(1);

        DecryptedFile file2 = new DecryptedFile();
        file2.setInitializationVector("hnJLF8uhDvDoFK4ajuvwrg==");
        file2.setEncrypted(data2);
        file2.setMetadataKey(0);
        file2.setAuthenticationTag("qOQZdu5soFO77Y7y4rAOVA==");

        files.put(secondFilename, file2);

        return new DecryptedFolderMetadataFileV1(metadata1, files);
    }

    private boolean cryptFile(String fileName, String md5, byte[] key, byte[] iv)
        throws Exception {
        File file = File.createTempFile(fileName, "enc");
        String md5BeforeEncryption = getMD5Sum(file);

        // Encryption
        Cipher encryptorCipher = EncryptionUtils.getCipher(Cipher.ENCRYPT_MODE, key, iv);
        EncryptionUtils.encryptFile(user.getAccountName(), file, encryptorCipher);
        String encryptorCipherAuthTag = EncryptionUtils.getAuthenticationTag(encryptorCipher);

        // Decryption
        Cipher decryptorCipher = EncryptionUtils.getCipher(Cipher.DECRYPT_MODE, key, iv);
        File decryptedFile = File.createTempFile("file", "dec");
        decryptFile(decryptorCipher, file, decryptedFile, encryptorCipherAuthTag, new ArbitraryDataProviderImpl(targetContext), user);

        String md5AfterEncryption = getMD5Sum(decryptedFile);

        if (md5BeforeEncryption == null) {
            Assert.fail();
        }

        return md5BeforeEncryption.equals(md5AfterEncryption);
    }

    public static String getMD5Sum(File file) {
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest md = MessageDigest.getInstance(MD5_ALGORITHM);
            DigestInputStream dis = new DigestInputStream(fis, md);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = dis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            byte[] digest = md.digest();
            return bytesToHex(digest);
        } catch (IOException | NoSuchAlgorithmException e) {
            return null;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
