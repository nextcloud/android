package com.owncloud.android.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/**
 * Created by scherzia on 29.12.2015.
 */
public final class PermissionUtil {
    public static final int PERMISSIONS_WRITE_EXTERNAL_STORAGE = 1;
    public static final int PERMISSIONS_READ_CONTACTS_AUTOMATIC = 2;
    public static final int PERMISSIONS_READ_CONTACTS_MANUALLY = 3;
    public static final int PERMISSIONS_WRITE_CONTACTS = 4;
    public static final int PERMISSIONS_READ_EXTERNAL_STORAGE = 5;

    private PermissionUtil() {
        // utility class -> private constructor
    }

    /**
     * Wrapper method for ContextCompat.checkSelfPermission().
     * Determine whether <em>the app</em> has been granted a particular permission.
     *
     * @param permission The name of the permission being checked.
     * @return <code>true</code> if app has the permission, or <code>false</code> if not.
     */
    public static boolean checkSelfPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Wrapper method for ActivityCompat.shouldShowRequestPermissionRationale().
     * Gets whether you should show UI with rationale for requesting a permission.
     * You should do this only if you do not have the permission and the context in
     * which the permission is requested does not clearly communicate to the user
     * what would be the benefit from granting this permission.
     *
     * @param activity   The target activity.
     * @param permission A permission to be requested.
     * @return Whether to show permission rationale UI.
     */
    public static boolean shouldShowRequestPermissionRationale(Activity activity, String permission) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
    }

    /**
     * request the write permission for external storage.
     *
     * @param activity The target activity.
     */
    public static void requestWriteExternalStoreagePermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                PERMISSIONS_WRITE_EXTERNAL_STORAGE);
    }

    /**
     * request the read permission for external storage.
     *
     * @param activity The target activity.
     */
    public static void requesReadExternalStoreagePermission(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                PERMISSIONS_READ_EXTERNAL_STORAGE);
    }

    /**
     * request the ignore battery optimizations.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void requestIgnoreBatteryOptimizationsPermission(Context context) {
        Intent intent = new Intent();
        String packageName = context.getPackageName();
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm != null && pm.isIgnoringBatteryOptimizations(packageName)) {
            intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        } else {
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
        }
        context.startActivity(intent);
    }

}
