/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017-2018 Mario Danic <mario@lovelyhq.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;

import com.google.gson.Gson;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.client.preferences.AppPreferencesImpl;
import com.nextcloud.common.NextcloudClient;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl;
import com.owncloud.android.datamodel.PushConfigurationState;
import com.owncloud.android.datamodel.SignatureVerification;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.notifications.RegisterAccountDeviceForNotificationsOperation;
import com.owncloud.android.lib.resources.notifications.RegisterAccountDeviceForProxyOperation;
import com.owncloud.android.lib.resources.notifications.UnregisterAccountDeviceForNotificationsOperation;
import com.owncloud.android.lib.resources.notifications.UnregisterAccountDeviceForProxyOperation;
import com.owncloud.android.lib.resources.notifications.models.PushResponse;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Locale;

public final class PushUtils {

    public static final String KEY_PUSH = "push";
    private static final String TAG = "PushUtils";
    private static final String KEYPAIR_FOLDER = "nc-keypair";
    private static final String KEYPAIR_FILE_NAME = "push_key";
    private static final String KEYPAIR_PRIV_EXTENSION = ".priv";
    private static final String KEYPAIR_PUB_EXTENSION = ".pub";
    private static ArbitraryDataProvider arbitraryDataProvider;

    private PushUtils() {
    }

    public static String generateSHA512Hash(String pushToken) {
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("SHA-512");
            messageDigest.update(pushToken.getBytes());
            return EncryptionUtils.bytesToHex(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            Log_OC.d(TAG, "SHA-512 algorithm not supported");
        }
        return "";
    }

    private static int generateRsa2048KeyPair() {
        migratePushKeys();
        String keyPath = MainApp.getAppContext().getFilesDir().getAbsolutePath() + File.separator +
                MainApp.getDataFolder() + File.separator + KEYPAIR_FOLDER;

        String privateKeyPath = keyPath + File.separator + KEYPAIR_FILE_NAME + KEYPAIR_PRIV_EXTENSION;
        String publicKeyPath = keyPath + File.separator + KEYPAIR_FILE_NAME + KEYPAIR_PUB_EXTENSION;
        File keyPathFile = new File(keyPath);

        if (!new File(privateKeyPath).exists() && !new File(publicKeyPath).exists()) {
            try {
                if (!keyPathFile.exists()) {
                    keyPathFile.mkdir();
                }
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                keyGen.initialize(2048);

                KeyPair pair = keyGen.generateKeyPair();
                int statusPrivate = saveKeyToFile(pair.getPrivate(), privateKeyPath);
                int statusPublic = saveKeyToFile(pair.getPublic(), publicKeyPath);

                if (statusPrivate == 0 && statusPublic == 0) {
                    // all went well
                    return 0;
                } else {
                    return -2;
                }
            } catch (NoSuchAlgorithmException e) {
                Log_OC.d(TAG, "RSA algorithm not supported");
            }
        } else {
            // we already have the key
            return -1;
        }

        // we failed to generate the key
        return -2;
    }

    private static void deleteRegistrationForAccount(Account account) {
        Context context = MainApp.getAppContext();
        OwnCloudAccount ocAccount;
        arbitraryDataProvider = new ArbitraryDataProviderImpl(MainApp.getAppContext());

        try {
            ocAccount = new OwnCloudAccount(account, context);
            NextcloudClient mClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                getNextcloudClientFor(ocAccount, context);

            RemoteOperationResult<Void> remoteOperationResult =
                new UnregisterAccountDeviceForNotificationsOperation().execute(mClient);

            if (remoteOperationResult.getHttpCode() == HttpStatus.SC_ACCEPTED) {
                String arbitraryValue;
                if (!TextUtils.isEmpty(arbitraryValue = arbitraryDataProvider.getValue(account.name, KEY_PUSH))) {
                    Gson gson = new Gson();
                    PushConfigurationState pushArbitraryData = gson.fromJson(arbitraryValue,
                            PushConfigurationState.class);
                    RemoteOperationResult unregisterResult = new UnregisterAccountDeviceForProxyOperation(
                        context.getResources().getString(R.string.push_server_url),
                        pushArbitraryData.getDeviceIdentifier(),
                        pushArbitraryData.getDeviceIdentifierSignature(),
                        pushArbitraryData.getUserPublicKey()).run();

                    if (unregisterResult.isSuccess()) {
                        arbitraryDataProvider.deleteKeyForAccount(account.name, KEY_PUSH);
                    }
                }
            }
        } catch (com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException e) {
            Log_OC.d(TAG, "Failed to find an account");
        } catch (AuthenticatorException e) {
            Log_OC.d(TAG, "Failed via AuthenticatorException");
        } catch (IOException e) {
            Log_OC.d(TAG, "Failed via IOException");
        } catch (OperationCanceledException e) {
            Log_OC.d(TAG, "Failed via OperationCanceledException");
        }
    }

