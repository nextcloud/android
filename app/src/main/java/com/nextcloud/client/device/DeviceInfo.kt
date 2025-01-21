/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.device

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.util.Locale

class DeviceInfo {
    val vendor: String = Build.MANUFACTURER.lowercase(Locale.ROOT)
    val apiLevel: Int = Build.VERSION.SDK_INT
    val androidVersion = Build.VERSION.RELEASE

    fun hasCamera(context: Context): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
}
