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

package com.owncloud.android.util;

import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.test.runner.AndroidJUnit4;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.owncloud.android.datamodel.DecryptedFolderMetadata;
import com.owncloud.android.datamodel.EncryptedFolderMetadata;
import com.owncloud.android.utils.CsrHelper;
import com.owncloud.android.utils.EncryptionUtils;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
@RunWith(AndroidJUnit4.class)
public class EncryptionTestIT {
    private String privateKey = "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAo" +
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

    private String cert = "-----BEGIN CERTIFICATE-----\n" +
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
        byte[] key1 = EncryptionUtils.generateKey();
        String base64encodedKey = EncryptionUtils.encodeBytesToBase64String(key1);

        String encryptedString = EncryptionUtils.encryptStringAsymmetric(base64encodedKey, cert);
        String decryptedString = EncryptionUtils.decryptStringAsymmetric(encryptedString, privateKey);

        byte[] key2 = EncryptionUtils.decodeStringToBase64Bytes(decryptedString);

        assertTrue(Arrays.equals(key1, key2));
    }

    @Test
    public void encryptStringSymmetric() throws Exception {
        byte[] key = EncryptionUtils.generateKey();

        String encryptedString = EncryptionUtils.encryptStringSymmetric(privateKey, key);
        String decryptedString = EncryptionUtils.decryptStringSymmetric(encryptedString, key);

        assertEquals(privateKey, decryptedString);
    }

    @Test
    public void encryptPrivateKey() throws Exception {
        String keyPhrase = "moreovertelevisionfactorytendencyindependenceinternationalintellectualimpress" +
                "interestvolunteer";
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(4096, new SecureRandom());
        KeyPair keyPair = keyGen.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        byte[] privateKeyBytes = privateKey.getEncoded();
        String privateKeyString = EncryptionUtils.encodeBytesToBase64String(privateKeyBytes);

        String encryptedString = EncryptionUtils.encryptPrivateKey(privateKeyString, keyPhrase);
        String decryptedString = EncryptionUtils.decryptPrivateKey(encryptedString, keyPhrase);

        assertEquals(privateKeyString, decryptedString);
    }

    @Test
    public void generateCSR() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048, new SecureRandom());
        KeyPair keyPair = keyGen.generateKeyPair();

        assertFalse(CsrHelper.generateCsrPemEncodedString(keyPair, "").isEmpty());
        assertFalse(EncryptionUtils.encodeBytesToBase64String(keyPair.getPublic().getEncoded()).isEmpty());
    }

    /**
     * DecryptedFolderMetadata -> EncryptedFolderMetadata -> JSON -> encrypt
     * -> decrypt -> JSON -> EncryptedFolderMetadata -> DecryptedFolderMetadata
     */
    @Test
    public void encryptionMetadata() throws Exception {
        DecryptedFolderMetadata decryptedFolderMetadata1 = generateFolderMetadata();

        // encrypt
        EncryptedFolderMetadata encryptedFolderMetadata1 = EncryptionUtils.encryptFolderMetadata(
                decryptedFolderMetadata1, privateKey);

        // serialize
        String encryptedJson = EncryptionUtils.serializeJSON(encryptedFolderMetadata1);

        // de-serialize
        EncryptedFolderMetadata encryptedFolderMetadata2 = EncryptionUtils.deserializeJSON(encryptedJson,
                new TypeToken<EncryptedFolderMetadata>() {
                });

        // decrypt
        DecryptedFolderMetadata decryptedFolderMetadata2 = EncryptionUtils.decryptFolderMetaData(
                encryptedFolderMetadata2, privateKey);

        // compare
        assertTrue(compareJsonStrings(EncryptionUtils.serializeJSON(decryptedFolderMetadata1),
                EncryptionUtils.serializeJSON(decryptedFolderMetadata2)));
    }

    @Test
    public void testCryptFileWithoutMetadata() throws Exception {
        byte[] key = EncryptionUtils.decodeStringToBase64Bytes("WANM0gRv+DhaexIsI0T3Lg==");
        byte[] iv = EncryptionUtils.decodeStringToBase64Bytes("gKm3n+mJzeY26q4OfuZEqg==");
        byte[] authTag = EncryptionUtils.decodeStringToBase64Bytes("PboI9tqHHX3QeAA22PIu4w==");

        assertTrue(cryptFile("ia7OEEEyXMoRa1QWQk8r", "78f42172166f9dc8fd1a7156b1753353", key, iv, authTag));
    }

    @Test
    public void cryptFileWithMetadata() throws Exception {
        DecryptedFolderMetadata metadata = generateFolderMetadata();

        // n9WXAIXO2wRY4R8nXwmo
        assertTrue(cryptFile("ia7OEEEyXMoRa1QWQk8r",
                "78f42172166f9dc8fd1a7156b1753353",
                EncryptionUtils.decodeStringToBase64Bytes(metadata.getFiles().get("ia7OEEEyXMoRa1QWQk8r")
                        .getEncrypted().getKey()),
                EncryptionUtils.decodeStringToBase64Bytes(metadata.getFiles().get("ia7OEEEyXMoRa1QWQk8r")
                        .getInitializationVector()),
                EncryptionUtils.decodeStringToBase64Bytes(metadata.getFiles().get("ia7OEEEyXMoRa1QWQk8r")
                        .getAuthenticationTag())));

        // n9WXAIXO2wRY4R8nXwmo
        assertTrue(cryptFile("n9WXAIXO2wRY4R8nXwmo",
                "825143ed1f21ebb0c3b3c3f005b2f5db",
                EncryptionUtils.decodeStringToBase64Bytes(metadata.getFiles().get("n9WXAIXO2wRY4R8nXwmo")
                        .getEncrypted().getKey()),
                EncryptionUtils.decodeStringToBase64Bytes(metadata.getFiles().get("n9WXAIXO2wRY4R8nXwmo")
                        .getInitializationVector()),
                EncryptionUtils.decodeStringToBase64Bytes(metadata.getFiles().get("n9WXAIXO2wRY4R8nXwmo")
                        .getAuthenticationTag())));
    }

    /**
     * generates new keys and tests if they are unique
     */
    @Test
    public void testKey() {
        Set<String> keys = new HashSet<>();

        for (int i = 0; i < 50; i++) {
            assertTrue(keys.add(EncryptionUtils.encodeBytesToBase64String(EncryptionUtils.generateKey())));
        }
    }

    /**
     * generates new ivs and tests if they are unique
     */
    @Test
    public void testIV() {
        Set<String> ivs = new HashSet<>();

        for (int i = 0; i < 50; i++) {
            assertTrue(ivs.add(EncryptionUtils.encodeBytesToBase64String(
                    EncryptionUtils.randomBytes(EncryptionUtils.ivLength))));
        }
    }

    /**
     * generates new salt and tests if they are unique
     */
    @Test
    public void testSalt() {
        Set<String> ivs = new HashSet<>();

        for (int i = 0; i < 50; i++) {
            assertTrue(ivs.add(EncryptionUtils.encodeBytesToBase64String(
                    EncryptionUtils.randomBytes(EncryptionUtils.saltLength))));
        }
    }


    // Helper
    private boolean compareJsonStrings(String expected, String actual) {
        JsonParser parser = new JsonParser();
        JsonElement o1 = parser.parse(expected);
        JsonElement o2 = parser.parse(actual);

        if (o1.equals(o2)) {
            return true;
        } else {
            System.out.println("expected: " + o1);
            System.out.println("actual: " + o2);
            return false;
        }
    }

    private DecryptedFolderMetadata generateFolderMetadata() throws Exception {
        String metadataKey0 = EncryptionUtils.encodeBytesToBase64String(EncryptionUtils.generateKey());
        String metadataKey1 = EncryptionUtils.encodeBytesToBase64String(EncryptionUtils.generateKey());
        String metadataKey2 = EncryptionUtils.encodeBytesToBase64String(EncryptionUtils.generateKey());
        HashMap<Integer, String> metadataKeys = new HashMap<>();
        metadataKeys.put(0, EncryptionUtils.encryptStringAsymmetric(metadataKey0, cert));
        metadataKeys.put(1, EncryptionUtils.encryptStringAsymmetric(metadataKey1, cert));
        metadataKeys.put(2, EncryptionUtils.encryptStringAsymmetric(metadataKey2, cert));
        DecryptedFolderMetadata.Encrypted encrypted = new DecryptedFolderMetadata.Encrypted();
        encrypted.setMetadataKeys(metadataKeys);

        DecryptedFolderMetadata.Metadata metadata1 = new DecryptedFolderMetadata.Metadata();
        metadata1.setMetadataKeys(metadataKeys);
        metadata1.setVersion(1);

        DecryptedFolderMetadata.Sharing sharing = new DecryptedFolderMetadata.Sharing();
        sharing.setSignature("HMACOFRECIPIENTANDNEWESTMETADATAKEY");
        HashMap<String, String> recipient = new HashMap<>();
        recipient.put("blah@schiessle.org", "PUBLIC KEY");
        recipient.put("bjoern@schiessle.org", "PUBLIC KEY");
        sharing.setRecipient(recipient);
        metadata1.setSharing(sharing);

        HashMap<String, DecryptedFolderMetadata.DecryptedFile> files = new HashMap<>();

        DecryptedFolderMetadata.Data data1 = new DecryptedFolderMetadata.Data();
        data1.setKey("WANM0gRv+DhaexIsI0T3Lg==");
        data1.setFilename("test.txt");
        data1.setVersion(1);

        DecryptedFolderMetadata.DecryptedFile file1 = new DecryptedFolderMetadata.DecryptedFile();
        file1.setInitializationVector("gKm3n+mJzeY26q4OfuZEqg==");
        file1.setEncrypted(data1);
        file1.setMetadataKey(0);
        file1.setAuthenticationTag("PboI9tqHHX3QeAA22PIu4w==");

        files.put("ia7OEEEyXMoRa1QWQk8r", file1);

        DecryptedFolderMetadata.Data data2 = new DecryptedFolderMetadata.Data();
        data2.setKey("9dfzbIYDt28zTyZfbcll+g==");
        data2.setFilename("test2.txt");
        data2.setVersion(1);

        DecryptedFolderMetadata.DecryptedFile file2 = new DecryptedFolderMetadata.DecryptedFile();
        file2.setInitializationVector("hnJLF8uhDvDoFK4ajuvwrg==");
        file2.setEncrypted(data2);
        file2.setMetadataKey(0);
        file2.setAuthenticationTag("qOQZdu5soFO77Y7y4rAOVA==");

        files.put("n9WXAIXO2wRY4R8nXwmo", file2);

        return new DecryptedFolderMetadata(metadata1, files);
    }

    private boolean cryptFile(String fileName, String md5, byte[] key, byte[] iv, byte[] expectedAuthTag)
            throws Exception {
        File file = getFile(fileName);
        assertEquals(md5, EncryptionUtils.getMD5Sum(file));

        EncryptionUtils.EncryptedFile encryptedFile = EncryptionUtils.encryptFile(file, key, iv);

        File encryptedTempFile = File.createTempFile("file", "tmp");
        FileOutputStream fileOutputStream = new FileOutputStream(encryptedTempFile);
        fileOutputStream.write(encryptedFile.encryptedBytes);
        fileOutputStream.close();

        byte[] authenticationTag = EncryptionUtils.decodeStringToBase64Bytes(encryptedFile.authenticationTag);

        // verify authentication tag
        assertTrue(Arrays.equals(expectedAuthTag, authenticationTag));

        byte[] decryptedBytes = EncryptionUtils.decryptFile(encryptedTempFile, key, iv, authenticationTag);

        File decryptedFile = File.createTempFile("file", "dec");
        FileOutputStream fileOutputStream1 = new FileOutputStream(decryptedFile);
        fileOutputStream1.write(decryptedBytes);
        fileOutputStream1.close();

        return md5.compareTo(EncryptionUtils.getMD5Sum(decryptedFile)) == 0;
    }

    private File getFile(String filename) throws IOException {
        InputStream inputStream = getInstrumentation().getContext().getAssets().open(filename);
        File temp = File.createTempFile("file", "file");
        FileUtils.copyInputStreamToFile(inputStream, temp);

        return temp;
    }
}