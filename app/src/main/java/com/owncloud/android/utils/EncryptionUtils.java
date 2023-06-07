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

package com.owncloud.android.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Pair;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.nextcloud.client.account.User;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl;
import com.owncloud.android.datamodel.DecryptedFolderMetadata;
import com.owncloud.android.datamodel.EncryptedFiledrop;
import com.owncloud.android.datamodel.EncryptedFolderMetadata;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.e2ee.GetMetadataRemoteOperation;
import com.owncloud.android.lib.resources.e2ee.LockFileRemoteOperation;
import com.owncloud.android.lib.resources.e2ee.StoreMetadataRemoteOperation;
import com.owncloud.android.lib.resources.e2ee.UnlockFileRemoteOperation;
import com.owncloud.android.lib.resources.e2ee.UpdateMetadataRemoteOperation;
import com.owncloud.android.lib.resources.status.NextcloudVersion;
import com.owncloud.android.operations.UploadException;

import org.apache.commons.httpclient.HttpStatus;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Utils for encryption
 */
public final class EncryptionUtils {
    private static final String TAG = EncryptionUtils.class.getSimpleName();

    public static final String PUBLIC_KEY = "PUBLIC_KEY";
    public static final String PRIVATE_KEY = "PRIVATE_KEY";
    public static final String MNEMONIC = "MNEMONIC";
    public static final int ivLength = 16;
    public static final int saltLength = 40;
    public static final String ivDelimiter = "|"; // not base64 encoded
    public static final String ivDelimiterOld = "fA=="; // "|" base64 encoded

    private static final String HASH_DELIMITER = "$";
    private static final int iterationCount = 1024;
    private static final int keyStrength = 256;
    private static final String AES_CIPHER = "AES/GCM/NoPadding";
    private static final String AES = "AES";
    private static final String RSA_CIPHER = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final String RSA = "RSA";
    @VisibleForTesting
    public static final String MIGRATED_FOLDER_IDS = "MIGRATED_FOLDER_IDS";

    private EncryptionUtils() {
        // utility class -> private constructor
    }

    /*
    JSON
     */

    public static <T> T deserializeJSON(String json, TypeToken<T> type, boolean excludeTransient) {
        if (excludeTransient) {
            return new Gson().fromJson(json, type.getType());
        } else {
            return new GsonBuilder().excludeFieldsWithModifiers(0).create().fromJson(json, type.getType());
        }
    }

    public static <T> T deserializeJSON(String json, TypeToken<T> type) {
        return deserializeJSON(json, type, false);
    }

    public static String serializeJSON(Object data, boolean excludeTransient) {
        if (excludeTransient) {
            return new Gson().toJson(data);
        } else {
            return new GsonBuilder().excludeFieldsWithModifiers(0).create().toJson(data);
        }
    }

    public static String serializeJSON(Object data) {
        return serializeJSON(data, false);
    }

    /*
    METADATA
     */

    /**
     * Encrypt folder metaData
     *
     * @param decryptedFolderMetadata folder metaData to encrypt
     * @return EncryptedFolderMetadata encrypted folder metadata
     */
    public static EncryptedFolderMetadata encryptFolderMetadata(DecryptedFolderMetadata decryptedFolderMetadata,
                                                                String publicKey,
                                                                ArbitraryDataProvider arbitraryDataProvider,
                                                                User user,
                                                                long parentId
                                                               )
        throws NoSuchAlgorithmException, InvalidKeyException,
        InvalidAlgorithmParameterException, NoSuchPaddingException, BadPaddingException,
        IllegalBlockSizeException, InvalidKeySpecException, CertificateException {

        HashMap<String, EncryptedFolderMetadata.EncryptedFile> files = new HashMap<>();
        HashMap<String, EncryptedFiledrop> filesdrop = new HashMap<>();
        EncryptedFolderMetadata encryptedFolderMetadata = new EncryptedFolderMetadata(decryptedFolderMetadata
                                                                                          .getMetadata(),
                                                                                      files,
                                                                                      filesdrop);

        // set new metadata key
        byte[] metadataKeyBytes = EncryptionUtils.generateKey();
        String encryptedMetadataKey = EncryptionUtils.encryptStringAsymmetric(
            EncryptionUtils.encodeBytesToBase64String(metadataKeyBytes),
            publicKey);
        encryptedFolderMetadata.getMetadata().setMetadataKey(encryptedMetadataKey);

        // store that this folder has  been migrated
        addIdToMigratedIds(parentId, user, arbitraryDataProvider);

        // Encrypt each file in "files"
        for (Map.Entry<String, DecryptedFolderMetadata.DecryptedFile> entry : decryptedFolderMetadata
            .getFiles().entrySet()) {
            String key = entry.getKey();
            DecryptedFolderMetadata.DecryptedFile decryptedFile = entry.getValue();

            EncryptedFolderMetadata.EncryptedFile encryptedFile = new EncryptedFolderMetadata.EncryptedFile();
            encryptedFile.setInitializationVector(decryptedFile.getInitializationVector());
            encryptedFile.setAuthenticationTag(decryptedFile.getAuthenticationTag());

            // encrypt
            String dataJson = EncryptionUtils.serializeJSON(decryptedFile.getEncrypted());
            encryptedFile.setEncrypted(EncryptionUtils.encryptStringSymmetric(dataJson, metadataKeyBytes));

            files.put(key, encryptedFile);
        }

        // set checksum
        String mnemonic = arbitraryDataProvider.getValue(user.getAccountName(), EncryptionUtils.MNEMONIC);
        String checksum = EncryptionUtils.generateChecksum(decryptedFolderMetadata, mnemonic);
        encryptedFolderMetadata.getMetadata().setChecksum(checksum);

        return encryptedFolderMetadata;
    }

