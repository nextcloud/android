/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
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

    override fun getAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (pInfo != null) {
                pInfo.versionName
            } else {
                "n/a"
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log_OC.e(this, "Trying to get packageName", e.cause)
            "n/a"
        }
    }
}
