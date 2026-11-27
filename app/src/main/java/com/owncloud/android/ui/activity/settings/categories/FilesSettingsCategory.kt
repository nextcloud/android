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
import com.owncloud.android.ui.activity.settings.util.bindOrRemove
import com.owncloud.android.ui.activity.settings.util.findThemedCategory
import com.owncloud.android.ui.activity.settings.util.setOnClickAction
import com.owncloud.android.utils.theme.ViewThemeUtils

class FilesSettingsCategory(
    private val fragment: PreferenceFragmentCompat,
    private val preferences: AppPreferences,
    private val viewThemeUtils: ViewThemeUtils
) : SettingsCategory {

    override fun setup() {
        val category = fragment.findThemedCategory(CATEGORY_KEY, viewThemeUtils) ?: return
        val showHiddenFilesEnabled = fragment.resources.getBoolean(R.bool.show_hidden_files_enabled)

        category.bindOrRemove(
            fragment.findPreference<TwoStatePreference>(SHOW_HIDDEN_FILES_KEY),
            showHiddenFilesEnabled
        ) { preferences.setShowHiddenFilesEnabled(it.isChecked) }

        bindSwitch(SORT_FOLDERS_BEFORE_FILES_KEY, preferences::setSortFoldersBeforeFiles)
        bindSwitch(SORT_FAVORITES_FIRST_KEY, preferences::setSortFavoritesFirst)
    }

    private fun bindSwitch(key: String, onCheckedChange: (Boolean) -> Unit) {
        val switchPreference = fragment.findPreference<TwoStatePreference>(key) ?: return

        switchPreference.setOnClickAction { onCheckedChange(switchPreference.isChecked) }
    }

    companion object {
        private const val CATEGORY_KEY = "files"
        private const val SHOW_HIDDEN_FILES_KEY = "show_hidden_files"
        private const val SORT_FOLDERS_BEFORE_FILES_KEY = "sort_folders_before_files"
        private const val SORT_FAVORITES_FIRST_KEY = "sort_favorites_first"
    }
}
