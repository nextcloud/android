/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.preferences

import androidx.appcompat.app.AppCompatDelegate

enum class DarkMode {
    DARK, LIGHT, SYSTEM;

    fun setTheme() {
        AppCompatDelegate.setDefaultNightMode(this.toMode())
    }

    private fun toMode(): Int {
        return when (this) {
            LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            DARK -> AppCompatDelegate.MODE_NIGHT_YES
            SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }
}
