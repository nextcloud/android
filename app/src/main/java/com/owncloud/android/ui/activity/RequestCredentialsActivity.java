/*
 *  Nextcloud Android client application
 *
 *  @author Harikrishnan Rajan
 *  Copyright (C) 2017
 *  Copyright (C) 2017 Nextcloud GmbH.
 *
 *  SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 *
 */
package com.owncloud.android.ui.activity;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;

import com.nextcloud.client.preferences.AppPreferencesImpl;
import com.owncloud.android.R;
import com.owncloud.android.authentication.PassCodeManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.DeviceCredentialUtils;
import com.owncloud.android.utils.DisplayUtils;

import androidx.annotation.Nullable;

/**
 * Dummy activity that is used to handle the device's default authentication workflow.
 */
public class RequestCredentialsActivity extends Activity {

    private static final String TAG = RequestCredentialsActivity.class.getSimpleName();

    public final static String KEY_CHECK_RESULT = "KEY_CHECK_RESULT";
    public final static int KEY_CHECK_RESULT_TRUE = 1;
    public final static int KEY_CHECK_RESULT_FALSE = 0;
    public final static int KEY_CHECK_RESULT_CANCEL = -1;
    private static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PassCodeManager.Companion.setSecureFlag(this,true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS) {
            if (resultCode == Activity.RESULT_OK) {
                AppPreferencesImpl.fromContext(this).setLockTimestamp(SystemClock.elapsedRealtime());
                finishWithResult(KEY_CHECK_RESULT_TRUE);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                finishWithResult(KEY_CHECK_RESULT_CANCEL);
            } else {
                DisplayUtils.showSnackMessage(this, R.string.default_credentials_wrong);
                requestCredentials();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (DeviceCredentialUtils.areCredentialsAvailable(this)) {
            requestCredentials();
        } else {
            DisplayUtils.showSnackMessage(this, R.string.prefs_lock_device_credentials_not_setup);
            finishWithResult(KEY_CHECK_RESULT_CANCEL);
        }
    }

    private void requestCredentials() {
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager != null) {
            Intent i = keyguardManager.createConfirmDeviceCredentialIntent(null, null);
            i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivityForResult(i, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS);
        } else {
            Log_OC.e(TAG, "Keyguard manager is null");
            finishWithResult(KEY_CHECK_RESULT_FALSE);
        }
    }

    private void finishWithResult(int success) {
        Intent resultIntent = new Intent();
        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        resultIntent.putExtra(KEY_CHECK_RESULT, success);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    @Override
    protected void onDestroy() {
        PassCodeManager.Companion.setSecureFlag(this,false);
        super.onDestroy();
    }
}