    public static void pushRegistrationToServer(final UserAccountManager accountManager, final String token) {
        arbitraryDataProvider = new ArbitraryDataProviderImpl(MainApp.getAppContext());

        if (!TextUtils.isEmpty(MainApp.getAppContext().getResources().getString(R.string.push_server_url)) &&
                !TextUtils.isEmpty(token)) {
            PushUtils.generateRsa2048KeyPair();
            String pushTokenHash = PushUtils.generateSHA512Hash(token).toLowerCase(Locale.ROOT);
            PublicKey devicePublicKey = (PublicKey) PushUtils.readKeyFromFile(true);
            if (devicePublicKey != null) {
                byte[] publicKeyBytes = Base64.encode(devicePublicKey.getEncoded(), Base64.NO_WRAP);
                String publicKey = new String(publicKeyBytes);
                publicKey = publicKey.replaceAll("(.{64})", "$1\n");

                publicKey = "-----BEGIN PUBLIC KEY-----\n" + publicKey + "\n-----END PUBLIC KEY-----\n";

                Context context = MainApp.getAppContext();
                String providerValue;
                PushConfigurationState accountPushData;
                Gson gson = new Gson();
                for (Account account : accountManager.getAccounts()) {
                    providerValue = arbitraryDataProvider.getValue(account.name, KEY_PUSH);
                    if (!TextUtils.isEmpty(providerValue)) {
                        accountPushData = gson.fromJson(providerValue,
                                PushConfigurationState.class);
                    } else {
                        accountPushData = null;
                    }

                    if (accountPushData != null && !accountPushData.getPushToken().equals(token) &&
                            !accountPushData.isShouldBeDeleted() ||
                            TextUtils.isEmpty(providerValue)) {
                        try {
                            OwnCloudAccount ocAccount = new OwnCloudAccount(account, context);
                            NextcloudClient client = OwnCloudClientManagerFactory.getDefaultSingleton().
                                getNextcloudClientFor(ocAccount, context);

                            RemoteOperationResult<PushResponse> remoteOperationResult =
                                new RegisterAccountDeviceForNotificationsOperation(pushTokenHash,
                                                                                   publicKey,
                                                                                   context.getResources().getString(R.string.push_server_url))
                                    .execute(client);

                            if (remoteOperationResult.isSuccess()) {
                                PushResponse pushResponse = remoteOperationResult.getResultData();

                                RemoteOperationResult resultProxy = new RegisterAccountDeviceForProxyOperation(
                                    context.getResources().getString(R.string.push_server_url),
                                    token, pushResponse.getDeviceIdentifier(),
                                    pushResponse.getSignature(),
                                    pushResponse.getPublicKey(),
                                    MainApp.getUserAgent())
                                    .run();

                                if (resultProxy.isSuccess()) {
                                    PushConfigurationState pushArbitraryData = new PushConfigurationState(token,
                                            pushResponse.getDeviceIdentifier(), pushResponse.getSignature(),
                                            pushResponse.getPublicKey(), false);
                                    arbitraryDataProvider.storeOrUpdateKeyValue(account.name, KEY_PUSH,
                                            gson.toJson(pushArbitraryData));
                                }
                            } else if (remoteOperationResult.getCode() ==
                                    RemoteOperationResult.ResultCode.ACCOUNT_USES_STANDARD_PASSWORD) {
                                arbitraryDataProvider.storeOrUpdateKeyValue(account.name,
                                        UserAccountManager.ACCOUNT_USES_STANDARD_PASSWORD, "true");
                            }
                        } catch (com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException e) {
                            Log_OC.d(TAG, "Failed to find an account");
                        } catch (AuthenticatorException e) {
                            Log_OC.d(TAG, "Failed via AuthenticatorException");
                        } catch (IOException e) {
                            Log_OC.d(TAG, "Failed via IOException");
                        } catch (OperationCanceledException e) {
                            Log_OC.d(TAG, "Failed via OperationCanceledException");
                        }
                    } else if (accountPushData != null && accountPushData.isShouldBeDeleted()) {
                        deleteRegistrationForAccount(account);
                    }
                }
            }
        }
    }

