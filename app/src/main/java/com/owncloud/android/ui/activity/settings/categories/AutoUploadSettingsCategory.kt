/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.settings.categories

import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import com.nextcloud.client.account.User
import com.owncloud.android.R
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.ui.activity.SettingsActivity.Companion.SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI
import com.owncloud.android.ui.activity.settings.util.findThemedCategory
import com.owncloud.android.ui.activity.settings.util.setOnClickAction
import com.owncloud.android.ui.activity.settings.util.startNavigatorScreen
import com.owncloud.android.ui.navigation.NavigatorScreen
import com.owncloud.android.utils.theme.ViewThemeUtils

class AutoUploadSettingsCategory(
    private val fragment: PreferenceFragmentCompat,
    private val user: User,
    private val arbitraryDataProvider: ArbitraryDataProvider,
    private val viewThemeUtils: ViewThemeUtils
) : SettingsCategory {

    private val activity = fragment.requireActivity()

    override fun setup() {
        val category = fragment.findThemedCategory(CATEGORY_KEY, viewThemeUtils) ?: return

        if (!activity.resources.getBoolean(R.bool.syncedFolder_light)) {
            fragment.preferenceScreen.removePreference(category)
            return
        }

        setupUploadOnWifiPreference()

        fragment.findPreference<Preference>(CONFIGURE_FOLDERS_KEY)?.setOnClickAction {
            activity.startNavigatorScreen(NavigatorScreen.AutoUpload)
        }
    }

    private fun setupUploadOnWifiPreference() {
        val uploadOnWifi = fragment.findPreference<TwoStatePreference>(UPLOAD_ON_WIFI_KEY) ?: return
        uploadOnWifi.isChecked = arbitraryDataProvider.getBooleanValue(user, SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI)

        uploadOnWifi.setOnClickAction {
            arbitraryDataProvider.storeOrUpdateKeyValue(
                user.accountName,
                SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI,
                uploadOnWifi.isChecked.toString()
            )
        }
    }

    companion object {
        private const val CATEGORY_KEY = "synced_folders_category"
        private const val UPLOAD_ON_WIFI_KEY = "synced_folder_on_wifi"
        private const val CONFIGURE_FOLDERS_KEY = "synced_folders_configure_folders"
    }
}
