/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.settings.dev

import android.preference.PreferenceActivity
import android.preference.PreferenceCategory
import android.preference.PreferenceScreen
import com.nextcloud.client.etm.EtmActivity
import com.nextcloud.client.utils.IntentUtil
import com.owncloud.android.R
import com.owncloud.android.ui.activity.fileactivity.DevVersionChecker
import com.owncloud.android.ui.activity.settings.util.bindOrRemove
import com.owncloud.android.ui.activity.settings.util.findPreferenceAs
import com.owncloud.android.ui.activity.settings.util.setOnClickAction
import com.owncloud.android.ui.asynctasks.LoadingVersionNumberTask.VersionDevInterface
import com.owncloud.android.utils.theme.ViewThemeUtils

class DevSettingsCategory(private val activity: PreferenceActivity, private val viewThemeUtils: ViewThemeUtils) :
    VersionDevInterface {

    fun setup(preferenceScreen: PreferenceScreen) {
        val category = activity.findPreferenceAs<PreferenceCategory>(CATEGORY_KEY) ?: return
        val resources = activity.resources

        if (!resources.getBoolean(R.bool.is_beta)) {
            preferenceScreen.removePreference(category)
            return
        }

        viewThemeUtils.files.themePreferenceCategory(category)

        category.bindOrRemove(
            activity.findPreference(DEV_LINK_KEY),
            resources.getBoolean(R.bool.dev_version_direct_download_enabled)
        ) { DevVersionChecker.checkForNewDevVersion(this, activity.applicationContext) }

        activity.findPreference(CHANGELOG_LINK_KEY)?.setOnClickAction {
            IntentUtil.startLinkIntent(activity, R.string.dev_changelog)
        }

        activity.findPreference(ETM_KEY)?.setOnClickAction { EtmActivity.launch(activity) }
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
