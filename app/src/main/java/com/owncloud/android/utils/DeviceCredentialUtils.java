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

import com.owncloud.android.lib.common.utils.Log_OC;

import androidx.annotation.RequiresApi;

/**
 * Utility class with methods for handling device credentials.
 */
@RequiresApi(Build.VERSION_CODES.M)
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
