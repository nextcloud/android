/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.appinfo

import android.content.Context
import android.content.pm.PackageManager
import com.owncloud.android.BuildConfig
import com.owncloud.android.lib.common.utils.Log_OC

class AppInfoImpl : AppInfo {
    override val versionName: String = BuildConfig.VERSION_NAME
    override val versionCode: Int = BuildConfig.VERSION_CODE
    override val isDebugBuild: Boolean = BuildConfig.DEBUG

    override fun getAppVersion(context: Context): String = try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "n/a"
    } catch (e: PackageManager.NameNotFoundException) {
        Log_OC.e(this, "Trying to get packageName", e.cause)
        "n/a"
    }
}
