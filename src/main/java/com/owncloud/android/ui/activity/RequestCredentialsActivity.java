package com.owncloud.android.ui.activity;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.RequiresApi;
import android.widget.Toast;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.AnalyticsUtils;
import com.owncloud.android.utils.DeviceCredentialUtils;

/**
 * Dummy activity that is used to handle the device's default authentication workflow.
 */
@RequiresApi(value = 23)
public class RequestCredentialsActivity extends Activity {

    private static final String TAG = RequestCredentialsActivity.class.getSimpleName();
    private static final String SCREEN_NAME = "Device credentials";

    public final static String KEY_CHECK_RESULT = "KEY_CHECK_RESULT";
    private static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 1;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS) {
            if (resultCode == Activity.RESULT_OK && DeviceCredentialUtils.tryEncrypt()) {
                finishWithResult(true);
            } else {
                Toast.makeText(this, R.string.default_credentials_wrong, Toast.LENGTH_SHORT).show();
                requestCredentials();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtils.setCurrentScreenName(this, SCREEN_NAME, TAG);
        DeviceCredentialUtils.createKey();
        requestCredentials();
    }

    private void requestCredentials() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager != null) {
            Intent i = keyguardManager.createConfirmDeviceCredentialIntent(null, null);
            startActivityForResult(i, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS);
        } else {
            Log_OC.e(TAG, "Keyguard manager is null");
            finishWithResult(false);
        }
    }

    private void finishWithResult(boolean success) {
        Intent resultIntent = new Intent();
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        resultIntent.putExtra(KEY_CHECK_RESULT, success);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }
}
