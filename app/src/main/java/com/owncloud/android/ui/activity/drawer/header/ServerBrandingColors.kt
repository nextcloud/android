/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.drawer.header

import android.accounts.Account
import android.graphics.Color
import com.nextcloud.android.common.ui.util.PlatformThemeUtil
import com.owncloud.android.R
import com.owncloud.android.ui.activity.drawer.DrawerActivity
import com.owncloud.android.utils.theme.CapabilityUtils

class ServerBrandingColors(private val activity: DrawerActivity) {

    val textColor: Int
        get() {
            val serverTextColor = CapabilityUtils.getCapability(activity).serverTextColor
            val fallbackColor = if (PlatformThemeUtil.isDarkMode(activity)) Color.WHITE else Color.BLACK
            return activity.colorUtil.getNullSafeColor(serverTextColor, fallbackColor)
        }

    fun getPrimaryColor(account: Account): Int {
        val serverColor = CapabilityUtils.getCapability(account, activity).serverColor
        return activity.colorUtil.getNullSafeColor(serverColor, activity.getColor(R.color.primary))
    }
}
