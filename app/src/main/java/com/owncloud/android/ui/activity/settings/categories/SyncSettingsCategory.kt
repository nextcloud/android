/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.settings.categories

import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.nextcloud.utils.extensions.createAllFilesAccessSettingsIntent
import com.owncloud.android.R
import com.owncloud.android.ui.activity.settings.util.bindOrRemove
import com.owncloud.android.ui.activity.settings.util.findThemedCategory
import com.owncloud.android.ui.activity.settings.util.registerForResult
import com.owncloud.android.ui.activity.settings.util.setOnClickAction
import com.owncloud.android.ui.activity.settings.util.startNavigatorScreen
import com.owncloud.android.ui.navigation.NavigatorScreen
import com.owncloud.android.utils.PermissionUtil
import com.owncloud.android.utils.theme.ViewThemeUtils

class SyncSettingsCategory(private val fragment: PreferenceFragmentCompat, private val viewThemeUtils: ViewThemeUtils) :
    SettingsCategory {

    private val activity = fragment.requireActivity()
    private val allFilesAccessLauncher = fragment.registerForResult { refreshAllFilesAccessPreference() }

    override fun setup() {
        val category = fragment.findThemedCategory(CATEGORY_KEY, viewThemeUtils) ?: return

        category.bindOrRemove(
            fragment.findPreference<Preference>(AUTO_UPLOAD_KEY),
            !activity.resources.getBoolean(R.bool.syncedFolder_light)
        ) { activity.startNavigatorScreen(NavigatorScreen.AutoUpload) }

        fragment.findPreference<Preference>(TWO_WAY_SYNC_KEY)?.setOnClickAction {
            activity.startNavigatorScreen(NavigatorScreen.InternalTwoWaySync)
        }

        setupAllFilesAccessPreference(category)
    }

    private fun refreshAllFilesAccessPreference() {
        val category = fragment.findPreference<PreferenceCategory>(CATEGORY_KEY) ?: return

        setupAllFilesAccessPreference(category)
    }

    private fun setupAllFilesAccessPreference(category: PreferenceCategory) {
        val allFilesAccess = fragment.findPreference<Preference>(ALL_FILES_ACCESS_KEY) ?: return

        if (PermissionUtil.checkAllFilesAccess()) {
            category.removePreference(allFilesAccess)
        } else if (allFilesAccess.parent == null) {
            category.addPreference(allFilesAccess)
        }

        allFilesAccess.setOnClickAction {
            activity.createAllFilesAccessSettingsIntent()?.let(allFilesAccessLauncher::launch)
        }
    }

    companion object {
        private const val CATEGORY_KEY = "sync"
        private const val AUTO_UPLOAD_KEY = "syncedFolders"
        private const val TWO_WAY_SYNC_KEY = "internal_two_way_sync"
        private const val ALL_FILES_ACCESS_KEY = "allFilesAccess"
    }
}
