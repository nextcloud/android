/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.owncloud.android.utils.theme

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.Button
import androidx.annotation.ColorInt
import com.nextcloud.android.common.ui.theme.utils.AndroidViewThemeUtils
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.owncloud.android.R

class IonosAndroidViewThemeUtils(
    private val platformUtil: AndroidViewThemeUtils,
) {
    fun themeSystemBars(activity: Activity) {
        platformUtil.colorStatusBar(activity, activity.getSystemBarsColor())
    }

    fun themeSystemBars(activity: Activity, @ColorInt color: Int) {
        platformUtil.colorStatusBar(activity, color)
    }

    fun resetSystemBars(activity: Activity) {
        platformUtil.colorStatusBar(activity, activity.getSystemBarsColor())
    }

    @JvmOverloads
    fun colorViewBackground(view: View, colorRole: ColorRole = ColorRole.SURFACE) {}

    fun themeDialog(view: View) {}

    fun colorTextButtons(vararg buttons: Button) {}

    @ColorInt
    private fun Context.getSystemBarsColor(): Int = getColor(R.color.system_bars_color)
}