    /**
     * normally done on server only internal test
     */
    @VisibleForTesting
    public static void encryptFileDropFiles(DecryptedFolderMetadata decryptedFolderMetadata,
                                            EncryptedFolderMetadata encryptedFolderMetadata,
                                            String cert) throws NoSuchPaddingException, IllegalBlockSizeException, CertificateException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        final Map<String, EncryptedFiledrop> filesdrop = encryptedFolderMetadata.getFiledrop();
        for (Map.Entry<String, DecryptedFolderMetadata.DecryptedFile> entry : decryptedFolderMetadata
            .getFiledrop().entrySet()) {
            String key = entry.getKey();
            DecryptedFolderMetadata.DecryptedFile decryptedFile = entry.getValue();

            byte[] byt = generateKey();
            String metadataKey0 = encodeBytesToBase64String(byt);
            String enc = encryptStringAsymmetric(metadataKey0, cert);

            String dataJson = EncryptionUtils.serializeJSON(decryptedFile.getEncrypted());

            String encJson = encryptStringSymmetric(dataJson, byt);

            int delimiterPosition = encJson.lastIndexOf(ivDelimiter);
            String encryptedInitializationVector = encJson.substring(delimiterPosition + ivDelimiter.length());
            String encodedCryptedBytes = encJson.substring(0, delimiterPosition);


            byte[] bytes = decodeStringToBase64Bytes(encodedCryptedBytes);

            // check authentication tag
            byte[] extractedAuthenticationTag = Arrays.copyOfRange(bytes,
                                                                   bytes.length - (128 / 8),
                                                                   bytes.length);

            String encryptedTag = encodeBytesToBase64String(extractedAuthenticationTag);

            EncryptedFiledrop encryptedFile = new EncryptedFiledrop(encodedCryptedBytes,
                                                                    decryptedFile.getInitializationVector(),
                                                                    decryptedFile.getAuthenticationTag(),
                                                                    enc,
                                                                    encryptedTag,
                                                                    encryptedInitializationVector);

            filesdrop.put(key, encryptedFile);
        }
    }

    /*
     * decrypt folder metaData with private key
     */
    public static DecryptedFolderMetadata decryptFolderMetaData(EncryptedFolderMetadata encryptedFolderMetadata,
                                                                String privateKey,
                                                                ArbitraryDataProvider arbitraryDataProvider,
                                                                User user,
                                                                long remoteId)
        throws NoSuchAlgorithmException, InvalidKeyException,
        InvalidAlgorithmParameterException, NoSuchPaddingException, BadPaddingException,
        IllegalBlockSizeException, InvalidKeySpecException {

        HashMap<String, DecryptedFolderMetadata.DecryptedFile> files = new HashMap<>();
        DecryptedFolderMetadata decryptedFolderMetadata = new DecryptedFolderMetadata(
            encryptedFolderMetadata.getMetadata(), files);

        byte[] decryptedMetadataKey = null;

        String encryptedMetadataKey = decryptedFolderMetadata.getMetadata().getMetadataKey();

        if (encryptedMetadataKey != null) {
            decryptedMetadataKey = decodeStringToBase64Bytes(
                decryptStringAsymmetric(encryptedMetadataKey, privateKey));
        }

        if (encryptedFolderMetadata.getFiles() != null) {
            for (Map.Entry<String, EncryptedFolderMetadata.EncryptedFile> entry : encryptedFolderMetadata
                .getFiles().entrySet()) {
                String key = entry.getKey();
                EncryptedFolderMetadata.EncryptedFile encryptedFile = entry.getValue();

                DecryptedFolderMetadata.DecryptedFile decryptedFile = new DecryptedFolderMetadata.DecryptedFile();
                decryptedFile.setInitializationVector(encryptedFile.getInitializationVector());
                decryptedFile.setMetadataKey(encryptedFile.getMetadataKey());
                decryptedFile.setAuthenticationTag(encryptedFile.getAuthenticationTag());

                if (decryptedMetadataKey == null) {
                    decryptedMetadataKey = EncryptionUtils.decodeStringToBase64Bytes(
                        decryptStringAsymmetric(decryptedFolderMetadata.getMetadata()
                                                    .getMetadataKeys().get(encryptedFile.getMetadataKey()),
                                                privateKey));
                }

                // decrypt
                String dataJson = EncryptionUtils.decryptStringSymmetric(encryptedFile.getEncrypted(), decryptedMetadataKey);
                decryptedFile.setEncrypted(EncryptionUtils.deserializeJSON(dataJson,
                                                                           new TypeToken<DecryptedFolderMetadata.Data>() {
                                                                           }));

                files.put(key, decryptedFile);
            }
        }

        // verify checksum
        String mnemonic = arbitraryDataProvider.getValue(user.getAccountName(), EncryptionUtils.MNEMONIC);
        String checksum = EncryptionUtils.generateChecksum(decryptedFolderMetadata, mnemonic);
        String decryptedFolderChecksum = decryptedFolderMetadata.getMetadata().getChecksum();

        if (TextUtils.isEmpty(decryptedFolderChecksum) &&
            isFolderMigrated(remoteId, user, arbitraryDataProvider)) {
            throw new IllegalStateException("Possible downgrade attack detected!");
        }

        if (!TextUtils.isEmpty(decryptedFolderChecksum) && !decryptedFolderChecksum.equals(checksum)) {
            throw new IllegalStateException("Wrong checksum!");
        }

        Map<String, EncryptedFiledrop> fileDrop = encryptedFolderMetadata.getFiledrop();

        if (fileDrop != null) {
            for (Map.Entry<String, EncryptedFiledrop> entry : fileDrop.entrySet()) {
                String key = entry.getKey();
                EncryptedFiledrop encryptedFile = entry.getValue();

                // decrypt key
                String encryptedKey = decryptStringAsymmetric(encryptedFile.getEncryptedKey(),
                                                              privateKey);

                // decrypt encrypted blob with key
                String decryptedData = decryptStringSymmetric(
                    encryptedFile.getEncrypted(),
                    decodeStringToBase64Bytes(encryptedKey),
                    decodeStringToBase64Bytes(encryptedFile.getEncryptedInitializationVector()),
                    decodeStringToBase64Bytes(encryptedFile.getEncryptedTag())
                                                             );

                DecryptedFolderMetadata.DecryptedFile decryptedFile = new DecryptedFolderMetadata.DecryptedFile();
                decryptedFile.setInitializationVector(encryptedFile.getInitializationVector());
                decryptedFile.setAuthenticationTag(encryptedFile.getAuthenticationTag());


                decryptedFile.setEncrypted(EncryptionUtils.deserializeJSON(decryptedData,
                                                                           new TypeToken<DecryptedFolderMetadata.Data>() {
                                                                           }));

                files.put(key, decryptedFile);

                // remove from filedrop
                fileDrop.remove(key);
            }
        }

        return decryptedFolderMetadata;
    }

    /**
     * Download metadata for folder and decrypt it
     *
     * @return decrypted metadata or null
     */
    public static @Nullable
    DecryptedFolderMetadata downloadFolderMetadata(OCFile folder,
                                                   OwnCloudClient client,
                                                   Context context,
                                                   User user) {
        RemoteOperationResult getMetadataOperationResult = new GetMetadataRemoteOperation(folder.getLocalId())
            .execute(client);

        if (!getMetadataOperationResult.isSuccess()) {
            return null;
        }

        // decrypt metadata
        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProviderImpl(context);
        String serializedEncryptedMetadata = (String) getMetadataOperationResult.getData().get(0);
        String privateKey = arbitraryDataProvider.getValue(user.getAccountName(), EncryptionUtils.PRIVATE_KEY);
        String publicKey = arbitraryDataProvider.getValue(user.getAccountName(), EncryptionUtils.PUBLIC_KEY);

        EncryptedFolderMetadata encryptedFolderMetadata = EncryptionUtils.deserializeJSON(
            serializedEncryptedMetadata, new TypeToken<EncryptedFolderMetadata>() {
            });

        try {
            int filesDropCountBefore = 0;
            if (encryptedFolderMetadata.getFiledrop() != null) {
                filesDropCountBefore = encryptedFolderMetadata.getFiledrop().size();
            }
            DecryptedFolderMetadata decryptedFolderMetadata = EncryptionUtils.decryptFolderMetaData(
                encryptedFolderMetadata,
                privateKey,
                arbitraryDataProvider,
                user,
                folder.getLocalId());

            boolean transferredFiledrop = filesDropCountBefore > 0 && decryptedFolderMetadata.getFiles().size() ==
                encryptedFolderMetadata.getFiles().size() + filesDropCountBefore;

            if (transferredFiledrop) {
                // lock folder
                String token = EncryptionUtils.lockFolder(folder, client);

                // upload metadata
                EncryptedFolderMetadata encryptedFolderMetadataNew = encryptFolderMetadata(decryptedFolderMetadata,
                                                                                           publicKey,
                                                                                           arbitraryDataProvider,
                                                                                           user,
                                                                                           folder.getLocalId());

                String serializedFolderMetadata = EncryptionUtils.serializeJSON(encryptedFolderMetadataNew);

                EncryptionUtils.uploadMetadata(folder,
                                               serializedFolderMetadata,
                                               token,
                                               client,
                                               true);

                // unlock folder
                RemoteOperationResult unlockFolderResult = EncryptionUtils.unlockFolder(folder, client, token);

                if (!unlockFolderResult.isSuccess()) {
                    Log_OC.e(TAG, unlockFolderResult.getMessage());

                    return null;
                }
            }

            return decryptedFolderMetadata;
        } catch (Exception e) {
            Log_OC.e(TAG, e.getMessage());
            return null;
        }
    }

    /*
    BASE 64
     */

    public static byte[] encodeStringToBase64Bytes(String string) {
        try {
            return Base64.encode(string.getBytes(), Base64.NO_WRAP);
        } catch (Exception e) {
            return new byte[0];
        }
    }

    public static String decodeBase64BytesToString(byte[] bytes) {
        try {
            return new String(Base64.decode(bytes, Base64.NO_WRAP));
        } catch (Exception e) {
            return "";
        }
    }

    public static String encodeBytesToBase64String(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    public static byte[] decodeStringToBase64Bytes(String string) {
        return Base64.decode(string, Base64.NO_WRAP);
    }

    /*
    ENCRYPTION
     */

    /**
     * @param ocFile             file do crypt
     * @param encryptionKeyBytes key, either from metadata or {@link EncryptionUtils#generateKey()}
     * @param iv                 initialization vector, either from metadata or
     *                           {@link EncryptionUtils#randomBytes(int)}
     * @return encryptedFile with encryptedBytes and authenticationTag
     */
    public static EncryptedFile encryptFile(OCFile ocFile, byte[] encryptionKeyBytes, byte[] iv)
        throws NoSuchAlgorithmException,
        InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeyException,
        BadPaddingException, IllegalBlockSizeException, IOException {
        File file = new File(ocFile.getStoragePath());

        return encryptFile(file, encryptionKeyBytes, iv);
    }

    /**
     * @param file               file do crypt
     * @param encryptionKeyBytes key, either from metadata or {@link EncryptionUtils#generateKey()}
     * @param iv                 initialization vector, either from metadata or
     *                           {@link EncryptionUtils#randomBytes(int)}
     * @return encryptedFile with encryptedBytes and authenticationTag
     */
    public static EncryptedFile encryptFile(File file, byte[] encryptionKeyBytes, byte[] iv)
        throws NoSuchAlgorithmException,
        InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeyException,
        BadPaddingException, IllegalBlockSizeException, IOException {

        Cipher cipher = Cipher.getInstance(AES_CIPHER);

        Key key = new SecretKeySpec(encryptionKeyBytes, AES);

        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
        byte[] fileBytes = new byte[(int) randomAccessFile.length()];
        randomAccessFile.readFully(fileBytes);

        byte[] cryptedBytes = cipher.doFinal(fileBytes);
        String authenticationTag = encodeBytesToBase64String(Arrays.copyOfRange(cryptedBytes,
                                                                                cryptedBytes.length - (128 / 8), cryptedBytes.length));

        return new EncryptedFile(cryptedBytes, authenticationTag);
    }

    /**
     * @param file               encrypted file
     * @param encryptionKeyBytes key from metadata
     * @param iv                 initialization vector from metadata
     * @param authenticationTag  authenticationTag from metadata
     * @return decrypted byte[]
     */
    public static byte[] decryptFile(File file, byte[] encryptionKeyBytes, byte[] iv, byte[] authenticationTag)
        throws NoSuchAlgorithmException,
        InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeyException,
        BadPaddingException, IllegalBlockSizeException, IOException {


        Cipher cipher = Cipher.getInstance(AES_CIPHER);
        Key key = new SecretKeySpec(encryptionKeyBytes, AES);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
        byte[] fileBytes = new byte[(int) randomAccessFile.length()];
        randomAccessFile.readFully(fileBytes);

        // check authentication tag
        byte[] extractedAuthenticationTag = Arrays.copyOfRange(fileBytes,
                                                               fileBytes.length - (128 / 8), fileBytes.length);

        if (!Arrays.equals(extractedAuthenticationTag, authenticationTag)) {
            throw new SecurityException("Tag not correct");
        }

        return cipher.doFinal(fileBytes);
    }

    public static class EncryptedFile {
        public byte[] encryptedBytes;
        public String authenticationTag;

        public EncryptedFile(byte[] encryptedBytes, String authenticationTag) {
            this.encryptedBytes = encryptedBytes;
            this.authenticationTag = authenticationTag;
        }
    }

    /**
     * Encrypt string with RSA algorithm, ECB mode, OAEPWithSHA-256AndMGF1 padding Asymmetric encryption, with private
     * and public key
     *
     * @param string String to encrypt
     * @param cert   contains public key in it
     * @return encrypted string
     */
    public static String encryptStringAsymmetric(String string, String cert)
        throws NoSuchAlgorithmException,
        NoSuchPaddingException, InvalidKeyException,
        BadPaddingException, IllegalBlockSizeException,
        CertificateException {

        Cipher cipher = Cipher.getInstance(RSA_CIPHER);

        String trimmedCert = cert.replace("-----BEGIN CERTIFICATE-----\n", "")
            .replace("-----END CERTIFICATE-----\n", "");
        byte[] encodedCert = trimmedCert.getBytes(StandardCharsets.UTF_8);
        byte[] decodedCert = org.apache.commons.codec.binary.Base64.decodeBase64(encodedCert);

        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        InputStream in = new ByteArrayInputStream(decodedCert);
        X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(in);
        PublicKey realPublicKey = certificate.getPublicKey();

        cipher.init(Cipher.ENCRYPT_MODE, realPublicKey);

        byte[] bytes = encodeStringToBase64Bytes(string);
        byte[] cryptedBytes = cipher.doFinal(bytes);

        return encodeBytesToBase64String(cryptedBytes);
    }

    public static String encryptStringAsymmetric(String string, PublicKey publicKey) throws NoSuchPaddingException,
        NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(RSA_CIPHER);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] bytes = encodeStringToBase64Bytes(string);
        byte[] cryptedBytes = cipher.doFinal(bytes);

        return encodeBytesToBase64String(cryptedBytes);
    }


    /**
     * Decrypt string with RSA algorithm, ECB mode, OAEPWithSHA-256AndMGF1 padding Asymmetric encryption, with private
     * and public key
     *
     * @param string           string to decrypt
     * @param privateKeyString private key
     * @return decrypted string
     */
    public static String decryptStringAsymmetric(String string, String privateKeyString)
        throws NoSuchAlgorithmException,
        NoSuchPaddingException, InvalidKeyException,
        BadPaddingException, IllegalBlockSizeException,
        InvalidKeySpecException {

        Cipher cipher = Cipher.getInstance(RSA_CIPHER);

        byte[] privateKeyBytes = decodeStringToBase64Bytes(privateKeyString);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory kf = KeyFactory.getInstance(RSA);
        PrivateKey privateKey = kf.generatePrivate(keySpec);

        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] bytes = decodeStringToBase64Bytes(string);
        byte[] encodedBytes = cipher.doFinal(bytes);

        return decodeBase64BytesToString(encodedBytes);
    }

    public static String decryptStringAsymmetric(String string, PrivateKey privateKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(RSA_CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] bytes = decodeStringToBase64Bytes(string);
        byte[] encodedBytes = cipher.doFinal(bytes);

        return decodeBase64BytesToString(encodedBytes);
    }

    /**
     * Encrypt string with RSA algorithm, ECB mode, OAEPWithSHA-256AndMGF1 padding Asymmetric encryption, with private
     * and public key
     *
     * @param string             String to encrypt
     * @param encryptionKeyBytes key, either from metadata or {@link EncryptionUtils#generateKey()}
     * @return encrypted string
     */
    public static String encryptStringSymmetric(String string, byte[] encryptionKeyBytes)
        throws NoSuchPaddingException,
        InvalidKeyException,
        NoSuchAlgorithmException,
        IllegalBlockSizeException,
        BadPaddingException,
        InvalidAlgorithmParameterException {
        return encryptStringSymmetric(string, encryptionKeyBytes, ivDelimiter);
    }

    @VisibleForTesting
    public static String encryptStringSymmetricOld(String string, byte[] encryptionKeyBytes)
        throws NoSuchPaddingException,
        InvalidKeyException,
        NoSuchAlgorithmException,
        IllegalBlockSizeException,
        BadPaddingException,
        InvalidAlgorithmParameterException {
        return encryptStringSymmetric(string, encryptionKeyBytes, ivDelimiterOld);
    }

    private static String encryptStringSymmetric(String string,
                                                 byte[] encryptionKeyBytes,
                                                 String delimiter)
        throws NoSuchAlgorithmException,
        InvalidAlgorithmParameterException,
        NoSuchPaddingException,
        InvalidKeyException,
        BadPaddingException,
        IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(AES_CIPHER);
        byte[] iv = randomBytes(ivLength);

        Key key = new SecretKeySpec(encryptionKeyBytes, AES);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] bytes = encodeStringToBase64Bytes(string);
        byte[] cryptedBytes = cipher.doFinal(bytes);

        String encodedCryptedBytes = encodeBytesToBase64String(cryptedBytes);
        String encodedIV = encodeBytesToBase64String(iv);

        return encodedCryptedBytes + delimiter + encodedIV;
    }

    public static String decryptStringSymmetric(String string,
                                                byte[] encryptionKeyBytes,
                                                byte[] iv,
                                                byte[] authenticationTag)
        throws NoSuchPaddingException,
        NoSuchAlgorithmException,
        InvalidAlgorithmParameterException,
        InvalidKeyException,
        IllegalBlockSizeException,
        BadPaddingException {
        Cipher cipher = Cipher.getInstance(AES_CIPHER);
        Key key = new SecretKeySpec(encryptionKeyBytes, AES);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] bytes = decodeStringToBase64Bytes(string);

        // check authentication tag
        byte[] extractedAuthenticationTag = Arrays.copyOfRange(bytes,
                                                               bytes.length - (128 / 8),
                                                               bytes.length);

        if (!Arrays.equals(extractedAuthenticationTag, authenticationTag)) {
            throw new SecurityException("Tag not correct");
        }

        byte[] encodedBytes = cipher.doFinal(bytes);

        return decodeBase64BytesToString(encodedBytes);
    }

    /**
     * Decrypt string with RSA algorithm, ECB mode, OAEPWithSHA-256AndMGF1 padding Asymmetric encryption, with private
     * and public key
     *
     * @param string             string to decrypt
     * @param encryptionKeyBytes key from metadata
     * @return decrypted string
     */
    public static String decryptStringSymmetric(String string, byte[] encryptionKeyBytes)
        throws NoSuchAlgorithmException,
        InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeyException,
        BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(AES_CIPHER);

        String ivString;
        int delimiterPosition = string.lastIndexOf(ivDelimiter);

        if (delimiterPosition == -1) {
            // backward compatibility
            delimiterPosition = string.lastIndexOf(ivDelimiterOld);
            ivString = string.substring(delimiterPosition + ivDelimiterOld.length());
        } else {
            ivString = string.substring(delimiterPosition + ivDelimiter.length());
        }

        String cipherString = string.substring(0, delimiterPosition);

        byte[] iv = new IvParameterSpec(decodeStringToBase64Bytes(ivString)).getIV();

        Key key = new SecretKeySpec(encryptionKeyBytes, AES);

        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] bytes = decodeStringToBase64Bytes(cipherString);
        byte[] encodedBytes = cipher.doFinal(bytes);

        return decodeBase64BytesToString(encodedBytes);
    }

    /**
     * Encrypt private key with symmetric AES encryption, GCM mode mode and no padding
     *
     * @param privateKey byte64 encoded string representation of private key
     * @param keyPhrase  key used for encryption, e.g. 12 random words
     *                   {@link EncryptionUtils#getRandomWords(int, Context)}
     * @return encrypted string, bytes first encoded base64, IV separated with "|", then to string
     */
    public static String encryptPrivateKey(String privateKey, String keyPhrase)
        throws NoSuchPaddingException,
        NoSuchAlgorithmException,
        InvalidKeyException,
        BadPaddingException,
        IllegalBlockSizeException,
        InvalidKeySpecException {
        return encryptPrivateKey(privateKey, keyPhrase, ivDelimiter);
    }

    @VisibleForTesting
    public static String encryptPrivateKeyOld(String privateKey, String keyPhrase)
        throws NoSuchPaddingException,
        NoSuchAlgorithmException,
        InvalidKeyException,
        BadPaddingException,
        IllegalBlockSizeException,
        InvalidKeySpecException {
        return encryptPrivateKey(privateKey, keyPhrase, ivDelimiterOld);
    }

    private static String encryptPrivateKey(String privateKey, String keyPhrase, String delimiter)
        throws NoSuchPaddingException,
        NoSuchAlgorithmException,
        InvalidKeyException,
        BadPaddingException,
        IllegalBlockSizeException,
        InvalidKeySpecException {
        Cipher cipher = Cipher.getInstance(AES_CIPHER);

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] salt = randomBytes(saltLength);
        KeySpec spec = new PBEKeySpec(keyPhrase.toCharArray(), salt, iterationCount, keyStrength);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKeySpec key = new SecretKeySpec(tmp.getEncoded(), AES);

        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] bytes = encodeStringToBase64Bytes(privateKey);
        byte[] encrypted = cipher.doFinal(bytes);

        byte[] iv = cipher.getIV();
        String encodedIV = encodeBytesToBase64String(iv);
        String encodedSalt = encodeBytesToBase64String(salt);
        String encodedEncryptedBytes = encodeBytesToBase64String(encrypted);

        return encodedEncryptedBytes + delimiter + encodedIV + delimiter + encodedSalt;
    }

    /**
     * Decrypt private key with symmetric AES encryption, GCM mode mode and no padding
     *
     * @param privateKey byte64 encoded string representation of private key, IV separated with "|"
     * @param keyPhrase  key used for encryption, e.g. 12 random words
     *                   {@link EncryptionUtils#getRandomWords(int, Context)}
     * @return decrypted string
     */
    @SuppressFBWarnings("UCPM_USE_CHARACTER_PARAMETERIZED_METHOD")
    public static String decryptPrivateKey(String privateKey, String keyPhrase) throws NoSuchPaddingException,
        NoSuchAlgorithmException, InvalidKeyException, BadPaddingException,
        IllegalBlockSizeException, InvalidKeySpecException, InvalidAlgorithmParameterException {

        String[] strings;

        // split up iv, salt
        if (privateKey.lastIndexOf(ivDelimiter) == -1) {
            // backward compatibility
            strings = privateKey.split(ivDelimiterOld);
        } else {
            strings = privateKey.split("\\" + ivDelimiter);
        }

        String realPrivateKey = strings[0];
        byte[] iv = decodeStringToBase64Bytes(strings[1]);
        byte[] salt = decodeStringToBase64Bytes(strings[2]);

        Cipher cipher = Cipher.getInstance(AES_CIPHER);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec spec = new PBEKeySpec(keyPhrase.toCharArray(), salt, iterationCount, keyStrength);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKeySpec key = new SecretKeySpec(tmp.getEncoded(), AES);

        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

        byte[] bytes = decodeStringToBase64Bytes(realPrivateKey);
        byte[] decrypted = cipher.doFinal(bytes);

        String pemKey = decodeBase64BytesToString(decrypted);

        return pemKey.replaceAll("\n", "").replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "");
    }

    public static String privateKeyToPEM(PrivateKey privateKey) {
        String privateKeyString = encodeBytesToBase64String(privateKey.getEncoded());

        return "-----BEGIN PRIVATE KEY-----\n" + privateKeyString.replaceAll("(.{65})", "$1\n")
            + "\n-----END PRIVATE KEY-----";
    }

    /*
    Helper
     */

    public static ArrayList<String> getRandomWords(int count, Context context) throws IOException {
        InputStream ins = context.getResources().openRawResource(R.raw.encryption_key_words);

        InputStreamReader inputStreamReader = new InputStreamReader(ins);

        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        List<String> lines = new ArrayList<>();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            lines.add(line);
        }

        SecureRandom random = new SecureRandom();

        ArrayList<String> outputLines = Lists.newArrayListWithCapacity(count);
        for (int i = 0; i < count; i++) {
            int randomLine = random.nextInt(lines.size());
            outputLines.add(lines.get(randomLine));
        }

        return outputLines;
    }

    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA);
        keyGen.initialize(2048, new SecureRandom());
        return keyGen.generateKeyPair();
    }

    public static byte[] generateKey() {
        KeyGenerator keyGenerator;
        try {
            keyGenerator = KeyGenerator.getInstance(AES);
            keyGenerator.init(128);

            return keyGenerator.generateKey().getEncoded();
        } catch (NoSuchAlgorithmException e) {
            Log_OC.e(TAG, e.getMessage());
        }

        return null;
    }

    public static byte[] randomBytes(int size) {
        SecureRandom random = new SecureRandom();
        final byte[] iv = new byte[size];
        random.nextBytes(iv);

        return iv;
    }

    /**
     * Generate a SHA512 with appended salt
     *
     * @param token token to be hashed
     * @return SHA512 with appended salt, delimiter HASH_DELIMITER
     */
    public static String generateSHA512(String token) {
        String salt = EncryptionUtils.encodeBytesToBase64String(EncryptionUtils.randomBytes(EncryptionUtils.saltLength));

        return generateSHA512(token, salt);
    }

    /**
     * Generate a SHA512 with appended salt
     *
     * @param token token to be hashed
     * @return SHA512 with appended salt, delimiter HASH_DELIMITER
     */
    public static String generateSHA512(String token, String salt) {
        MessageDigest digest;
        String hashedToken = "";
        byte[] hash;
        try {
            digest = MessageDigest.getInstance("SHA-512");
            digest.update(salt.getBytes());
            hash = digest.digest(token.getBytes());

            StringBuilder stringBuilder = new StringBuilder();
            for (byte hashByte : hash) {
                stringBuilder.append(Integer.toString((hashByte & 0xff) + 0x100, 16).substring(1));
            }

            stringBuilder.append(HASH_DELIMITER).append(salt);

            hashedToken = stringBuilder.toString();

        } catch (NoSuchAlgorithmException e) {
            Log_OC.e(TAG, "Generating SHA512 failed", e);
        }

        return hashedToken;
    }

    public static boolean verifySHA512(String hashWithSalt, String compareToken) {
        String salt = hashWithSalt.split("\\" + HASH_DELIMITER)[1];

        String newHash = generateSHA512(compareToken, salt);

        return hashWithSalt.equals(newHash);
    }

    public static String lockFolder(OCFile parentFile, OwnCloudClient client) throws UploadException {
        // Lock folder
        LockFileRemoteOperation lockFileOperation = new LockFileRemoteOperation(parentFile.getLocalId());
        RemoteOperationResult lockFileOperationResult = lockFileOperation.execute(client);

        if (lockFileOperationResult.isSuccess() &&
            !TextUtils.isEmpty((String) lockFileOperationResult.getData().get(0))) {
            return (String) lockFileOperationResult.getData().get(0);
        } else if (lockFileOperationResult.getHttpCode() == HttpStatus.SC_FORBIDDEN) {
            throw new UploadException("Forbidden! Please try again later.)");
        } else {
            throw new UploadException("Could not lock folder");
        }
    }

    /**
     * @param parentFile file metadata should be retrieved for
     * @return Pair: boolean: true: metadata already exists, false: metadata new created
     */
    public static Pair<Boolean, DecryptedFolderMetadata> retrieveMetadata(OCFile parentFile,
                                                                          OwnCloudClient client,
                                                                          String privateKey,
                                                                          String publicKey,
                                                                          ArbitraryDataProvider arbitraryDataProvider,
                                                                          User user)
        throws UploadException,
        InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchPaddingException, BadPaddingException,
        IllegalBlockSizeException, InvalidKeyException, InvalidKeySpecException, CertificateException {
        long localId = parentFile.getLocalId();

        GetMetadataRemoteOperation getMetadataOperation = new GetMetadataRemoteOperation(localId);
        RemoteOperationResult getMetadataOperationResult = getMetadataOperation.execute(client);

        DecryptedFolderMetadata metadata;

        if (getMetadataOperationResult.isSuccess()) {
            // decrypt metadata
            String serializedEncryptedMetadata = (String) getMetadataOperationResult.getData().get(0);


            EncryptedFolderMetadata encryptedFolderMetadata = EncryptionUtils.deserializeJSON(
                serializedEncryptedMetadata, new TypeToken<EncryptedFolderMetadata>() {
                });

            return new Pair<>(Boolean.TRUE, decryptFolderMetaData(encryptedFolderMetadata,
                                                                  privateKey,
                                                                  arbitraryDataProvider,
                                                                  user,
                                                                  localId));

        } else if (getMetadataOperationResult.getHttpCode() == HttpStatus.SC_NOT_FOUND) {
            // new metadata
            metadata = new DecryptedFolderMetadata();
            metadata.setMetadata(new DecryptedFolderMetadata.Metadata());
            metadata.getMetadata().setMetadataKeys(new HashMap<>());
            String metadataKey = EncryptionUtils.encodeBytesToBase64String(EncryptionUtils.generateKey());
            String encryptedMetadataKey = EncryptionUtils.encryptStringAsymmetric(metadataKey, publicKey);
            metadata.getMetadata().setMetadataKey(encryptedMetadataKey);

            return new Pair<>(Boolean.FALSE, metadata);
        } else {
            // TODO error
            throw new UploadException("something wrong");
        }
    }

    public static void uploadMetadata(OCFile parentFile,
                                      String serializedFolderMetadata,
                                      String token,
                                      OwnCloudClient client,
                                      boolean metadataExists) throws UploadException {
        RemoteOperationResult uploadMetadataOperationResult;
        if (metadataExists) {
            // update metadata
            UpdateMetadataRemoteOperation storeMetadataOperation = new UpdateMetadataRemoteOperation(
                parentFile.getLocalId(), serializedFolderMetadata, token);
            uploadMetadataOperationResult = storeMetadataOperation.execute(client);
        } else {
            // store metadata
            StoreMetadataRemoteOperation storeMetadataOperation = new StoreMetadataRemoteOperation(
                parentFile.getLocalId(), serializedFolderMetadata);
            uploadMetadataOperationResult = storeMetadataOperation.execute(client);
        }

        if (!uploadMetadataOperationResult.isSuccess()) {
            throw new UploadException("Storing/updating metadata was not successful");
        }
    }

    public static RemoteOperationResult unlockFolder(OCFile parentFolder, OwnCloudClient client, String token) {
        if (token != null) {
            return new UnlockFileRemoteOperation(parentFolder.getLocalId(), token).execute(client);
        } else {
            return new RemoteOperationResult(new Exception("No token available"));
        }
    }

    public static RSAPublicKey convertPublicKeyFromString(String string) throws CertificateException {
        String trimmedCert = string.replace("-----BEGIN CERTIFICATE-----\n", "")
            .replace("-----END CERTIFICATE-----\n", "");
        byte[] encodedCert = trimmedCert.getBytes(StandardCharsets.UTF_8);
        byte[] decodedCert = org.apache.commons.codec.binary.Base64.decodeBase64(encodedCert);

        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        InputStream in = new ByteArrayInputStream(decodedCert);
        X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(in);
        return (RSAPublicKey) certificate.getPublicKey();
    }

    public static void removeE2E(ArbitraryDataProvider arbitraryDataProvider, User user) {
        // delete stored E2E keys and mnemonic
        arbitraryDataProvider.deleteKeyForAccount(user.getAccountName(), EncryptionUtils.PRIVATE_KEY);
        arbitraryDataProvider.deleteKeyForAccount(user.getAccountName(), EncryptionUtils.PUBLIC_KEY);
        arbitraryDataProvider.deleteKeyForAccount(user.getAccountName(), EncryptionUtils.MNEMONIC);
    }

    public static boolean isMatchingKeys(KeyPair keyPair, String publicKeyString) throws CertificateException {
        // check key
        RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) keyPair.getPrivate();
        RSAPublicKey publicKey = EncryptionUtils.convertPublicKeyFromString(publicKeyString);

        BigInteger modulusPublic = publicKey.getModulus();
        BigInteger modulusPrivate = privateKey.getModulus();

        return modulusPrivate.compareTo(modulusPublic) == 0;
    }

    public static boolean supportsSecureFiledrop(OCFile file, User user) {
        return file.isEncrypted() &&
            file.isFolder() &&
            user.getServer().getVersion().isNewerOrEqual(NextcloudVersion.nextcloud_26);
    }

    public static String generateChecksum(DecryptedFolderMetadata metadata,
                                          String mnemonic) throws NoSuchAlgorithmException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(mnemonic.replaceAll(" ", ""));

        ArrayList<String> keys = new ArrayList<>(metadata.getFiles().keySet());
        Collections.sort(keys);

        for (String key : keys) {
            stringBuilder.append(key);
        }

        stringBuilder.append(metadata.getMetadata().getMetadataKey());

        // sha256 hash-sum
        return sha256(stringBuilder.toString());
    }

    /**
     * SHA-256 hash of metadata-key
     */
    public static String sha256(String string) throws NoSuchAlgorithmException {
        byte[] bytes = MessageDigest
            .getInstance("SHA-256")
            .digest(string.getBytes(StandardCharsets.UTF_8));

        return bytesToHex(bytes);
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte individualByte : bytes) {
            result.append(Integer.toString((individualByte & 0xff) + 0x100, 16)
                              .substring(1));
        }
        return result.toString();
    }

    public static void addIdToMigratedIds(long id,
                                          User user,
                                          ArbitraryDataProvider arbitraryDataProvider) {
        Gson gson = new Gson();
        String ids = arbitraryDataProvider.getValue(user, MIGRATED_FOLDER_IDS);

        ArrayList<Long> arrayList = gson.fromJson(ids, ArrayList.class);

        if (arrayList == null) {
            arrayList = new ArrayList<>();
        }

        if (arrayList.contains(id)) {
            // nothing to do here
            return;
        }

        arrayList.add(id);

        String json = gson.toJson(arrayList);
        arbitraryDataProvider.storeOrUpdateKeyValue(user.getAccountName(),
                                                    MIGRATED_FOLDER_IDS,
                                                    json);
    }

    public static boolean isFolderMigrated(long id,
                                           User user,
                                           ArbitraryDataProvider arbitraryDataProvider) {
        Gson gson = new Gson();
        String ids = arbitraryDataProvider.getValue(user, MIGRATED_FOLDER_IDS);

        ArrayList<Long> arrayList = gson.fromJson(ids, new TypeToken<List<Long>>() {
        }.getType());

        if (arrayList == null) {
            return false;
        }

        return arrayList.contains(id);
    }
}
