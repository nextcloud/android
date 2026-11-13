/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.fileactivity

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.client.utils.IntentUtil
import com.nextcloud.utils.SnackbarUtil
import com.owncloud.android.R
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.asynctasks.LoadingVersionNumberTask
import com.owncloud.android.ui.asynctasks.LoadingVersionNumberTask.VersionDevInterface
import com.owncloud.android.utils.FilesSyncHelper

object DevVersionChecker {
    private const val TAG = "DevVersionChecker"
    private const val UNKNOWN_VALUE = -1
    private const val MIN_APP_OPENED_COUNT_FOR_CHECK = 10
    private const val DEV_APK_EXTENSION = ".apk"

    fun isCheckNecessary(resources: Resources, arbitraryDataProvider: ArbitraryDataProvider): Boolean {
        if (!resources.getBoolean(R.bool.dev_version_direct_download_enabled)) return false

        val appOpenedCount =
            arbitraryDataProvider.getIntegerValue(FilesSyncHelper.GLOBAL, FileActivity.APP_OPENED_COUNT)
        return appOpenedCount > MIN_APP_OPENED_COUNT_FOR_CHECK || appOpenedCount == UNKNOWN_VALUE
    }

    @JvmStatic
    fun checkForNewDevVersion(callback: VersionDevInterface?, context: Context) {
        LoadingVersionNumberTask(callback).execute(context.getString(R.string.dev_latest))
    }

    @JvmStatic
    fun showDevSnackbar(activity: Activity, latestVersion: Int, openDirectly: Boolean, inBackground: Boolean) {
        val currentVersion = activity.installedVersionCode()

        if (latestVersion == UNKNOWN_VALUE || currentVersion == UNKNOWN_VALUE) {
            SnackbarUtil.show(activity, R.string.dev_version_no_information_available)
        }

        if (latestVersion <= currentVersion) {
            if (!inBackground) {
                SnackbarUtil.show(activity, R.string.dev_version_no_new_version_available)
            }
            return
        }

        val devApkLink = activity.getString(R.string.dev_link) + latestVersion + DEV_APK_EXTENSION
        if (openDirectly) {
            IntentUtil.startLinkIntent(activity, devApkLink)
            return
        }

        Snackbar
            .make(
                activity.findViewById(android.R.id.content),
                R.string.dev_version_new_version_available,
                Snackbar.LENGTH_LONG
            )
            .setAction(activity.getString(R.string.version_dev_download)) {
                IntentUtil.startLinkIntent(activity, devApkLink)
            }
            .show()
    }

    @Suppress("DEPRECATION")
    private fun Activity.installedVersionCode(): Int = try {
        packageManager.getPackageInfo(packageName, 0).versionCode
    } catch (e: PackageManager.NameNotFoundException) {
        Log_OC.e(TAG, "Package not found", e)
        UNKNOWN_VALUE
    }
}
