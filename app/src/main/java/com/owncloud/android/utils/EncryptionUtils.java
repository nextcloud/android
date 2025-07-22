/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
import com.owncloud.android.datamodel.EncryptedFiledrop;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFile;
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFileV1;
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedMetadata;
import com.owncloud.android.datamodel.e2e.v1.encrypted.EncryptedFile;
import com.owncloud.android.datamodel.e2e.v1.encrypted.EncryptedFolderMetadataFileV1;
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile;
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedUser;
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedFolderMetadataFile;
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedMetadata;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.e2ee.GetMetadataRemoteOperation;
import com.owncloud.android.lib.resources.e2ee.LockFileRemoteOperation;
import com.owncloud.android.lib.resources.e2ee.MetadataResponse;
import com.owncloud.android.lib.resources.e2ee.StoreMetadataRemoteOperation;
import com.owncloud.android.lib.resources.e2ee.StoreMetadataV2RemoteOperation;
import com.owncloud.android.lib.resources.e2ee.UnlockFileRemoteOperation;
import com.owncloud.android.lib.resources.e2ee.UnlockFileV1RemoteOperation;
import com.owncloud.android.lib.resources.e2ee.UpdateMetadataRemoteOperation;
import com.owncloud.android.lib.resources.e2ee.UpdateMetadataV2RemoteOperation;
import com.owncloud.android.lib.resources.files.model.ServerFileInterface;
import com.owncloud.android.lib.resources.status.E2EVersion;
import com.owncloud.android.lib.resources.status.NextcloudVersion;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.lib.resources.status.Problem;
import com.owncloud.android.lib.resources.status.SendClientDiagnosticRemoteOperation;
import com.owncloud.android.operations.UploadException;
import com.owncloud.android.utils.theme.CapabilityUtils;

