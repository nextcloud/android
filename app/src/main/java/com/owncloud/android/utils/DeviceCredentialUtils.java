/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 Harikrishnan Rajan <rhari991@gmail.com>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils;

import android.app.KeyguardManager;
import android.content.Context;

import com.owncloud.android.lib.common.utils.Log_OC;

/**
 * Utility class with methods for handling device credentials.
 */
public final class DeviceCredentialUtils {

    private static final String TAG = DeviceCredentialUtils.class.getSimpleName();

    private DeviceCredentialUtils() {
        // utility class -> private constructor
    }

    public static boolean areCredentialsAvailable(Context context) {
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);

        if (keyguardManager != null) {
            return keyguardManager.isKeyguardSecure();
        } else {
            Log_OC.e(TAG, "Keyguard manager is null");
            return false;
        }
    }
}
