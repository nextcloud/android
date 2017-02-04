/**
 *   ownCloud Android client application
 *
 *   @author Florian Lentz
 *   Copyright (C) 2017 Florian Lentz
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.owncloud.android.ui.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.v4.app.ActivityCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class FingerprintActivity extends AppCompatActivity {

    public final static String KEY_CHECK_RESULT = "KEY_CHECK_RESULT";

    public final static String PREFERENCE_USE_FINGERPRINT = "use_fingerprint";

    private KeyStore keyStore;
    // Variable used for storing the key in the Android Keystore container
    private static final String KEY_NAME = "Nextcloud";
    private Cipher cipher;

    FingerprintManager fingerprintManager;

    FingerprintHandler helper;

    FingerprintManager.CryptoObject cryptoObject;

    CancellationSignal cancellationSignal;

    /**
     * Initializes the activity.
     *
     * @param savedInstanceState    Previously saved state - irrelevant in this case
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fingerprintlock);
    }

    private void startFingerprint() {
        TextView fingerprinttext = (TextView) findViewById(R.id.scanfingerprinttext);

        fingerprintManager = (FingerprintManager) MainApp.getAppContext().getSystemService(Context.FINGERPRINT_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);

        if (!keyguardManager.isKeyguardSecure()) {
            return;
        }else{
            generateKey();


            if (cipherInit()) {
                cryptoObject = new FingerprintManager.CryptoObject(cipher);
                FingerprintHandler.Callback callback = new FingerprintHandler.Callback() {
                    @Override
                    public void onAuthenticated() {
                        fingerprintresult(true);
                    }

                    @Override
                    public void onFailed(String error) {
                        Toast.makeText(
                                MainApp.getAppContext(),
                                error,
                                Toast.LENGTH_LONG)
                                .show();
                        ImageView imageView = (ImageView)findViewById(R.id.fingerprinticon);
                        int[][] states = new int[][] { new int[] { android.R.attr.state_activated }, new int[] { -android.R.attr.state_activated } };
                        int[] colors = new int[] { Color.parseColor("#FF0000"), Color.RED };
                        ColorStateList csl = new ColorStateList(states, colors);
                        Drawable drawable = DrawableCompat.wrap(imageView.getDrawable());
                        DrawableCompat.setTintList(drawable, csl);
                        imageView.setImageDrawable(drawable);
                    }
                };

                helper = new FingerprintHandler(fingerprinttext, callback);
                cancellationSignal = new CancellationSignal();
                if (ActivityCompat.checkSelfPermission(MainApp.getAppContext(), Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                fingerprintManager.authenticate(cryptoObject, cancellationSignal, 0, helper, null);
            }
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        startFingerprint();
        ImageView imageView = (ImageView)findViewById(R.id.fingerprinticon);
        imageView.setImageDrawable(getDrawable(R.drawable.ic_fingerprint));
    }


    @Override
    public void onStop(){
        super.onStop();
        cancellationSignal.cancel();
    }

    /**
     * Overrides click on the BACK arrow to prevent fingerprint from being worked around.
     *
     * @param keyCode       Key code of the key that triggered the down event.
     * @param event         Event triggered.
     * @return              'True' when the key event was processed by this method.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount()== 0){
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @TargetApi(Build.VERSION_CODES.M)
    protected void generateKey() {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (Exception e) {
            e.printStackTrace();
        }


        KeyGenerator keyGenerator;
        try {
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException("Failed to get KeyGenerator instance", e);
        }


        try {
            keyStore.load(null);
            keyGenerator.init(new
                    KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(
                            KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException |
                InvalidAlgorithmParameterException
                | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }


    @TargetApi(Build.VERSION_CODES.M)
    public boolean cipherInit() {
        try {
            cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get Cipher", e);
        }


        try {
            keyStore.load(null);
            SecretKey key = (SecretKey) keyStore.getKey(KEY_NAME,
                    null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }


    private void fingerprintresult(boolean fingerok) {

            if (fingerok) {
                Intent resultIntent = new Intent();
                resultIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                resultIntent.putExtra(KEY_CHECK_RESULT, true);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                showErrorAndRestart(R.string.fingerprint_unknown);
            }


    }


    private void showErrorAndRestart(int errorMessage) {
        CharSequence errorSeq = getString(errorMessage);
        Toast.makeText(this, errorSeq, Toast.LENGTH_LONG).show();
    }



    final static public boolean isFingerprintCapable(Context context) {
        FingerprintManager fingerprintManager = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!fingerprintManager.isHardwareDetected()) {
                // Device doesn't support fingerprint authentication
                return false;
            } else if (!fingerprintManager.hasEnrolledFingerprints()) {
                // User hasn't enrolled any fingerprints to authenticate with
                return true;
            } else {
                // Everything is ready for fingerprint authentication
                return true;
            }
        }
        return false;
    }

    final static public boolean isFingerprintReady(Context context) {
        FingerprintManager fingerprintManager = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return fingerprintManager.isHardwareDetected() && fingerprintManager.hasEnrolledFingerprints();
        }
        return false;
    }



}
@SuppressLint("NewApi")
class FingerprintHandler extends FingerprintManager.AuthenticationCallback {


    private TextView text;
    private Callback callback;


    // Constructor
    FingerprintHandler(TextView mtext, Callback mcallback) {
        text = mtext;
        callback = mcallback;
    }



    @Override
    public void onAuthenticationError(int errMsgId, CharSequence errString) {
  //      this.update(String.valueOf(errString), false);
    }


    @Override
    public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
        this.update(String.valueOf(helpString), false);
    }


    @Override
    public void onAuthenticationFailed() {
        this.update(MainApp.getAppContext().getString(R.string.fingerprint_unknown), false);
    }


    @Override
    public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
        this.update("Fingerprint Authentication succeeded.", true);
    }


    public void update(final String e, Boolean success) {
        if(success) {
            text.postDelayed(new Runnable() {
                @Override
                public void run() {
                    callback.onAuthenticated();
                }
            }, 0);
        } else {
            text.postDelayed(new Runnable() {
                @Override
                public void run() {
                    callback.onFailed(e);
                }
            }, 0);
        }
    }

    interface Callback {

        void onAuthenticated();

        void onFailed(String error);

    }
}