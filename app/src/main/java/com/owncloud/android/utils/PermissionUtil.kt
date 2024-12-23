/*
 * Nextcloud - Android Client
 *
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
import com.google.android.material.snackbar.Snackbar
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.dialog.StoragePermissionDialogFragment
import com.owncloud.android.utils.theme.ViewThemeUtils

object PermissionUtil {
    private const val TAG = "PermissionUtil"

    const val PERMISSIONS_EXTERNAL_STORAGE = 1
    const val PERMISSIONS_READ_CONTACTS_AUTOMATIC = 2
    const val PERMISSIONS_WRITE_CONTACTS = 4
    const val PERMISSIONS_CAMERA = 5
    const val PERMISSIONS_READ_CALENDAR_AUTOMATIC = 6
    const val PERMISSIONS_WRITE_CALENDAR = 7
    const val PERMISSIONS_POST_NOTIFICATIONS = 8
    const val PERMISSIONS_MEDIA_LOCATION = 9

    const val REQUEST_CODE_MANAGE_ALL_FILES = 19203

    const val PERMISSION_CHOICE_DIALOG_TAG = "PERMISSION_CHOICE_DIALOG"

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
     * Determine whether the app has been granted storage permissions depending on SDK.
     *
     * For sdk >= 30 we use the storage manager special permission for full access, or READ_EXTERNAL_STORAGE
     * for limited access
     *
     * Under sdk 30 we use WRITE_EXTERNAL_STORAGE
     *
     * @return `true` if app has the permission, or `false` if not.
     */
    @JvmStatic
    fun checkStoragePermission(context: Context): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Environment.isExternalStorageManager() ||
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) || checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            } else {
                checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
            }

        else -> checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    fun checkPermissions(context: Context, permissions: Array<String>): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Request relevant storage permission depending on SDK, if needed.
     *
     * Activities should implement [Activity.onRequestPermissionsResult]
     * and handle the [PERMISSIONS_EXTERNAL_STORAGE] code, as well as [Activity.onActivityResult]
     * with `requestCode=`[REQUEST_CODE_MANAGE_ALL_FILES]
     *
     */
    @JvmStatic
    @JvmOverloads
    fun requestStoragePermissionIfNeeded(
        activity: AppCompatActivity,
        viewThemeUtils: ViewThemeUtils,
        showStrictText: Boolean = false
    ) {
        if (checkStoragePermission(activity)) {
            Log_OC.d(TAG, "Storage permissions are already granted")
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && canRequestAllFilesPermission(activity)) {
            showStoragePermissionDialogFragment(activity, showStrictText)
        } else {
            showStoragePermissionsSnackbarOrRequest(
                activity,
                viewThemeUtils
            )
        }
    }

    fun showStoragePermissionsSnackbarOrRequest(activity: Activity, viewThemeUtils: ViewThemeUtils) {
        val permissions = getStoragePermissions()

        if (permissions.any { shouldShowRequestPermissionRationale(activity, it) }) {
            showStoragePermissionsSnackbar(activity, permissions, viewThemeUtils)
        } else {
            requestPermissions(activity, permissions)
        }
    }

    private fun getStoragePermissions() = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        else -> arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    private fun requestPermissions(activity: Activity, permissions: Array<String>) {
        ActivityCompat.requestPermissions(
            activity,
            permissions,
            PERMISSIONS_EXTERNAL_STORAGE
        )
    }

    private fun showStoragePermissionsSnackbar(
        activity: Activity,
        permissions: Array<String>,
        viewThemeUtils: ViewThemeUtils
    ) {
        Snackbar.make(
            activity.findViewById(android.R.id.content),
            R.string.permission_storage_access,
            Snackbar.LENGTH_INDEFINITE
        ).setAction(R.string.common_ok) {
            requestPermissions(activity, permissions)
        }.also { viewThemeUtils.material.themeSnackbar(it) }.show()
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
    private fun manifestHasAllFilesPermission(context: Context): Boolean {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        return packageInfo?.requestedPermissions?.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE) ?: false
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun showStoragePermissionDialogFragment(activity: AppCompatActivity, showStrictText: Boolean) {
        activity.runOnUiThread {
            val existingDialog = activity.supportFragmentManager.findFragmentByTag(PERMISSION_CHOICE_DIALOG_TAG)

            if (existingDialog == null) {
                StoragePermissionDialogFragment.newInstance(showStrictText).run {
                    show(activity.supportFragmentManager, PERMISSION_CHOICE_DIALOG_TAG)
                }
            }
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

    /**
     * Request media location permission. Required on API level >= 34.
     * Does not have any effect on API level < 34.
     *
     * @param activity target activity
     */
    @Suppress("ReturnCount")
    @JvmStatic
    fun requestMediaLocationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
            return
        }

        if (checkSelfPermission(activity, Manifest.permission.ACCESS_MEDIA_LOCATION)) {
            return
        }

        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_MEDIA_LOCATION),
            PERMISSIONS_MEDIA_LOCATION
        )
    }
}
