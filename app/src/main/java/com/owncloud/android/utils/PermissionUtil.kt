/*
 * Nextcloud Android client application
 *
 * @author Álvaro Brey Vilas
 * @author Andy Scherzinger
 * Copyright (C) 2021 Álvaro Brey Vilas
 * Copyright (C) 2021 Nextcloud GmbH
 * Copyright (C) 2015 Andy Scherzinger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.preferences.AppPreferencesImpl
import com.owncloud.android.R
import com.owncloud.android.ui.dialog.StoragePermissionDialogFragment
import com.owncloud.android.utils.theme.ThemeSnackbarUtils

object PermissionUtil {
    const val PERMISSIONS_EXTERNAL_STORAGE = 1
    const val PERMISSIONS_READ_CONTACTS_AUTOMATIC = 2
    const val PERMISSIONS_WRITE_CONTACTS = 4
    const val PERMISSIONS_CAMERA = 5
    const val PERMISSIONS_READ_CALENDAR_AUTOMATIC = 6
    const val PERMISSIONS_WRITE_CALENDAR = 7
    const val PERMISSIONS_SCAN_DOCUMENT = 6

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
     * Determine whether the app has been granted external storage permissions depending on SDK.
     *
     * For sdk >= 30 we use the storage manager special permission for full access, or READ_EXTERNAL_STORAGE
     * for limited access
     *
     * Under sdk 30 we use WRITE_EXTERNAL_STORAGE
     *
     * @return `true` if app has the permission, or `false` if not.
     */
    @JvmStatic
    fun checkExternalStoragePermission(context: Context): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Environment.isExternalStorageManager() || checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        else -> checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    /**
     * Request relevant external storage permission depending on SDK, if needed.
     *
     * Activities should implement [Activity.onRequestPermissionsResult]
     * and handle the [PERMISSIONS_EXTERNAL_STORAGE] code, as well as [Activity.onActivityResult]
     * with `requestCode=`[REQUEST_CODE_MANAGE_ALL_FILES]
     *
     * @param activity The target activity.
     * @param permissionRequired for SDK >=30 specifically, show again even if already denied in the past
     */
    @JvmStatic
    @JvmOverloads
    fun requestExternalStoragePermission(activity: AppCompatActivity, permissionRequired: Boolean = false) {
        if (!checkExternalStoragePermission(activity)) {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    if (canRequestAllFilesPermission(activity)) {
                        // can request All Files, show choice
                        showPermissionChoiceDialog(activity, permissionRequired)
                    } else {
                        // can not request all files, request READ_EXTERNAL_STORAGE
                        requestStoragePermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
                else -> requestStoragePermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }
    /**
     * Request a storage permission
     */
    private fun requestStoragePermission(activity: Activity, permission: String) {
        fun doRequest() {
            ActivityCompat.requestPermissions(
                activity, arrayOf(permission),
                PERMISSIONS_EXTERNAL_STORAGE
            )
        }

        // Check if we should show an explanation
        if (shouldShowRequestPermissionRationale(activity, permission)) {
            // Show explanation to the user and then request permission
            Snackbar
                .make(
                    activity.findViewById(android.R.id.content),
                    R.string.permission_storage_access,
                    Snackbar.LENGTH_INDEFINITE
                )
                .setAction(R.string.common_ok) {
                    doRequest()
                }
                .also {
                    ThemeSnackbarUtils.colorSnackbar(activity, it)
                }
                .show()
        } else {
            // No explanation needed, request the permission.
            doRequest()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun canRequestAllFilesPermission(context: Context) =
        manifestHasAllFilesPermission(context) && hasManageAllFilesActivity(context)

    @RequiresApi(Build.VERSION_CODES.R)
    private fun hasManageAllFilesActivity(context: Context): Boolean {
        val intent = getManageAllFilesIntent(context)

        val launchables: List<ResolveInfo> = context.packageManager
            .queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER)
        return launchables.isNotEmpty()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun manifestHasAllFilesPermission(context: Context): Boolean {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        return packageInfo?.requestedPermissions?.contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE) ?: false
    }

    /**
     * sdk >= 30: Choice between All Files access or read_external_storage
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun showPermissionChoiceDialog(activity: AppCompatActivity, permissionRequired: Boolean) {
        val preferences: AppPreferences = AppPreferencesImpl.fromContext(activity)

        if (!preferences.isStoragePermissionRequested || permissionRequired) {
            if (activity.supportFragmentManager.findFragmentByTag(PERMISSION_CHOICE_DIALOG_TAG) == null) {
                val listener = object : StoragePermissionDialogFragment.Listener {
                    override fun onCancel() {
                        preferences.isStoragePermissionRequested = true
                    }

                    override fun onClickFullAccess() {
                        preferences.isStoragePermissionRequested = true
                        val intent = getManageAllFilesIntent(activity)
                        activity.startActivityForResult(intent, REQUEST_CODE_MANAGE_ALL_FILES)
                        preferences.isStoragePermissionRequested = true
                    }

                    override fun onClickMediaReadOnly() {
                        preferences.isStoragePermissionRequested = true
                        requestStoragePermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                    }
                }
                val dialogFragment = StoragePermissionDialogFragment(listener, permissionRequired)
                dialogFragment.show(activity.supportFragmentManager, PERMISSION_CHOICE_DIALOG_TAG)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun getManageAllFilesIntent(context: Context) =
        Intent().apply {
            action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
            data = Uri.parse("package:${context.applicationContext.packageName}")
        }

    /**
     * request camera permission.
     *
     * @param activity The target activity.
     */
    @JvmStatic
    fun requestCameraPermission(activity: Activity, requestCode: Int) {
        ActivityCompat.requestPermissions(
            activity, arrayOf(Manifest.permission.CAMERA),
            requestCode
        )
    }
}
