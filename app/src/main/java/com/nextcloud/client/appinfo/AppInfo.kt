/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.appinfo

import android.content.Context

/**
 * This class provides general, static information about application
 * build.
 *
 * All methods should be thread-safe.
 */
interface AppInfo {
    val versionName: String
    val versionCode: Int
    val isDebugBuild: Boolean
    fun getAppVersion(context: Context): String
}
