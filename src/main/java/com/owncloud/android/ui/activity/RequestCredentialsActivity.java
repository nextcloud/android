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
package com.owncloud.android.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import com.owncloud.android.R;
import com.owncloud.android.utils.DeviceCredentialUtils;
import com.owncloud.android.utils.DisplayUtils;

import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.biometrics.BiometricConstants;
import androidx.biometrics.BiometricPrompt;
import androidx.fragment.app.FragmentActivity;

/**
 * Dummy activity that is used to handle the device's default authentication workflow.
 */
@RequiresApi(Build.VERSION_CODES.M)
public class RequestCredentialsActivity extends FragmentActivity {

    private static final String TAG = RequestCredentialsActivity.class.getSimpleName();

    public final static String KEY_CHECK_RESULT = "KEY_CHECK_RESULT";
    public final static int KEY_CHECK_RESULT_TRUE = 1;
    public final static int KEY_CHECK_RESULT_FALSE = 0;
    public final static int KEY_CHECK_RESULT_CANCEL = -1;
    private static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 1;
    private BiometricPrompt prompt;

    private void showMessage(String s) {
        DisplayUtils.showSnackMessage(this, s);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS) {
            if (resultCode == Activity.RESULT_OK && DeviceCredentialUtils.tryEncrypt(getApplicationContext())) {
                finishWithResult(KEY_CHECK_RESULT_TRUE);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                finishWithResult(KEY_CHECK_RESULT_CANCEL);
            } else {
                Toast.makeText(this, R.string.default_credentials_wrong, Toast.LENGTH_SHORT).show();
                requestCredentials();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (DeviceCredentialUtils.areCredentialsAvailable(this)) {
//            DeviceCredentialUtils.createKey(getApplicationContext());
            requestCredentials();


        } else {
            DisplayUtils.showSnackMessage(this, R.string.prefs_lock_device_credentials_not_setup);
            finishWithResult(KEY_CHECK_RESULT_TRUE);
        }
    }

    private void requestCredentials() {

        BiometricPrompt.AuthenticationCallback callback = new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                switch (errorCode) {
                    case BiometricConstants.ERROR_USER_CANCELED:
                    case BiometricConstants.ERROR_NEGATIVE_BUTTON:
                    case BiometricConstants.ERROR_CANCELED:
                        finishWithResult(KEY_CHECK_RESULT_CANCEL);
                        break;

                    case BiometricConstants.ERROR_TIMEOUT:
                    case BiometricConstants.ERROR_LOCKOUT:
                    case BiometricConstants.ERROR_LOCKOUT_PERMANENT:
                        showMessage(errString.toString());
                        // finishWithResult(KEY_CHECK_RESULT_CANCEL);
                        break;

                    default:
                        showMessage(errString.toString());
                        requestCredentials();
                        break;
                }
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                finishWithResult(KEY_CHECK_RESULT_TRUE);
            }

            @Override
            public void onAuthenticationFailed() {
                showMessage("failed");
                // finishWithResult(KEY_CHECK_RESULT_CANCEL);
            }
        };

        BiometricPrompt.PromptInfo test = new BiometricPrompt.PromptInfo.Builder()
            .setTitle("Bio")
            .setSubtitle("Subtitle")
            .setDescription("Description")
            .setNegativeButtonText("Cancel")
            .build();

        prompt = new BiometricPrompt(this, Executors.newCachedThreadPool(), callback);
        prompt.authenticate(test);

//        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
//        if (keyguardManager != null) {
//            Intent i = keyguardManager.createConfirmDeviceCredentialIntent(null, null);
//            startActivityForResult(i, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS);
//        } else {
//            Log_OC.e(TAG, "Keyguard manager is null");
//            finishWithResult(KEY_CHECK_RESULT_FALSE);
//        }
    }

    private void finishWithResult(int success) {
        Intent resultIntent = new Intent();
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        resultIntent.putExtra(KEY_CHECK_RESULT, success);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        finishWithResult(KEY_CHECK_RESULT_CANCEL);
    }
}
