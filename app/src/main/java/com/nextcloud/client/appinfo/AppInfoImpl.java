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
package com.nextcloud.client.appinfo;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.owncloud.android.BuildConfig;
import com.owncloud.android.lib.common.utils.Log_OC;

class AppInfoImpl implements AppInfo {

    @Override
    public String getFormattedVersionCode() {
        return Integer.toString(BuildConfig.VERSION_CODE);
    }

    @Override
    public int getVersionCode() {
        return BuildConfig.VERSION_CODE;
    }

    @Override
    public boolean isDebugBuild() {
        return BuildConfig.DEBUG;
    }

    @Override
    public String getAppVersion(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            if (pInfo != null) {
                return pInfo.versionName;
            } else {
                return "n/a";
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log_OC.e(this, "Trying to get packageName", e.getCause());

            return "n/a";
        }
    }
}
