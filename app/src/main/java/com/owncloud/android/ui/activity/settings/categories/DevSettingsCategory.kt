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
import com.nextcloud.client.etm.EtmActivity
import com.nextcloud.client.utils.IntentUtil
import com.owncloud.android.R
import com.owncloud.android.ui.activity.fileactivity.DevVersionChecker
import com.owncloud.android.ui.activity.settings.util.bindOrRemove
import com.owncloud.android.ui.activity.settings.util.setOnClickAction
import com.owncloud.android.ui.asynctasks.LoadingVersionNumberTask.VersionDevInterface
import com.owncloud.android.utils.theme.ViewThemeUtils

class DevSettingsCategory(private val fragment: PreferenceFragmentCompat, private val viewThemeUtils: ViewThemeUtils) :
    SettingsCategory,
    VersionDevInterface {

    private val activity = fragment.requireActivity()

    override fun setup() {
        val category = fragment.findPreference<PreferenceCategory>(CATEGORY_KEY) ?: return
        val resources = activity.resources

        if (!resources.getBoolean(R.bool.is_beta)) {
            fragment.preferenceScreen.removePreference(category)
            return
        }

        viewThemeUtils.files.themePreferenceCategory(category)

        category.bindOrRemove(
            fragment.findPreference<Preference>(DEV_LINK_KEY),
            resources.getBoolean(R.bool.dev_version_direct_download_enabled)
        ) { DevVersionChecker.checkForNewDevVersion(this, activity.applicationContext) }

        fragment.findPreference<Preference>(CHANGELOG_LINK_KEY)?.setOnClickAction {
            IntentUtil.startLinkIntent(activity, R.string.dev_changelog)
        }

        fragment.findPreference<Preference>(ETM_KEY)?.setOnClickAction { EtmActivity.launch(activity) }
    }

    override fun returnVersion(latestVersion: Int) {
        DevVersionChecker.showDevSnackbar(activity, latestVersion, openDirectly = true, inBackground = false)
    }

    companion object {
        private const val CATEGORY_KEY = "dev_category"
        private const val DEV_LINK_KEY = "dev_link"
        private const val CHANGELOG_LINK_KEY = "changelog_link"
        private const val ETM_KEY = "etm"
    }
}
