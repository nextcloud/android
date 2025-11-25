/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2021 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2021 Nextcloud GmbH
 * SPDX-FileCopyrightText: 2015 Andy Scherzinger
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.preferences.AppPreferencesImpl
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.dialog.StoragePermissionDialogFragment

object PermissionUtil {
    private const val TAG = "PermissionUtil"

    const val PERMISSIONS_EXTERNAL_STORAGE = 1
    const val PERMISSIONS_READ_CONTACTS_AUTOMATIC = 2
    const val PERMISSIONS_WRITE_CONTACTS = 4
    const val PERMISSIONS_CAMERA = 5
    const val PERMISSIONS_READ_CALENDAR_AUTOMATIC = 6
    const val PERMISSIONS_WRITE_CALENDAR = 7
    const val PERMISSIONS_POST_NOTIFICATIONS = 8
    const val PERMISSION_CHOICE_DIALOG_TAG = "PERMISSION_CHOICE_DIALOG"

    // region Permission Check Helpers
    @JvmStatic
    fun checkSelfPermission(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun checkPermissions(context: Context, permissions: Array<String>): Boolean =
        permissions.all { checkSelfPermission(context, it) }
    // endregion

    /**
     * Request storage permission as needed.
     * Will handle:
     * - Full file access (Android 11+)
     * - Media permissions (Android 13+)
     * - Legacy storage (Android < 11)
     */
    @JvmStatic
    fun requestStoragePermissionIfNeeded(activity: AppCompatActivity) {
        if (checkStoragePermission(activity)) {
            Log_OC.d(TAG, "Storage permissions are already granted")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && canRequestAllFilesPermission(activity)) {
            showStoragePermissionDialogFragment(activity)
            return
        }

        requestRequiredStoragePermissions(activity)
    }

    fun requestRequiredStoragePermissions(activity: Activity) {
        val permissions = getRequiredStoragePermissions()
        if (checkPermissions(activity, permissions)) {
            return
        }

        ActivityCompat.requestPermissions(
            activity,
            permissions,
            PERMISSIONS_EXTERNAL_STORAGE
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun canRequestAllFilesPermission(context: Context) =
        manifestHasAllFilesPermission(context) && hasManageAllFilesActivity(context)

    @RequiresApi(Build.VERSION_CODES.R)
    private fun hasManageAllFilesActivity(context: Context): Boolean {
        val intent = getManageAllFilesIntent(context)

        val launchables: List<ResolveInfo> =
            context.packageManager.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER)
        return launchables.isNotEmpty()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun manifestHasAllFilesPermission(context: Context): Boolean {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        return packageInfo?.requestedPermissions?.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE) ?: false
    }

    /**
     * sdk >= 30: Choice between All Files access or read_external_storage
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun showStoragePermissionDialogFragment(activity: AppCompatActivity) {
        val preferences: AppPreferences = AppPreferencesImpl.fromContext(activity)
        val existingDialog = activity.supportFragmentManager.findFragmentByTag(PERMISSION_CHOICE_DIALOG_TAG)
        if (preferences.isStoragePermissionRequested || existingDialog != null) {
            return
        }

        activity.runOnUiThread {
            val dialogFragment = StoragePermissionDialogFragment()
            dialogFragment.show(activity.supportFragmentManager, PERMISSION_CHOICE_DIALOG_TAG)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun getManageAllFilesIntent(context: Context) = Intent().apply {
        action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
        data = "package:${context.applicationContext.packageName}".toUri()
    }

    /**
     * request camera permission.
     *
     * @param activity The target activity.
     */
    @JvmStatic
    fun requestCameraPermission(activity: Activity, requestCode: Int) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.CAMERA),
            requestCode
        )
    }

    /**
     * Request notification to show notifications. Required on API level >= 33.
     * Does not have any effect on API level < 33.
     *
     * @param activity target activity
     */
    @JvmStatic
    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSIONS_POST_NOTIFICATIONS
                )
            }
        }
    }

    // region Storage permission checks
    /**
     * Checks if the application has storage/media access permissions.
     *
     * - Android 11+ (API 30+): Checks for MANAGE_EXTERNAL_STORAGE (full file system access)
     * - Android 13+ (API 33+): Checks for granular media permissions (READ_MEDIA_IMAGES, READ_MEDIA_VIDEO)
     * - Android 14+ (API 34+): Also checks for limited/partial media access (READ_MEDIA_VISUAL_USER_SELECTED)
     * - Below Android 11: Uses legacy WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE permission
     */
    @JvmStatic
    fun checkStoragePermission(context: Context): Boolean = checkFullFileAccess() || checkMediaAccess(context)

    @JvmStatic
    fun checkFullFileAccess(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()

    fun checkMediaAccess(context: Context): Boolean = checkPermissions(context, getRequiredStoragePermissions())

    private fun getRequiredStoragePermissions() = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> getApiLevel34StoragePermissions()
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getApiLevel33StoragePermissions()
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> getApiLevel29StoragePermissions()
        else -> getLegacyStoragePermissions()
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun getApiLevel34StoragePermissions(): Array<String> = listOf(
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
    ).plus(getApiLevel33StoragePermissions()).toTypedArray()

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun getApiLevel33StoragePermissions(): Array<String> = listOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.ACCESS_MEDIA_LOCATION
    ).toTypedArray()

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getApiLevel29StoragePermissions(): Array<String> = listOf(
        Manifest.permission.ACCESS_MEDIA_LOCATION
    ).plus(getLegacyStoragePermissions()).toTypedArray()

    private fun getLegacyStoragePermissions(): Array<String> = listOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ).toTypedArray()
    // endregion
}
