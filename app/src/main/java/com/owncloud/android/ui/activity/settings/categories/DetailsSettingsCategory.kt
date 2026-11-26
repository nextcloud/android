/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.settings.categories

import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import com.owncloud.android.ui.activity.SettingsActivity.Companion.PREFERENCE_SHOW_MEDIA_SCAN_NOTIFICATIONS
import com.owncloud.android.ui.activity.settings.AppLockSettings
import com.owncloud.android.ui.activity.settings.util.bindOrRemove
import com.owncloud.android.ui.activity.settings.util.findThemedCategory
import com.owncloud.android.utils.theme.ViewThemeUtils

class DetailsSettingsCategory(
    private val fragment: PreferenceFragmentCompat,
    private val preferences: AppPreferences,
    private val viewThemeUtils: ViewThemeUtils,
    private val appLockSettings: AppLockSettings
) : SettingsCategory {

    override fun setup() {
        val category = fragment.findThemedCategory(CATEGORY_KEY, viewThemeUtils) ?: return
        val resources = fragment.resources
        val isLockAvailable = resources.getBoolean(R.bool.passcode_enabled) ||
            resources.getBoolean(R.bool.device_credentials_enabled)

        appLockSettings.setup(category, isLockAvailable)

        category.bindOrRemove(
            fragment.findPreference<TwoStatePreference>(SHOW_ECOSYSTEM_APPS_KEY),
            !resources.getBoolean(R.bool.is_branded_client)
        ) { preferences.setShowEcosystemApps(it.isChecked) }

        if (preferences.isShowMediaScanNotifications) {
            category.removePreferenceRecursively(PREFERENCE_SHOW_MEDIA_SCAN_NOTIFICATIONS)
        }

        if (category.preferenceCount == 0) {
            fragment.preferenceScreen.removePreference(category)
        }
    }

    companion object {
        private const val CATEGORY_KEY = "details"
        private const val SHOW_ECOSYSTEM_APPS_KEY = "show_ecosystem_apps"
    }
}
