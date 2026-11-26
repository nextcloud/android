/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.settings.categories

import androidx.core.content.edit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.nextcloud.client.account.User
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.preferences.AppPreferencesImpl
import com.nextcloud.client.preferences.DarkMode
import com.nextcloud.ui.dialog.chooseStorage.ChooseStorageLocationDialogFragment
import com.owncloud.android.MainApp
import com.owncloud.android.ui.activity.StorageMigration
import com.owncloud.android.ui.activity.StorageMigration.StorageMigrationProgressListener
import com.owncloud.android.ui.activity.settings.util.findThemedCategory
import com.owncloud.android.ui.activity.settings.util.setOnClickAction
import com.owncloud.android.ui.activity.settings.util.showIfAbsent
import com.owncloud.android.ui.dialog.ThemeSelectionDialog
import com.owncloud.android.utils.theme.ViewThemeUtils

class GeneralSettingsCategory(
    private val fragment: PreferenceFragmentCompat,
    private val user: User,
    private val preferences: AppPreferences,
    private val viewThemeUtils: ViewThemeUtils
) : SettingsCategory,
    StorageMigrationProgressListener {

    private val activity = fragment.requireActivity()
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity.applicationContext)
    private var storagePath = readStoragePath()

    init {
        val fragmentManager = fragment.childFragmentManager

        fragmentManager.setFragmentResultListener(
            ChooseStorageLocationDialogFragment.REQUEST_KEY,
            fragment
        ) { _, result ->
            result.getString(ChooseStorageLocationDialogFragment.RESULT_STORAGE_PATH)?.let(::onStorageLocationSelected)
        }

        fragmentManager.setFragmentResultListener(ThemeSelectionDialog.REQUEST_KEY, fragment) { _, result ->
            result.getString(ThemeSelectionDialog.RESULT_DARK_MODE)?.let(::onThemeSelected)
        }
    }

    override fun setup() {
        fragment.findThemedCategory(CATEGORY_KEY, viewThemeUtils)

        fragment.findPreference<Preference>(AppPreferencesImpl.DATA_STORAGE_LOCATION)?.setOnClickAction {
            ChooseStorageLocationDialogFragment().showIfAbsent(
                fragment.childFragmentManager,
                ChooseStorageLocationDialogFragment.TAG
            )
        }

        setupThemePreference()
    }

    override fun onStorageMigrationFinished(storagePath: String?, succeed: Boolean) {
        if (!succeed || storagePath == null) return

        saveStoragePath(storagePath)
    }

    override fun onCancelMigration() = Unit

    private fun setupThemePreference() {
        val themePreference = fragment.findPreference<Preference>(DARK_MODE_KEY) ?: return
        themePreference.setSummary(preferences.darkThemeMode.summaryResId)

        themePreference.setOnClickAction {
            ThemeSelectionDialog().showIfAbsent(fragment.childFragmentManager, ThemeSelectionDialog.TAG)
        }
    }

    private fun onStorageLocationSelected(newPath: String) {
        if (storagePath == newPath) return

        StorageMigration(activity, user, storagePath, newPath, viewThemeUtils).run {
            setStorageMigrationProgressListener(this@GeneralSettingsCategory)
            migrate()
        }
    }

    private fun onThemeSelected(themeValue: String) {
        val mode = DarkMode.entries.firstOrNull { it.name == themeValue } ?: DarkMode.SYSTEM

        fragment.findPreference<Preference>(DARK_MODE_KEY)?.setSummary(mode.summaryResId)
    }

    private fun readStoragePath(): String {
        val defaultPath = activity.applicationContext.filesDir.absolutePath

        return sharedPreferences.getString(AppPreferencesImpl.STORAGE_PATH, defaultPath) ?: defaultPath
    }

    private fun saveStoragePath(newStoragePath: String) {
        storagePath = newStoragePath
        MainApp.setStoragePath(newStoragePath)

        sharedPreferences.edit { putString(AppPreferencesImpl.STORAGE_PATH, newStoragePath) }
    }

    companion object {
        private const val CATEGORY_KEY = "general"
        private const val DARK_MODE_KEY = "darkMode"
    }
}
