package com.owncloud.android.utils;

import com.owncloud.android.lib.common.utils.Log_OC;

import java.io.Closeable;
import java.io.IOException;

/**
 * Static system IO helper methods
 */

public class IOHelper {
    private static final String TAG = IOHelper.class.getSimpleName();

    public static void close(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (IOException e) {
            Log_OC.e(TAG, "Error closing stream", e);
        }
    }

}
