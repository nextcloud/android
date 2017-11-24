package com.owncloud.android.utils;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

public class PowerUtils {

    /**
     * Checks if device is in power save mode. For older devices that do not support this API, returns false.
     * @return true if it is, false otherwise.
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static boolean isPowerSaveMode(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return powerManager != null && powerManager.isPowerSaveMode();
        }

        // For older versions, we just say that device is not in power save mode
        return false;
    }
}
