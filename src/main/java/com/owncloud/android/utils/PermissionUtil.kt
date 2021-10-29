package com.owncloud.android.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Created by scherzia on 29.12.2015.
 */
object PermissionUtil {
    const val PERMISSIONS_EXTERNAL_STORAGE = 1
    const val PERMISSIONS_READ_CONTACTS_AUTOMATIC = 2
    const val PERMISSIONS_WRITE_CONTACTS = 4
    const val PERMISSIONS_CAMERA = 5
    const val PERMISSIONS_READ_CALENDAR_AUTOMATIC = 6
    const val PERMISSIONS_WRITE_CALENDAR = 7

    /**
     * Wrapper method for ContextCompat.checkSelfPermission().
     * Determine whether *the app* has been granted a particular permission.
     *
     * @param permission The name of the permission being checked.
     * @return `true` if app has the permission, or `false` if not.
     */
    @JvmStatic
    fun checkSelfPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

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
    @JvmStatic
    fun shouldShowRequestPermissionRationale(activity: Activity, permission: String): Boolean =
        ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

    /**
     * For SDK < 30, we can do whatever we want using WRITE_EXTERNAL_STORAGE.
     * For SDK above 30, scoped storage is in effect, and WRITE_EXTERNAL_STORAGE is useless. However, we do still need
     * READ_EXTERNAL_STORAGE to read and upload files from folders that we don't manage and are not public access.
     *
     * @return The relevant external storage permission, depending on SDK
     */
    @JvmStatic
    fun getExternalStoragePermission(): String = when {
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R -> Manifest.permission.WRITE_EXTERNAL_STORAGE
        else -> Manifest.permission.READ_EXTERNAL_STORAGE
    }

    /**
     * Determine whether *the app* has been granted external storage permissions depending on SDK.
     *
     * @return `true` if app has the permission, or `false` if not.
     */
    @JvmStatic
    fun checkExternalStoragePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, getExternalStoragePermission()) == PackageManager.PERMISSION_GRANTED

    /**
     * Request relevant external storage permission depending on SDK.
     *
     * @param activity The target activity.
     */
    @JvmStatic
    fun requestExternalStoragePermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity, arrayOf(getExternalStoragePermission()),
            PERMISSIONS_EXTERNAL_STORAGE
        )
    }

    /**
     * request camera permission.
     *
     * @param activity The target activity.
     */
    @JvmStatic
    fun requestCameraPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity, arrayOf(Manifest.permission.CAMERA),
            PERMISSIONS_CAMERA
        )
    }
}