import org.apache.commons.httpclient.HttpStatus;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
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

    private static final char HASH_DELIMITER = '$';
    private static final int iterationCount = 1024;
    private static final int keyStrength = 256;
    private static final String AES_CIPHER = "AES/GCM/NoPadding";
    private static final String AES = "AES";
    public static final String RSA_CIPHER = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    public static final String RSA = "RSA";
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
            return new GsonBuilder()
                .disableHtmlEscaping()
                .create()
                .toJson(data);
        } else {
            return new GsonBuilder()
                .disableHtmlEscaping()
                .excludeFieldsWithModifiers(0)
                .create()
                .toJson(data);
        }
    }

    public static void removeFileFromMetadata(String fileName, DecryptedFolderMetadataFileV1 metadata) {
        metadata.getFiles().remove(fileName);
    }

    public static String serializeJSON(Object data) {
        return serializeJSON(data, false);
    }

    /*
    METADATA
     */

    /**
     * Encrypt folder metaData V1
     *
     * @param decryptedFolderMetadata folder metaData to encrypt
     * @return EncryptedFolderMetadataFile encrypted folder metadata
     */
    public static EncryptedFolderMetadataFileV1 encryptFolderMetadata(
        DecryptedFolderMetadataFileV1 decryptedFolderMetadata,
        String publicKey,
        long parentId,
        User user,
        ArbitraryDataProvider arbitraryDataProvider
                                                                     )
        throws NoSuchAlgorithmException, InvalidKeyException,
        InvalidAlgorithmParameterException, NoSuchPaddingException, BadPaddingException,
        IllegalBlockSizeException, CertificateException {

        HashMap<String, EncryptedFolderMetadataFileV1.EncryptedFile> files = new HashMap<>();
        HashMap<String, EncryptedFiledrop> filesdrop = new HashMap<>();
        EncryptedFolderMetadataFileV1 encryptedFolderMetadata = new EncryptedFolderMetadataFileV1(decryptedFolderMetadata
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
        for (Map.Entry<String, DecryptedFile> entry : decryptedFolderMetadata
            .getFiles().entrySet()) {
            String key = entry.getKey();
            DecryptedFile decryptedFile = entry.getValue();

            EncryptedFolderMetadataFileV1.EncryptedFile encryptedFile = new EncryptedFolderMetadataFileV1.EncryptedFile();
            encryptedFile.setInitializationVector(decryptedFile.getInitializationVector());
            encryptedFile.setAuthenticationTag(decryptedFile.getAuthenticationTag());

            // encrypt
            String dataJson = EncryptionUtils.serializeJSON(decryptedFile.getEncrypted());
            encryptedFile.setEncrypted(EncryptionUtils.encryptStringSymmetricAsString(dataJson, metadataKeyBytes));

            files.put(key, encryptedFile);
        }

        // set checksum
        String mnemonic = arbitraryDataProvider.getValue(user.getAccountName(), EncryptionUtils.MNEMONIC).trim();
        String checksum = EncryptionUtils.generateChecksum(decryptedFolderMetadata, mnemonic);
        encryptedFolderMetadata.getMetadata().setChecksum(checksum);

        return encryptedFolderMetadata;
    }

    /**
     * normally done on server only internal test
     */
    @VisibleForTesting
    public static void encryptFileDropFiles(DecryptedFolderMetadataFileV1 decryptedFolderMetadata,
                                            EncryptedFolderMetadataFileV1 encryptedFolderMetadata,
                                            String cert) throws NoSuchAlgorithmException, NoSuchPaddingException,
        InvalidKeyException, BadPaddingException, IllegalBlockSizeException, CertificateException,
        InvalidAlgorithmParameterException {
        final Map<String, EncryptedFiledrop> filesdrop = encryptedFolderMetadata.getFiledrop();
        for (Map.Entry<String, DecryptedFile> entry : decryptedFolderMetadata
            .getFiledrop().entrySet()) {
            String key = entry.getKey();
            DecryptedFile decryptedFile = entry.getValue();

            byte[] byt = generateKey();
            String metadataKey0 = encodeBytesToBase64String(byt);
            String enc = encryptStringAsymmetric(metadataKey0, cert);

            String dataJson = EncryptionUtils.serializeJSON(decryptedFile.getEncrypted());

            String encJson = encryptStringSymmetricAsString(dataJson, byt);

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
     * decrypt folder metaData V1 with private key
     */
    public static DecryptedFolderMetadataFileV1 decryptFolderMetaData(EncryptedFolderMetadataFileV1 encryptedFolderMetadata,
                                                                      String privateKey,
                                                                      ArbitraryDataProvider arbitraryDataProvider,
                                                                      User user,
                                                                      long remoteId)
        throws NoSuchAlgorithmException, InvalidKeyException,
        InvalidAlgorithmParameterException, NoSuchPaddingException, BadPaddingException,
        IllegalBlockSizeException, InvalidKeySpecException {

        HashMap<String, DecryptedFile> files = new HashMap<>();
        DecryptedFolderMetadataFileV1 decryptedFolderMetadata = new DecryptedFolderMetadataFileV1(
            encryptedFolderMetadata.getMetadata(), files);

        byte[] decryptedMetadataKey = null;

        String encryptedMetadataKey = decryptedFolderMetadata.getMetadata().getMetadataKey();

        if (encryptedMetadataKey != null) {
            decryptedMetadataKey = decodeStringToBase64Bytes(
                decryptStringAsymmetric(encryptedMetadataKey, privateKey));
        }

        if (encryptedFolderMetadata.getFiles() != null) {
            for (Map.Entry<String, EncryptedFolderMetadataFileV1.EncryptedFile> entry : encryptedFolderMetadata
                .getFiles().entrySet()) {
                String key = entry.getKey();
                EncryptedFolderMetadataFileV1.EncryptedFile encryptedFile = entry.getValue();

                DecryptedFile decryptedFile = new DecryptedFile();
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
                                                                           new TypeToken<>() {
                                                                           }));

                files.put(key, decryptedFile);
            }
        }

        // verify checksum
        String mnemonic = arbitraryDataProvider.getValue(user.getAccountName(), EncryptionUtils.MNEMONIC).trim();
        String checksum = EncryptionUtils.generateChecksum(decryptedFolderMetadata, mnemonic);
        String decryptedFolderChecksum = decryptedFolderMetadata.getMetadata().getChecksum();

        if (TextUtils.isEmpty(decryptedFolderChecksum) &&
            isFolderMigrated(remoteId, user, arbitraryDataProvider)) {
            reportE2eError(arbitraryDataProvider, user);
            throw new IllegalStateException("Possible downgrade attack detected!");
        }

        if (!TextUtils.isEmpty(decryptedFolderChecksum) && !decryptedFolderChecksum.equals(checksum)) {
            reportE2eError(arbitraryDataProvider, user);
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
                String decryptedData = decryptStringSymmetricAsString(
                    encryptedFile.getEncrypted(),
                    decodeStringToBase64Bytes(encryptedKey),
                    decodeStringToBase64Bytes(encryptedFile.getEncryptedInitializationVector()),
                    decodeStringToBase64Bytes(encryptedFile.getEncryptedTag()),
                    arbitraryDataProvider,
                    user
                                                                     );

                DecryptedFile decryptedFile = new DecryptedFile();
                decryptedFile.setInitializationVector(encryptedFile.getInitializationVector());
                decryptedFile.setAuthenticationTag(encryptedFile.getAuthenticationTag());


                decryptedFile.setEncrypted(EncryptionUtils.deserializeJSON(decryptedData,
                                                                           new TypeToken<>() {
                                                                           }));

                files.put(key, decryptedFile);

                // remove from filedrop
                fileDrop.remove(key);
            }
        }

        return decryptedFolderMetadata;
    }

    /**
     * Download metadata (v1 or v2) for folder and decrypt it
     *
     * @return decrypted v2 metadata or null
     */
    @SuppressFBWarnings("URV")
    public static @Nullable
    Object
    downloadFolderMetadata(OCFile folder,
                           OwnCloudClient client,
                           Context context,
                           User user
                          ) {
        RemoteOperationResult<MetadataResponse> getMetadataOperationResult = new GetMetadataRemoteOperation(folder.getLocalId())
            .execute(client);

        if (!getMetadataOperationResult.isSuccess()) {
            return null;
        }

        OCCapability capability = CapabilityUtils.getCapability(context);

        // decrypt metadata
        EncryptionUtilsV2 encryptionUtilsV2 = new EncryptionUtilsV2();
        String serializedEncryptedMetadata = getMetadataOperationResult.getResultData().getMetadata();

        E2EVersion version = determinateVersion(serializedEncryptedMetadata);

        switch (version) {
            case UNKNOWN:
                Log_OC.e(TAG, "Unknown e2e state");
                return null;

            case V1_0, V1_1, V1_2:
                ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProviderImpl(context);
                String privateKey = arbitraryDataProvider.getValue(user.getAccountName(), EncryptionUtils.PRIVATE_KEY);
                String publicKey = arbitraryDataProvider.getValue(user.getAccountName(), EncryptionUtils.PUBLIC_KEY);
                EncryptedFolderMetadataFileV1 encryptedFolderMetadata = EncryptionUtils.deserializeJSON(
                    serializedEncryptedMetadata, new TypeToken<>() {
                    });

                try {
                    DecryptedFolderMetadataFileV1 v1 = decryptFolderMetaData(encryptedFolderMetadata,
                                                                             privateKey,
                                                                             arbitraryDataProvider,
                                                                             user,
                                                                             folder.getLocalId());

                    if (capability.getEndToEndEncryptionApiVersion().compareTo(E2EVersion.V2_0) >= 0) {
                        new EncryptionUtilsV2().migrateV1ToV2andUpload(
                            v1,
                            client.getUserId(),
                            publicKey,
                            folder,
                            new FileDataStorageManager(user, context.getContentResolver()),
                            client,
                            user,
                            context
                                                                      );
                    } else {
                        return v1;
                    }
                } catch (Exception e) {
                    // TODO do not crash, but show meaningful error 
                    Log_OC.e(TAG, "Could not decrypt metadata for " + folder.getDecryptedFileName(), e);
                    return null;
                }

            case V2_0:
                return encryptionUtilsV2.parseAnyMetadata(getMetadataOperationResult.getResultData(),
                                                          user,
                                                          client,
                                                          context,
                                                          folder);
        }
        return null;
    }

    public static E2EVersion determinateVersion(String metadata) {
        try {
            EncryptedFolderMetadataFileV1 v1 = EncryptionUtils.deserializeJSON(
                metadata,
                new TypeToken<>() {
                });

            double version = v1.getMetadata().getVersion();

            if (version == 1.0) {
                return E2EVersion.V1_0;
            } else if (version == 1.1) {
                return E2EVersion.V1_1;
            } else if (version == 1.2) {
                return E2EVersion.V1_2;
            } else {
                throw new IllegalStateException("Unknown version");
            }
        } catch (Exception e) {
            EncryptedFolderMetadataFile v2 = EncryptionUtils.deserializeJSON(
                metadata,
                new TypeToken<>() {
                });

            if (v2 != null) {
                if ("2.0".equals(v2.getVersion()) || "2".equals(v2.getVersion())) {
                    return E2EVersion.V2_0;
                }
            } else {
                return E2EVersion.UNKNOWN;
            }
        }

        return E2EVersion.UNKNOWN;
    }

    /*
    BASE 64
     */
    @SuppressFBWarnings({"DM", "MDM"})
    public static byte[] encodeStringToBase64Bytes(String string) {
        try {
            return Base64.encode(string.getBytes(), Base64.NO_WRAP);
        } catch (Exception e) {
            return new byte[0];
        }
    }

    @SuppressFBWarnings({"DM", "MDM"})
    public static String decodeBase64BytesToString(byte[] bytes) {
        try {
            return new String(Base64.decode(bytes, Base64.NO_WRAP));
        } catch (Exception e) {
            return "";
        }
    }

    @SuppressFBWarnings({"DM", "MDM"})
    public static String encodeBytesToBase64String(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    @SuppressFBWarnings({"DM", "MDM"})
    public static String encodeStringToBase64String(String string) {
        return Base64.encodeToString(string.getBytes(), Base64.NO_WRAP);
    }

    @SuppressFBWarnings({"DM", "MDM"})
    public static String decodeBase64StringToString(String string) {
        return new String(Base64.decode(string, Base64.NO_WRAP));
    }

    public static byte[] decodeStringToBase64Bytes(String string) {
        return Base64.decode(string, Base64.NO_WRAP);
    }

    public static EncryptedFile encryptFile(String accountName, File file, Cipher cipher) throws InvalidParameterSpecException, IOException {
        File tempEncryptedFolder = FileDataStorageManager.createTempEncryptedFolder(accountName);
        File tempEncryptedFile = File.createTempFile(file.getName(), null, tempEncryptedFolder);
        encryptFileWithGivenCipher(file, tempEncryptedFile, cipher);
        String authenticationTagString = getAuthenticationTag(cipher);
        return new EncryptedFile(tempEncryptedFile, authenticationTagString);
    }

    public static String getAuthenticationTag(Cipher cipher) throws InvalidParameterSpecException {
        byte[] authenticationTag = cipher.getParameters().getParameterSpec(GCMParameterSpec.class).getIV();
        return encodeBytesToBase64String(authenticationTag);
    }

    public static Cipher getCipher(int mode, byte[] encryptionKeyBytes, byte[] iv) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance(AES_CIPHER);
        Key key = new SecretKeySpec(encryptionKeyBytes, AES);
        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(mode, key, spec);
        return cipher;
    }

    public static void encryptFileWithGivenCipher(File inputFile, File encryptedFile, Cipher cipher) {
        try (FileInputStream inputStream = new FileInputStream(inputFile);
             FileOutputStream fileOutputStream = new FileOutputStream(encryptedFile);
             CipherOutputStream outputStream = new CipherOutputStream(fileOutputStream, cipher)) {
            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            Log_OC.d(TAG, encryptedFile.getName() + "encrypted successfully");
        } catch (IOException exception) {
            Log_OC.d(TAG, "Error caught at encryptFileWithGivenCipher(): " + exception.getLocalizedMessage());
        }
    }

    public static void decryptFile(Cipher cipher,
                                   File encryptedFile,
                                   File decryptedFile,
                                   String authenticationTag,
                                   ArbitraryDataProvider arbitraryDataProvider,
                                   User user) {
        try (FileInputStream inputStream = new FileInputStream(encryptedFile);
             FileOutputStream outputStream = new FileOutputStream(decryptedFile)) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byte[] output = cipher.update(buffer, 0, bytesRead);
                if (output != null) {
                    outputStream.write(output);
                }
            }
            byte[] output = cipher.doFinal();
            if (output != null) {
                outputStream.write(output);
            }
            inputStream.close();
            outputStream.close();

            if (!getAuthenticationTag(cipher).equals(authenticationTag)) {
                reportE2eError(arbitraryDataProvider, user);
                throw new SecurityException("Tag not correct");
            }

            Log_OC.d(TAG, encryptedFile.getName() + "decrypted successfully");
        } catch (IOException | BadPaddingException | IllegalBlockSizeException | InvalidParameterSpecException |
                 SecurityException exception) {
            Log_OC.d(TAG, "Error caught at decryptFile(): " + exception.getLocalizedMessage());
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

    public static String encryptStringAsymmetricV2(byte[] bytes, String cert)
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

    public static byte[] decryptStringAsymmetricAsBytes(String string, String privateKeyString)
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

        return cipher.doFinal(bytes);
    }

    public static byte[] decryptStringAsymmetricV2(String string, String privateKeyString)
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

        byte[] bytes;
        try {
            bytes = decodeStringToBase64Bytes(string);
        } catch (Exception e) {
            bytes = encodeStringToBase64Bytes(string);
        }

        return cipher.doFinal(bytes);
    }

    /**
     * Decrypt string with RSA algorithm, ECB mode, OAEPWithSHA-256AndMGF1 padding Asymmetric encryption, with private
     * and public key
     *
     * @param string     string to decrypt
     * @param privateKey private key
     * @return decrypted string
     */
    public static String decryptStringAsymmetric(String string, PrivateKey privateKey) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(RSA_CIPHER);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] bytes = decodeStringToBase64Bytes(string);
        byte[] encodedBytes = cipher.doFinal(bytes);

        return decodeBase64BytesToString(encodedBytes);
    }

    /**
     * Decrypt string with AES/GCM/NoPadding
     *
     * @param string             string to decrypt
     * @param encryptionKeyBytes key from metadata
     * @return decrypted string
     */
    public static String encryptStringSymmetricAsString(String string, byte[] encryptionKeyBytes)
        throws NoSuchPaddingException,
        InvalidKeyException,
        NoSuchAlgorithmException,
        IllegalBlockSizeException,
        BadPaddingException,
        InvalidAlgorithmParameterException {
        EncryptedMetadata metadata = encryptStringSymmetric(string, encryptionKeyBytes, ivDelimiter);

        return metadata.getCiphertext();
    }

    @VisibleForTesting
    public static String encryptStringSymmetricAsStringOld(String string, byte[] encryptionKeyBytes)
        throws NoSuchPaddingException,
        InvalidKeyException,
        NoSuchAlgorithmException,
        IllegalBlockSizeException,
        BadPaddingException,
        InvalidAlgorithmParameterException {
        EncryptedMetadata metadata = encryptStringSymmetric(string, encryptionKeyBytes, ivDelimiterOld);

        return metadata.getCiphertext();
    }

    //    /**
