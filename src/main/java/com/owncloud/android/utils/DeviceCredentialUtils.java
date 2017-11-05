/*
 *  Nextcloud Android client application
 *
 *  @author Harikrishnan Rajan
 *  Copyright (C) 2017
 *  Copyright (C) 2017 Nextcloud GmbH.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License version 3,
 *  as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program. If not, see http://www.gnu.org/licenses/.
 *
 */
package com.owncloud.android.utils;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

/**
 * Utility class with methods for handling device credentials.
 */
@RequiresApi(Build.VERSION_CODES.M)
public class DeviceCredentialUtils {

    private static final String TAG = DeviceCredentialUtils.class.getSimpleName();

    private static final int AUTHENTICATION_DURATION_SECONDS = 30;

    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";

    public static boolean areCredentialsAvailable(Context context) {
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(
                Context.KEYGUARD_SERVICE);
        if (keyguardManager != null) {
            return keyguardManager.isKeyguardSecure();
        } else {
            Log_OC.e(TAG, "Keyguard manager is null");
            return false;
        }
    }

    /**
     * Creates a symmetric key in the Android Key Store which can only be used after the user has
     * authenticated with device credentials within the last X seconds.
     */
    public static void createKey(Context context) {
        // Generate a key to decrypt payment credentials, tokens, etc.
        final String keyName = context.getResources().getString(R.string.secret_key_name);
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);

            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder
            keyGenerator.init(new KeyGenParameterSpec.Builder(keyName,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    // Require that the user has unlocked in the last 30 seconds
                    .setUserAuthenticationValidityDurationSeconds(AUTHENTICATION_DURATION_SECONDS)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException | NoSuchProviderException
                | InvalidAlgorithmParameterException | KeyStoreException
                | CertificateException | IOException e) {
            Log_OC.e(TAG, "Exception: " + e.getMessage());
        }
    }

    /**
     * Tries to encrypt some data with the generated key in {@link #createKey} which
     * only works if the user has just authenticated via device credentials.
     */
    public static boolean tryEncrypt(Context context) {
        try {
            final String keyName = context.getResources().getString(R.string.secret_key_name);
            final int[] secretIntArray = context.getResources().getIntArray(R.array.secret_byte_array);
            ByteBuffer byteBuffer = ByteBuffer.allocate(secretIntArray.length * 4);
            IntBuffer intBuffer = byteBuffer.asIntBuffer();
            intBuffer.put(secretIntArray);
            byte[] secretByteArray = byteBuffer.array();
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(keyName, null);
            Cipher cipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7);

            // Try encrypting something, it will only work if the user authenticated within
            // the last AUTHENTICATION_DURATION_SECONDS seconds.
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            cipher.doFinal(secretByteArray);

            // If the user has recently authenticated, you will reach here.
            return true;
        } catch (BadPaddingException | IllegalBlockSizeException | KeyStoreException |
                CertificateException | UnrecoverableKeyException | IOException
                | NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException e) {
            return false;
        }
    }
}
