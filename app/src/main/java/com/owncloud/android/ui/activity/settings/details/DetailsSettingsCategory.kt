/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.settings.details

import android.preference.PreferenceActivity
import android.preference.PreferenceScreen
import android.preference.SwitchPreference
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import com.owncloud.android.ui.activity.SettingsActivity.Companion.PREFERENCE_SHOW_MEDIA_SCAN_NOTIFICATIONS
import com.owncloud.android.ui.activity.settings.lock.AppLockSettings
import com.owncloud.android.ui.activity.settings.util.bindOrRemove
import com.owncloud.android.ui.activity.settings.util.findPreferenceAs
import com.owncloud.android.ui.activity.settings.util.findThemedCategory
import com.owncloud.android.ui.activity.settings.util.removePreferences
import com.owncloud.android.utils.theme.ViewThemeUtils

class DetailsSettingsCategory(
    private val activity: PreferenceActivity,
    private val preferences: AppPreferences,
    private val viewThemeUtils: ViewThemeUtils,
    private val appLockSettings: AppLockSettings
) {

    fun setup(preferenceScreen: PreferenceScreen) {
        val category = activity.findThemedCategory(CATEGORY_KEY, viewThemeUtils) ?: return
        val resources = activity.resources
        val passCodeEnabled = resources.getBoolean(R.bool.passcode_enabled)
        val deviceCredentialsEnabled = resources.getBoolean(R.bool.device_credentials_enabled)
        val showEcosystemAppsEnabled = !resources.getBoolean(R.bool.is_branded_client)
        val syncedFolderLightEnabled = resources.getBoolean(R.bool.syncedFolder_light)
        val showMediaScanNotifications = preferences.isShowMediaScanNotifications

        appLockSettings.setup(category, passCodeEnabled || deviceCredentialsEnabled)

        category.bindOrRemove(
            activity.findPreferenceAs<SwitchPreference>(SHOW_ECOSYSTEM_APPS_KEY),
            showEcosystemAppsEnabled
        ) { preferences.setShowEcosystemApps(it.isChecked) }

        if (showMediaScanNotifications) {
            category.removePreferences(activity, PREFERENCE_SHOW_MEDIA_SCAN_NOTIFICATIONS)
        }

        val shouldHideCategory = !passCodeEnabled &&
            !deviceCredentialsEnabled &&
            syncedFolderLightEnabled &&
            showMediaScanNotifications
        if (shouldHideCategory) {
            preferenceScreen.removePreference(category)
        }
    }

    companion object {
        private const val CATEGORY_KEY = "details"
        private const val SHOW_ECOSYSTEM_APPS_KEY = "show_ecosystem_apps"
    }
}
