/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.view.Window
import android.view.WindowManager

fun Window?.makeStatusBarTransparent() {
    val flag = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
    this?.setFlags(flag, flag)
}
