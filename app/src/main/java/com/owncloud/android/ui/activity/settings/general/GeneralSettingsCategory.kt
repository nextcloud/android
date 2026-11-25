/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.settings.general

import android.preference.PreferenceActivity
import android.preference.PreferenceManager
import androidx.core.content.edit
import com.nextcloud.client.account.User
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.preferences.AppPreferencesImpl
import com.nextcloud.client.preferences.DarkMode
import com.owncloud.android.MainApp
import com.owncloud.android.ui.activity.StorageMigration
import com.owncloud.android.ui.activity.StorageMigration.StorageMigrationProgressListener
import com.owncloud.android.ui.activity.settings.util.findThemedCategory
import com.owncloud.android.ui.activity.settings.util.setOnClickAction
import com.owncloud.android.ui.activity.settings.util.startExtendedSettingsDialog
import com.owncloud.android.ui.activity.settings.util.summaryResId
import com.owncloud.android.ui.model.ExtendedSettingsActivityDialog
import com.owncloud.android.utils.theme.ViewThemeUtils

class GeneralSettingsCategory(
    private val activity: PreferenceActivity,
    private val user: User,
    private val preferences: AppPreferences,
    private val viewThemeUtils: ViewThemeUtils
) : StorageMigrationProgressListener {

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity.applicationContext)
    private var storagePath = readStoragePath()

    fun setup() {
        activity.findThemedCategory(CATEGORY_KEY, viewThemeUtils)

        activity.findPreference(AppPreferencesImpl.DATA_STORAGE_LOCATION)?.setOnClickAction {
            activity.startExtendedSettingsDialog(ExtendedSettingsActivityDialog.StorageLocation)
        }

        setupThemePreference()
    }

    fun onThemeSelected(themeValue: String) {
        val mode = DarkMode.entries.firstOrNull { it.name == themeValue } ?: DarkMode.SYSTEM

        activity.findPreference(DARK_MODE_KEY)?.setSummary(mode.summaryResId)
    }

    fun onStorageLocationSelected(newPath: String) {
        if (storagePath == newPath) return

        StorageMigration(activity, user, storagePath, newPath, viewThemeUtils).run {
            setStorageMigrationProgressListener(this@GeneralSettingsCategory)
            migrate()
        }
    }

    override fun onStorageMigrationFinished(storagePath: String?, succeed: Boolean) {
        if (!succeed || storagePath == null) return

        saveStoragePath(storagePath)
    }

    override fun onCancelMigration() = Unit

    private fun setupThemePreference() {
        val themePreference = activity.findPreference(DARK_MODE_KEY) ?: return
        themePreference.setSummary(preferences.darkThemeMode.summaryResId)

        themePreference.setOnClickAction {
            activity.startExtendedSettingsDialog(ExtendedSettingsActivityDialog.ThemeSelection)
        }
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
