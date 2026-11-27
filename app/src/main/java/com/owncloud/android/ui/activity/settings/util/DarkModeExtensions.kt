/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.settings.util

import androidx.annotation.StringRes
import com.nextcloud.client.preferences.DarkMode
import com.owncloud.android.R

@get:StringRes
val DarkMode.summaryResId: Int
    get() = when (this) {
        DarkMode.LIGHT -> R.string.prefs_value_theme_light
        DarkMode.DARK -> R.string.prefs_value_theme_dark
        DarkMode.SYSTEM -> R.string.prefs_value_theme_system
    }
