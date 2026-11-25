/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.settings.sync

import android.preference.PreferenceActivity
import android.preference.PreferenceCategory
import com.nextcloud.utils.extensions.openAllFilesAccessSettings
import com.owncloud.android.R
import com.owncloud.android.ui.activity.drawer.DrawerActivity
import com.owncloud.android.ui.activity.settings.util.bindOrRemove
import com.owncloud.android.ui.activity.settings.util.findPreferenceAs
import com.owncloud.android.ui.activity.settings.util.findThemedCategory
import com.owncloud.android.ui.activity.settings.util.setOnClickAction
import com.owncloud.android.ui.activity.settings.util.startNavigatorScreen
import com.owncloud.android.ui.navigation.NavigatorScreen
import com.owncloud.android.utils.PermissionUtil
import com.owncloud.android.utils.theme.ViewThemeUtils

class SyncSettingsCategory(private val activity: PreferenceActivity, private val viewThemeUtils: ViewThemeUtils) {

    fun setup() {
        val category = activity.findThemedCategory(CATEGORY_KEY, viewThemeUtils) ?: return

        category.bindOrRemove(
            activity.findPreference(AUTO_UPLOAD_KEY),
            !activity.resources.getBoolean(R.bool.syncedFolder_light)
        ) { activity.startNavigatorScreen(NavigatorScreen.AutoUpload) }

        activity.findPreference(TWO_WAY_SYNC_KEY)?.setOnClickAction {
            activity.startNavigatorScreen(NavigatorScreen.InternalTwoWaySync)
        }

        setupAllFilesAccessPreference(category)
    }

    fun refreshAllFilesAccessPreference() {
        val category = activity.findPreferenceAs<PreferenceCategory>(CATEGORY_KEY) ?: return

        setupAllFilesAccessPreference(category)
    }

    private fun setupAllFilesAccessPreference(category: PreferenceCategory) {
        val allFilesAccess = activity.findPreference(ALL_FILES_ACCESS_KEY) ?: return

        if (PermissionUtil.checkAllFilesAccess()) {
            category.removePreference(allFilesAccess)
        } else if (allFilesAccess.parent == null) {
            category.addPreference(allFilesAccess)
        }

        allFilesAccess.setOnClickAction {
            activity.openAllFilesAccessSettings(DrawerActivity.REQ_ALL_FILES_ACCESS)
        }
    }

    companion object {
        private const val CATEGORY_KEY = "sync"
        private const val AUTO_UPLOAD_KEY = "syncedFolders"
        private const val TWO_WAY_SYNC_KEY = "internal_two_way_sync"
        private const val ALL_FILES_ACCESS_KEY = "allFilesAccess"
    }
}
