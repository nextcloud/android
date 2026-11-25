/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.settings.more

import android.content.Intent
import android.preference.Preference
import android.preference.PreferenceActivity
import android.preference.PreferenceCategory
import androidx.annotation.BoolRes
import com.nextcloud.client.logger.ui.LogsActivity
import com.nextcloud.client.utils.IntentUtil
import com.nextcloud.utils.SnackbarUtil
import com.nextcloud.utils.mdm.MDMConfig
import com.owncloud.android.R
import com.owncloud.android.datamodel.ExternalLinksProvider
import com.owncloud.android.lib.common.ExternalLink
import com.owncloud.android.lib.common.ExternalLinkType
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.ContactsPreferenceActivity
import com.owncloud.android.ui.activity.settings.e2e.E2ESettings
import com.owncloud.android.ui.activity.settings.util.bindOrRemove
import com.owncloud.android.ui.activity.settings.util.findThemedCategory
import com.owncloud.android.ui.activity.settings.util.setOnClickAction
import com.owncloud.android.ui.activity.settings.util.startExternalSiteWebView
import com.owncloud.android.utils.MimeType
import com.owncloud.android.utils.theme.ViewThemeUtils

class MoreSettingsCategory(
    private val activity: PreferenceActivity,
    private val viewThemeUtils: ViewThemeUtils,
    private val e2eSettings: E2ESettings,
    private val davX5Launcher: DavX5Launcher
) {

    fun setup() {
        val category = activity.findThemedCategory(CATEGORY_KEY, viewThemeUtils) ?: return

        category.bindOrRemove(
            activity.findPreference(CALENDAR_CONTACTS_KEY),
            isEnabled(R.bool.davdroid_integration_enabled)
        ) { launchDavX5Login() }

        setupBackupPreference()
        e2eSettings.setup(category)

        category.bindOrRemove(activity.findPreference(HELP_KEY), isEnabled(R.bool.help_enabled)) {
            IntentUtil.startLinkIntent(activity, R.string.url_help)
        }

        category.bindOrRemove(activity.findPreference(RECOMMEND_KEY), isEnabled(R.bool.recommend_enabled)) {
            recommendApp()
        }

        category.bindOrRemove(activity.findPreference(LOGGER_KEY), MDMConfig.isLogEnabled(activity)) {
            activity.startActivity(Intent(activity.applicationContext, LogsActivity::class.java))
        }

        category.bindOrRemove(activity.findPreference(IMPRINT_KEY), isEnabled(R.bool.imprint_enabled)) {
            IntentUtil.startLinkIntent(activity, R.string.url_imprint)
        }

        loadExternalSettingLinks(category)
    }

    private fun isEnabled(@BoolRes flag: Int): Boolean = activity.resources.getBoolean(flag)

    @Suppress("TooGenericExceptionCaught")
    private fun launchDavX5Login() {
        try {
            davX5Launcher.launchLogin()
        } catch (t: Throwable) {
            Log_OC.e(TAG, "Error while setting up DavX5", t)
            SnackbarUtil.show(activity, R.string.prefs_davx5_setup_error)
        }
    }

    private fun setupBackupPreference() {
        val backup = activity.findPreference(BACKUP_KEY) ?: return
        val showCalendarBackup = isEnabled(R.bool.show_calendar_backup)

        backup.title = activity.getString(
            if (showCalendarBackup) R.string.backup_title else R.string.contact_backup_title
        )
        backup.summary = activity.getString(
            if (showCalendarBackup) R.string.prefs_daily_backup_summary else R.string.prefs_daily_contact_backup_summary
        )
        backup.setOnClickAction { ContactsPreferenceActivity.startActivityWithoutSidebar(activity) }
    }

    private fun recommendApp() {
        val appName = activity.getString(R.string.app_name)
        val downloadUrls = activity.getString(
            R.string.recommend_urls,
            activity.getString(R.string.url_app_download),
            activity.getString(R.string.fdroid_link)
        )

        val intent = Intent(Intent.ACTION_SEND)
            .setType(MimeType.TEXT_PLAIN)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.recommend_subject, appName))
            .putExtra(Intent.EXTRA_TEXT, activity.getString(R.string.recommend_text, appName, downloadUrls))

        activity.startActivity(intent)
    }

    private fun loadExternalSettingLinks(category: PreferenceCategory) {
        if (!MDMConfig.externalSiteSupport(activity)) return

        val externalLinksProvider = ExternalLinksProvider(activity.contentResolver)
        externalLinksProvider.getExternalLink(ExternalLinkType.SETTINGS) { links ->
            links.forEach { addExternalLinkPreference(category, it) }
        }

        externalLinksProvider.cleanup()
    }

    private fun addExternalLinkPreference(category: PreferenceCategory, link: ExternalLink) {
        val linkKey = link.id.toString()
        if (activity.findPreference(linkKey) != null) return

        val preference = Preference(activity).apply {
            title = link.name
            key = linkKey
            setOnClickAction { activity.startExternalSiteWebView(link.name, link.url) }
        }

        category.addPreference(preference)
    }

    companion object {
        private const val TAG = "MoreSettingsCategory"
        private const val CATEGORY_KEY = "more"
        private const val CALENDAR_CONTACTS_KEY = "calendar_contacts"
        private const val BACKUP_KEY = "backup"
        private const val HELP_KEY = "help"
        private const val RECOMMEND_KEY = "recommend"
        private const val LOGGER_KEY = "logger"
        private const val IMPRINT_KEY = "imprint"
    }
}
