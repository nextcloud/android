/**
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.utils;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;

import com.google.gson.Gson;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.PushConfigurationState;
import com.owncloud.android.db.PreferenceManager;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.notifications.RegisterAccountDeviceForNotificationsOperation;
import com.owncloud.android.lib.resources.notifications.RegisterAccountDeviceForProxyOperation;
import com.owncloud.android.lib.resources.notifications.UnregisterAccountDeviceForNotificationsOperation;
import com.owncloud.android.lib.resources.notifications.UnregisterAccountDeviceForProxyOperation;
import com.owncloud.android.lib.resources.notifications.models.PushResponse;

import org.apache.commons.httpclient.HttpStatus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class PushUtils {

    private static final String TAG = "PushUtils";
    private static final String KEYPAIR_FOLDER = "nc-keypair";
    private static final String KEYPAIR_FILE_NAME = "push_key";
    private static final String KEYPAIR_PRIV_EXTENSION = ".priv";
    private static final String KEYPAIR_PUB_EXTENSION = ".pub";

    public static final String KEY_PUSH = "push";

    private static ArbitraryDataProvider arbitraryDataProvider;

    public static String generateSHA512Hash(String pushToken) {
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("SHA-512");
            messageDigest.update(pushToken.getBytes());
            return bytesToHex(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            Log_OC.d(TAG, "SHA-512 algorithm not supported");
        }
        return "";
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte individualByte : bytes) {
            result.append(Integer.toString((individualByte & 0xff) + 0x100, 16)
                    .substring(1));
        }
        return result.toString();
    }
    
    private static int generateRsa2048KeyPair() {
        String keyPath = MainApp.getStoragePath() + File.separator + MainApp.getDataFolder() + File.separator
                + KEYPAIR_FOLDER;

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
        OwnCloudAccount ocAccount = null;
        arbitraryDataProvider = new ArbitraryDataProvider(MainApp.getAppContext().getContentResolver());

        try {
            ocAccount = new OwnCloudAccount(account, context);
            OwnCloudClient mClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                    getClientFor(ocAccount, context);

            RemoteOperation unregisterAccountDeviceForNotificationsOperation = new
                    UnregisterAccountDeviceForNotificationsOperation();

            RemoteOperationResult remoteOperationResult = unregisterAccountDeviceForNotificationsOperation.
                    execute(mClient);

            if (remoteOperationResult.getHttpCode() == HttpStatus.SC_ACCEPTED) {
                String arbitraryValue;
                if (!TextUtils.isEmpty(arbitraryValue = arbitraryDataProvider.getValue(account, KEY_PUSH))) {
                    Gson gson = new Gson();
                    PushConfigurationState pushArbitraryData = gson.fromJson(arbitraryValue,
                            PushConfigurationState.class);
                    RemoteOperation unregisterAccountDeviceForProxyOperation =
                            new UnregisterAccountDeviceForProxyOperation(context.getResources().
                                    getString(R.string.push_server_url),
                                    pushArbitraryData.getDeviceIdentifier(),
                                    pushArbitraryData.getDeviceIdentifierSignature(),
                                    pushArbitraryData.getUserPublicKey());

                    remoteOperationResult = unregisterAccountDeviceForProxyOperation.execute(mClient);

                    if (remoteOperationResult.isSuccess()) {
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

    public static void pushRegistrationToServer() {
        String token = PreferenceManager.getPushToken(MainApp.getAppContext());
        arbitraryDataProvider = new ArbitraryDataProvider(MainApp.getAppContext().getContentResolver());

        if (!TextUtils.isEmpty(MainApp.getAppContext().getResources().getString(R.string.push_server_url)) &&
                !TextUtils.isEmpty(token)) {
            PushUtils.generateRsa2048KeyPair();
            String pushTokenHash = PushUtils.generateSHA512Hash(token).toLowerCase();
            PublicKey devicePublicKey = (PublicKey) PushUtils.readKeyFromFile(true);
            if (devicePublicKey != null) {
                byte[] publicKeyBytes = Base64.encode(devicePublicKey.getEncoded(), Base64.NO_WRAP);
                String publicKey = new String(publicKeyBytes);
                publicKey = publicKey.replaceAll("(.{64})", "$1\n");

                publicKey = "-----BEGIN PUBLIC KEY-----\n" + publicKey + "\n-----END PUBLIC KEY-----\n";

                Context context = MainApp.getAppContext();
                String providerValue;
                PushConfigurationState accountPushData = null;
                Gson gson = new Gson();
                for (Account account : AccountUtils.getAccounts(context)) {
                    providerValue = arbitraryDataProvider.getValue(account, KEY_PUSH);
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
                            OwnCloudClient mClient = OwnCloudClientManagerFactory.getDefaultSingleton().
                                    getClientFor(ocAccount, context);

                            RemoteOperation registerAccountDeviceForNotificationsOperation =
                                    new RegisterAccountDeviceForNotificationsOperation(pushTokenHash,
                                            publicKey,
                                            context.getResources().getString(R.string.push_server_url));

                            RemoteOperationResult remoteOperationResult = registerAccountDeviceForNotificationsOperation.
                                    execute(mClient);

                            if (remoteOperationResult.isSuccess()) {
                                PushResponse pushResponse = remoteOperationResult.getPushResponseData();

                                RemoteOperation registerAccountDeviceForProxyOperation = new
                                        RegisterAccountDeviceForProxyOperation(
                                        context.getResources().getString(R.string.push_server_url),
                                        token, pushResponse.getDeviceIdentifier(), pushResponse.getSignature(),
                                        pushResponse.getPublicKey());

                                remoteOperationResult = registerAccountDeviceForProxyOperation.execute(mClient);

                                if (remoteOperationResult.isSuccess()) {
                                    PushConfigurationState pushArbitraryData = new PushConfigurationState(token,
                                            pushResponse.getDeviceIdentifier(), pushResponse.getSignature(),
                                            pushResponse.getPublicKey(), false);
                                    arbitraryDataProvider.storeOrUpdateKeyValue(account.name, KEY_PUSH,
                                            gson.toJson(pushArbitraryData));
                                }
                            } else if (remoteOperationResult.getCode() ==
                                    RemoteOperationResult.ResultCode.ACCOUNT_USES_STANDARD_PASSWORD) {
                                arbitraryDataProvider.storeOrUpdateKeyValue(account.name,
                                        AccountUtils.ACCOUNT_USES_STANDARD_PASSWORD, "true");
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
        String keyPath = MainApp.getStoragePath() + File.separator + MainApp.getDataFolder() + File.separator
                + KEYPAIR_FOLDER;

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
            fileInputStream.close();

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
        }

        return null;
    }

    private static int saveKeyToFile(Key key, String path) {
        byte[] encoded = key.getEncoded();
        FileOutputStream keyFileOutputStream = null;
        try {
            if (!new File(path).exists()) {
                new File(path).createNewFile();
            }
            keyFileOutputStream = new FileOutputStream(path);
            keyFileOutputStream.write(encoded);
            keyFileOutputStream.close();
            return 0;
        } catch (FileNotFoundException e) {
            Log_OC.d(TAG, "Failed to save key to file");
        } catch (IOException e) {
            Log_OC.d(TAG, "Failed to save key to file via IOException");
        }

        return -1;
    }
}
