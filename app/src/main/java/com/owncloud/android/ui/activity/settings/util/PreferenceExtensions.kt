/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.settings.util

import android.preference.Preference
import android.preference.PreferenceActivity
import android.preference.PreferenceCategory
import com.owncloud.android.utils.theme.ViewThemeUtils

inline fun <reified T : Preference> PreferenceActivity.findPreferenceAs(key: String): T? = findPreference(key) as? T

fun PreferenceActivity.findThemedCategory(key: String, viewThemeUtils: ViewThemeUtils): PreferenceCategory? =
    findPreferenceAs<PreferenceCategory>(key)?.also(viewThemeUtils.files::themePreferenceCategory)

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

fun PreferenceCategory.removePreferences(activity: PreferenceActivity, vararg keys: String) {
    keys.forEach { key ->
        activity.findPreference(key)?.let(::removePreference)
    }
}
