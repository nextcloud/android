/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.preferences

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import com.owncloud.android.R

enum class DarkMode(@StringRes val summaryResId: Int) {
    DARK(R.string.prefs_value_theme_dark),
    LIGHT(R.string.prefs_value_theme_light),
    SYSTEM(R.string.prefs_value_theme_system);

    fun setTheme() {
        AppCompatDelegate.setDefaultNightMode(toMode())
    }

    private fun toMode(): Int = when (this) {
        LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        DARK -> AppCompatDelegate.MODE_NIGHT_YES
        SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
}
