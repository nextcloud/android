/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.settings.util

import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.owncloud.android.utils.theme.ViewThemeUtils

fun PreferenceFragmentCompat.findThemedCategory(key: String, viewThemeUtils: ViewThemeUtils): PreferenceCategory? =
    findPreference<PreferenceCategory>(key)?.also(viewThemeUtils.files::themePreferenceCategory)

fun Preference.setOnClickAction(action: () -> Unit) {
    onPreferenceClickListener = Preference.OnPreferenceClickListener {
        action()
        true
    }
}

fun <T : Preference> PreferenceCategory.bindOrRemove(preference: T?, isEnabled: Boolean, onClick: (T) -> Unit): T? {
    if (preference == null) return null

    if (!isEnabled) {
        removePreference(preference)
        return null
    }

    preference.setOnClickAction { onClick(preference) }

    return preference
}