    public static Key readKeyFromFile(boolean readPublicKey) {
        String keyPath = MainApp.getAppContext().getFilesDir().getAbsolutePath() + File.separator +
                MainApp.getDataFolder() + File.separator + KEYPAIR_FOLDER;

        String privateKeyPath = keyPath + File.separator + KEYPAIR_FILE_NAME + KEYPAIR_PRIV_EXTENSION;
        String publicKeyPath = keyPath + File.separator + KEYPAIR_FILE_NAME + KEYPAIR_PUB_EXTENSION;

        String path;

        if (readPublicKey) {
            path = publicKeyPath;
        } else {
            path = privateKeyPath;
        }

        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(path);
            byte[] bytes = new byte[fileInputStream.available()];
            fileInputStream.read(bytes);

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            if (readPublicKey) {
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(bytes);
                return keyFactory.generatePublic(keySpec);
            } else {
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytes);
                return keyFactory.generatePrivate(keySpec);
            }

        } catch (FileNotFoundException e) {
            Log_OC.d(TAG, "Failed to find path while reading the Key");
        } catch (IOException e) {
            Log_OC.d(TAG, "IOException while reading the key");
        } catch (InvalidKeySpecException e) {
            Log_OC.d(TAG, "InvalidKeySpecException while reading the key");
        } catch (NoSuchAlgorithmException e) {
            Log_OC.d(TAG, "RSA algorithm not supported");
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    Log_OC.e(TAG, "Error closing input stream during reading key from file", e);
                }
            }
        }

        return null;
    }

    private static int saveKeyToFile(Key key, String path) {
        byte[] encoded = key.getEncoded();
        FileOutputStream keyFileOutputStream = null;
        try {
            if (!new File(path).exists()) {
                File newFile = new File(path);
                newFile.getParentFile().mkdirs();
                newFile.createNewFile();
            }
            keyFileOutputStream = new FileOutputStream(path);
            keyFileOutputStream.write(encoded);
            return 0;
        } catch (FileNotFoundException e) {
            Log_OC.d(TAG, "Failed to save key to file");
        } catch (IOException e) {
            Log_OC.d(TAG, "Failed to save key to file via IOException");
        } finally {
            if (keyFileOutputStream != null) {
                try {
                    keyFileOutputStream.close();
                } catch (IOException e) {
                    Log_OC.e(TAG, "Error closing input stream during reading key from file", e);
                }
            }
        }

        return -1;
    }

    public static void reinitKeys(final UserAccountManager accountManager) {
        Context context = MainApp.getAppContext();
        Account[] accounts = accountManager.getAccounts();
        for (Account account : accounts) {
            deleteRegistrationForAccount(account);
        }

        String keyPath = context.getDir("nc-keypair", Context.MODE_PRIVATE).getAbsolutePath();
        File privateKeyFile = new File(keyPath, "push_key.priv");
        File publicKeyFile = new File(keyPath, "push_key.pub");

        FileUtils.deleteQuietly(privateKeyFile);
        FileUtils.deleteQuietly(publicKeyFile);

        AppPreferences preferences = AppPreferencesImpl.fromContext(context);
        String pushToken = preferences.getPushToken();
        pushRegistrationToServer(accountManager, pushToken);
        preferences.setKeysReInitEnabled();
    }

    private static void migratePushKeys() {
        Context context = MainApp.getAppContext();
        AppPreferences preferences = AppPreferencesImpl.fromContext(context);
        if (!preferences.isKeysMigrationEnabled()) {
            String oldKeyPath = MainApp.getStoragePath() + File.separator + MainApp.getDataFolder()
                    + File.separator + "nc-keypair";
            File oldPrivateKeyFile = new File(oldKeyPath, "push_key.priv");
            File oldPublicKeyFile = new File(oldKeyPath, "push_key.pub");

            String keyPath = context.getDir("nc-keypair", Context.MODE_PRIVATE).getAbsolutePath();
            File privateKeyFile = new File(keyPath, "push_key.priv");
            File publicKeyFile = new File(keyPath, "push_key.pub");

            if ((privateKeyFile.exists() && publicKeyFile.exists()) ||
                    (!oldPrivateKeyFile.exists() && !oldPublicKeyFile.exists())) {
                preferences.setKeysMigrationEnabled(true);
            } else {
                if (oldPrivateKeyFile.exists()) {
                    FileStorageUtils.moveFile(oldPrivateKeyFile, privateKeyFile);
                }

                if (oldPublicKeyFile.exists()) {
                    FileStorageUtils.moveFile(oldPublicKeyFile, publicKeyFile);
                }

                if (privateKeyFile.exists() && publicKeyFile.exists()) {
                    preferences.setKeysMigrationEnabled(true);
                }
            }
        }
    }

    public static SignatureVerification verifySignature(
        final Context context,
        final UserAccountManager accountManager,
        final byte[] signatureBytes,
        final byte[] subjectBytes
    ) {
        Signature signature;
        PublicKey publicKey;

        Account[] accounts = accountManager.getAccounts();

        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProviderImpl(context);
        String arbitraryValue;
        Gson gson = new Gson();
        PushConfigurationState pushArbitraryData;

        try {
            signature = Signature.getInstance("SHA512withRSA");
            if (accounts.length > 0) {
                for (Account account : accounts) {
                    if (!TextUtils.isEmpty(arbitraryValue = arbitraryDataProvider.getValue(account.name, KEY_PUSH))) {
                        pushArbitraryData = gson.fromJson(arbitraryValue, PushConfigurationState.class);
                        if (!pushArbitraryData.isShouldBeDeleted()) {
                            publicKey = (PublicKey) readKeyFromString(true, pushArbitraryData.getUserPublicKey());
                            signature.initVerify(publicKey);
                            signature.update(subjectBytes);
                            if (signature.verify(signatureBytes)) {
                                return new SignatureVerification(true, account);
                            }
                        }
                    }
                }
            }
        } catch (NoSuchAlgorithmException e) {
            Log_OC.d(TAG, "No such algorithm");
        } catch (InvalidKeyException e) {
            Log_OC.d(TAG, "Invalid key while trying to verify");
        } catch (SignatureException e) {
            Log_OC.d(TAG, "Signature exception while trying to verify");
        }

        return new SignatureVerification(false, null);
    }

    private static Key readKeyFromString(boolean readPublicKey, String keyString) {
        String modifiedKey;
        if (readPublicKey) {
            modifiedKey = keyString.replaceAll("\\n", "").replace("-----BEGIN PUBLIC KEY-----",
                    "").replace("-----END PUBLIC KEY-----", "");
        } else {
            modifiedKey = keyString.replaceAll("\\n", "").replace("-----BEGIN PRIVATE KEY-----",
                    "").replace("-----END PRIVATE KEY-----", "");
        }

        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
            if (readPublicKey) {
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.decode(modifiedKey, Base64.DEFAULT));
                return keyFactory.generatePublic(keySpec);
            } else {
                PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.decode(modifiedKey, Base64.DEFAULT));
                return keyFactory.generatePrivate(keySpec);
            }
        } catch (NoSuchAlgorithmException e) {
            Log_OC.d("TAG", "No such algorithm while reading key from string");
        } catch (InvalidKeySpecException e) {
            Log_OC.d("TAG", "Invalid key spec while reading key from string");
        }

        return null;
    }
}