//     * Encrypt string with AES/GCM/NoPadding
//     *
//     * @param string             string to encrypt
//     * @param encryptionKeyBytes key from metadata
//     * @return decrypted string
//     */
//    private static String encryptStringSymmetric(String string,
//                                                 byte[] encryptionKeyBytes,
//                                                 String delimiter)
//        throws NoSuchAlgorithmException,
//        InvalidAlgorithmParameterException,
//        NoSuchPaddingException,
//        InvalidKeyException,
//        BadPaddingException,
//        IllegalBlockSizeException {
//
//        Cipher cipher = Cipher.getInstance(AES_CIPHER);
//        byte[] iv = randomBytes(ivLength);
//
//        Key key = new SecretKeySpec(encryptionKeyBytes, AES);
//        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
//        cipher.init(Cipher.ENCRYPT_MODE, key, spec);
//
//        byte[] bytes = encodeStringToBase64Bytes(string);
//        byte[] cryptedBytes = cipher.doFinal(bytes);
//
//        String encodedCryptedBytes = encodeBytesToBase64String(cryptedBytes);
//        String encodedIV = encodeBytesToBase64String(iv);
//
//        return encodedCryptedBytes + delimiter + encodedIV;
//    }
    public static String decryptStringSymmetricAsString(String string,
                                                        byte[] encryptionKeyBytes,
                                                        byte[] iv,
                                                        byte[] authenticationTag,
                                                        ArbitraryDataProvider arbitraryDataProvider,
                                                        User user
                                                       ) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        return decryptStringSymmetricAsString(
            decodeStringToBase64Bytes(string),
            encryptionKeyBytes,
            iv,
            authenticationTag,
            false,
            arbitraryDataProvider,
            user);
    }

    public static String decryptStringSymmetricAsString(String string,
                                                        byte[] encryptionKeyBytes,
                                                        byte[] iv,
                                                        byte[] authenticationTag,
                                                        boolean fileDropV2,
                                                        ArbitraryDataProvider arbitraryDataProvider,
                                                        User user) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {

        return decryptStringSymmetricAsString(
            decodeStringToBase64Bytes(string),
            encryptionKeyBytes,
            iv,
            authenticationTag,
            fileDropV2,
            arbitraryDataProvider,
            user);
    }

    public static String decryptStringSymmetricAsString(byte[] bytes,
                                                        byte[] encryptionKeyBytes,
                                                        byte[] iv,
                                                        byte[] authenticationTag,
                                                        boolean fileDropV2,
                                                        ArbitraryDataProvider arbitraryDataProvider,
                                                        User user)
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


        // check authentication tag
        byte[] extractedAuthenticationTag = Arrays.copyOfRange(bytes,
                                                               bytes.length - (128 / 8),
                                                               bytes.length);

        if (!Arrays.equals(extractedAuthenticationTag, authenticationTag)) {
            reportE2eError(arbitraryDataProvider, user);
            throw new SecurityException("Tag not correct");
        }

        byte[] encodedBytes = cipher.doFinal(bytes);

        if (fileDropV2) {
            return new EncryptionUtilsV2().gZipDecompress(encodedBytes);
        } else {
            return decodeBase64BytesToString(encodedBytes);
        }
    }

    public static EncryptedMetadata encryptStringSymmetric(
        String string,
        byte[] encryptionKeyBytes) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        return encryptStringSymmetric(string, encryptionKeyBytes, ivDelimiter);
    }


    public static EncryptedMetadata encryptStringSymmetric(
        String string,
        byte[] encryptionKeyBytes,
        String delimiter) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {

        byte[] bytes = encodeStringToBase64Bytes(string);

        return encryptStringSymmetric(bytes, encryptionKeyBytes, delimiter);
    }

    /**
     * Encrypt string with AES/GCM/NoPadding
     *
     * @param bytes              byte array
     * @param encryptionKeyBytes key from metadata
     * @return decrypted string
     */
    public static EncryptedMetadata encryptStringSymmetric(
        byte[] bytes,
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

        byte[] cryptedBytes = cipher.doFinal(bytes);

        String encodedCryptedBytes = encodeBytesToBase64String(cryptedBytes);
        String encodedIV = encodeBytesToBase64String(iv);
        String authenticationTag = encodeBytesToBase64String(Arrays.copyOfRange(cryptedBytes,
                                                                                cryptedBytes.length - (128 / 8),
                                                                                cryptedBytes.length));

        return new EncryptedMetadata(encodedCryptedBytes + delimiter + encodedIV, encodedIV, authenticationTag);
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
     * Decrypt string with AES/GCM/NoPadding
     *
     * @param string             string to decrypt
     * @param encryptionKeyBytes key from metadata
     * @param authenticationTag  auth tag to check
     * @return decrypted string
     */
    public static byte[] decryptStringSymmetric(String string,
                                                byte[] encryptionKeyBytes,
                                                String authenticationTag,
                                                String ivString)
        throws NoSuchAlgorithmException,
        InvalidAlgorithmParameterException, NoSuchPaddingException, InvalidKeyException,
        BadPaddingException, IllegalBlockSizeException {

        Cipher cipher = Cipher.getInstance(AES_CIPHER);

        int delimiterPosition = string.lastIndexOf(ivDelimiter);

        String cipherString;
        if (delimiterPosition == -1) {
            cipherString = string;
        } else {
            cipherString = string.substring(0, delimiterPosition);
        }

        byte[] iv = new IvParameterSpec(decodeStringToBase64Bytes(ivString)).getIV();

        Key key = new SecretKeySpec(encryptionKeyBytes, AES);

        GCMParameterSpec spec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, spec);

        byte[] bytes = decodeStringToBase64Bytes(cipherString);

        // check authentication tag
        if (authenticationTag != null) {
            byte[] authenticationTagBytes = decodeStringToBase64Bytes(authenticationTag);
            byte[] extractedAuthenticationTag = Arrays.copyOfRange(bytes,
                                                                   bytes.length - (128 / 8),
                                                                   bytes.length);

            if (!Arrays.equals(extractedAuthenticationTag, authenticationTagBytes)) {
                throw new SecurityException("Tag not correct");
            }
        }

        return cipher.doFinal(bytes);
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

    public static PrivateKey PEMtoPrivateKey(String pem) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] privateKeyBytes = EncryptionUtils.decodeStringToBase64Bytes(pem);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory kf = KeyFactory.getInstance(EncryptionUtils.RSA);
        return kf.generatePrivate(keySpec);
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

    /**
     * Generates private/public key pair, used for asymmetric encryption
     *
     * @return KeyPair
     */
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(RSA);
        keyGen.initialize(2048, new SecureRandom());
        return keyGen.generateKeyPair();
    }

    /**
     * Generates key for symmetric encryption
     *
     * @return byte[] byteArray of key
     */
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

    /**
     * Generates key for symmetric encryption
     *
     * @return String String base64 encoded key
     */
    public static String generateKeyString() {
        return EncryptionUtils.encodeBytesToBase64String(generateKey());
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

    public static String lockFolder(ServerFileInterface parentFile, OwnCloudClient client) throws UploadException {
        return lockFolder(parentFile, client, -1);
    }

    public static String lockFolder(ServerFileInterface parentFile, OwnCloudClient client, long counter) throws UploadException {
        // Lock folder
        LockFileRemoteOperation lockFileOperation = new LockFileRemoteOperation(parentFile.getLocalId(),
                                                                                counter);
        RemoteOperationResult<String> lockFileOperationResult = lockFileOperation.execute(client);

        if (lockFileOperationResult.isSuccess() &&
            !TextUtils.isEmpty(lockFileOperationResult.getResultData())) {
            return lockFileOperationResult.getResultData();
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
    public static Pair<Boolean, DecryptedFolderMetadataFileV1> retrieveMetadataV1(OCFile parentFile,
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
        RemoteOperationResult<MetadataResponse> getMetadataOperationResult = getMetadataOperation.execute(client);

        DecryptedFolderMetadataFileV1 metadata;

        if (getMetadataOperationResult.isSuccess()) {
            // decrypt metadata
            String serializedEncryptedMetadata = getMetadataOperationResult.getResultData().getMetadata();


            EncryptedFolderMetadataFileV1 encryptedFolderMetadata = EncryptionUtils.deserializeJSON(
                serializedEncryptedMetadata, new TypeToken<>() {
                });

            return new Pair<>(Boolean.TRUE, decryptFolderMetaData(encryptedFolderMetadata,
                                                                  privateKey,
                                                                  arbitraryDataProvider,
                                                                  user,
                                                                  localId));

        } else if (getMetadataOperationResult.getHttpCode() == HttpStatus.SC_NOT_FOUND) {
            // TODO extract
            // new metadata
            metadata = new DecryptedFolderMetadataFileV1();
            metadata.setMetadata(new DecryptedMetadata());
            metadata.getMetadata().setVersion(Double.parseDouble(E2EVersion.V1_2.getValue()));
            metadata.getMetadata().setMetadataKeys(new HashMap<>());
            String metadataKey = EncryptionUtils.encodeBytesToBase64String(EncryptionUtils.generateKey());
            String encryptedMetadataKey = EncryptionUtils.encryptStringAsymmetric(metadataKey, publicKey);
            metadata.getMetadata().setMetadataKey(encryptedMetadataKey);

            return new Pair<>(Boolean.FALSE, metadata);
        } else {
            // TODO E2E: error
            throw new UploadException("something wrong");
        }
    }

    /**
     * @param parentFile file metadata should be retrieved for
     * @return Pair: boolean: true: metadata already exists, false: metadata new created
     */
    public static Pair<Boolean, DecryptedFolderMetadataFile> retrieveMetadata(OCFile parentFile,
                                                                              OwnCloudClient client,
                                                                              String privateKey,
                                                                              String publicKey,
                                                                              FileDataStorageManager storageManager,
                                                                              User user,
                                                                              Context context,
                                                                              ArbitraryDataProvider arbitraryDataProvider)
        throws Throwable {

        long localId = parentFile.getLocalId();
        GetMetadataRemoteOperation getMetadataOperation = new GetMetadataRemoteOperation(localId);
        RemoteOperationResult<MetadataResponse> getMetadataOperationResult = getMetadataOperation.execute(client);
        boolean updateMetadata = getMetadataOperationResult.isSuccess();

        if (parentFile.getE2eCounter() == -1 || getMetadataOperationResult.getHttpCode() == HttpStatus.SC_NOT_FOUND ||
            getMetadataOperationResult.getHttpCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
            // new metadata
            DecryptedFolderMetadataFile metadata = new DecryptedFolderMetadataFile(new com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedMetadata(),
                                                       new ArrayList<>(),
                                                       new HashMap<>(),
                                                       E2EVersion.V2_0.getValue());
            metadata.getUsers().add(new DecryptedUser(client.getUserId(), publicKey, null));
            byte[] metadataKey = EncryptionUtils.generateKey();

            if (metadataKey == null) {
                throw new UploadException("Could not encrypt folder!");
            }

            metadata.getMetadata().setMetadataKey(metadataKey);
            metadata.getMetadata().getKeyChecksums().add(new EncryptionUtilsV2().hashMetadataKey(metadataKey));

            return new Pair<>(updateMetadata, metadata);
        }

        if (getMetadataOperationResult.isSuccess()) {
            // decrypt metadata

            final var metadataOperationResult = getMetadataOperationResult.getResultData();
            String serializedEncryptedMetadata = metadataOperationResult.getMetadata();


            EncryptedFolderMetadataFile encryptedFolderMetadata = EncryptionUtils.deserializeJSON(
                serializedEncryptedMetadata, new TypeToken<>() {
                });


            DecryptedFolderMetadataFile metadataFile = new EncryptionUtilsV2().decryptFolderMetadataFile(encryptedFolderMetadata,
                                                                                                         client.getUserId(),
                                                                                                         privateKey,
                                                                                                         parentFile,
                                                                                                         storageManager,
                                                                                                         client,
                                                                                                         parentFile.getE2eCounter(),
                                                                                                         metadataOperationResult.getSignature(),
                                                                                                         user,
                                                                                                         context,
                                                                                                         arbitraryDataProvider);
            return new Pair<>(updateMetadata, metadataFile);
        }

        reportE2eError(arbitraryDataProvider, user);
        throw new UploadException("something wrong");
    }

    public static void uploadMetadata(ServerFileInterface parentFile,
                                      String serializedFolderMetadata,
                                      String token,
                                      OwnCloudClient client,
                                      boolean metadataExists,
                                      E2EVersion version,
                                      String signature,
                                      ArbitraryDataProvider arbitraryDataProvider,
                                      User user) throws UploadException {
        RemoteOperationResult<String> uploadMetadataOperationResult;
        if (metadataExists) {
            // update metadata
            if (version == E2EVersion.V2_0) {
                uploadMetadataOperationResult = new UpdateMetadataV2RemoteOperation(
                    parentFile.getRemoteId(),
                    serializedFolderMetadata,
                    token,
                    signature)
                    .execute(client);
            } else {
                uploadMetadataOperationResult = new UpdateMetadataRemoteOperation(
                    parentFile.getLocalId(),
                    serializedFolderMetadata,
                    token)
                    .execute(client);
            }
        } else {
            // store metadata
            if (version == E2EVersion.V2_0) {
                uploadMetadataOperationResult = new StoreMetadataV2RemoteOperation(
                    parentFile.getRemoteId(),
                    serializedFolderMetadata,
                    token,
                    signature
                )
                    .execute(client);
            } else {
                uploadMetadataOperationResult = new StoreMetadataRemoteOperation(
                    parentFile.getLocalId(),
                    serializedFolderMetadata
                )
                    .execute(client);
            }
        }

        if (!uploadMetadataOperationResult.isSuccess()) {
            reportE2eError(arbitraryDataProvider, user);
            throw new UploadException("Storing/updating metadata was not successful");
        }
    }

    public static RemoteOperationResult<Void> unlockFolder(ServerFileInterface parentFolder, OwnCloudClient client, String token) {
        if (token != null) {
            return new UnlockFileRemoteOperation(parentFolder.getLocalId(), token).execute(client);
        } else {
            return new RemoteOperationResult<>(new Exception("No token available"));
        }
    }

    public static RemoteOperationResult<Void> unlockFolderV1(ServerFileInterface parentFolder, OwnCloudClient client, String token) {
        if (token != null) {
            return new UnlockFileV1RemoteOperation(parentFolder.getLocalId(), token).execute(client);
        } else {
            return new RemoteOperationResult<>(new Exception("No token available"));
        }
    }

    public static X509Certificate convertCertFromString(String string) throws CertificateException {
        String trimmedCert = string.replace("-----BEGIN CERTIFICATE-----\n", "")
            .replace("-----END CERTIFICATE-----\n", "");
        byte[] encodedCert = trimmedCert.getBytes(StandardCharsets.UTF_8);
        byte[] decodedCert = org.apache.commons.codec.binary.Base64.decodeBase64(encodedCert);

        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        InputStream in = new ByteArrayInputStream(decodedCert);
        return (X509Certificate) certFactory.generateCertificate(in);
    }

    public static RSAPublicKey convertPublicKeyFromString(String string) throws CertificateException {
        return (RSAPublicKey) convertCertFromString(string).getPublicKey();
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

    public static String generateChecksum(DecryptedFolderMetadataFileV1 metadataFile,
                                          String mnemonic) throws NoSuchAlgorithmException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(mnemonic.replaceAll(" ", ""));

        ArrayList<String> keys = new ArrayList<>(metadataFile.getFiles().keySet());
        Collections.sort(keys);

        for (String key : keys) {
            stringBuilder.append(key);
        }

        stringBuilder.append(metadataFile.getMetadata().getMetadataKey());

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

    public static void reportE2eError(ArbitraryDataProvider arbitraryDataProvider, User user) {
        arbitraryDataProvider.incrementValue(user.getAccountName(), ArbitraryDataProvider.E2E_ERRORS);

        if (arbitraryDataProvider.getLongValue(user.getAccountName(),
                                               ArbitraryDataProvider.E2E_ERRORS_TIMESTAMP) == -1L) {
            arbitraryDataProvider.storeOrUpdateKeyValue(
                user.getAccountName(),
                ArbitraryDataProvider.E2E_ERRORS_TIMESTAMP,
                System.currentTimeMillis() / 1000
                                                       );
        }
    }

    @Nullable
    public static Problem readE2eError(ArbitraryDataProvider arbitraryDataProvider, User user) {
        int value = arbitraryDataProvider.getIntegerValue(user.getAccountName(),
                                                          ArbitraryDataProvider.E2E_ERRORS);
        long timestamp = arbitraryDataProvider.getLongValue(user.getAccountName(),
                                                            ArbitraryDataProvider.E2E_ERRORS_TIMESTAMP);

        arbitraryDataProvider.deleteKeyForAccount(user.getAccountName(),
                                                  ArbitraryDataProvider.E2E_ERRORS);

        arbitraryDataProvider.deleteKeyForAccount(user.getAccountName(),
                                                  ArbitraryDataProvider.E2E_ERRORS_TIMESTAMP);

        if (value > 0 && timestamp > 0) {
            return new Problem(SendClientDiagnosticRemoteOperation.E2EE_ERRORS, value, timestamp);
        } else {
            return null;
        }
    }

    public static String generateUid() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    public static String retrievePublicKeyForUser(User user, Context context) {
        return new ArbitraryDataProviderImpl(context).getValue(user, PUBLIC_KEY);
    }

    public static byte[] generateIV() {
        return EncryptionUtils.randomBytes(EncryptionUtils.ivLength);
    }

    public static String byteToHex(byte[] bytes) {
        StringBuilder sbKey = new StringBuilder();
        for (byte b : bytes) {
            sbKey.append(String.format("%02X ", b));
        }
        return sbKey.toString();
    }

    public static void savePublicKey(User currentUser,
                                     String key,
                                     String user,
                                     ArbitraryDataProvider arbitraryDataProvider) {
        arbitraryDataProvider.storeOrUpdateKeyValue(currentUser,
                                                    ArbitraryDataProvider.PUBLIC_KEY + user,
                                                    key);
    }

    public static String getPublicKey(User currentUser,
                                      String user,
                                      ArbitraryDataProvider arbitraryDataProvider) {
        return arbitraryDataProvider.getValue(currentUser, ArbitraryDataProvider.PUBLIC_KEY + user);
    }
}
