/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.settings.categories

import android.content.Intent
import android.content.pm.PackageManager
import android.webkit.URLUtil
import androidx.annotation.BoolRes
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.nextcloud.client.utils.IntentUtil
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.settings.util.bindOrRemove
import com.owncloud.android.ui.activity.settings.util.findThemedCategory
import com.owncloud.android.ui.activity.settings.util.startExternalSiteWebView
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils

class AboutSettingsCategory(
    private val fragment: PreferenceFragmentCompat,
    private val viewThemeUtils: ViewThemeUtils
) : SettingsCategory {

    private val activity = fragment.requireActivity()

    private val appVersion: String
        get() = try {
            activity.packageManager.getPackageInfo(activity.packageName, 0).versionName.orEmpty()
        } catch (e: PackageManager.NameNotFoundException) {
            Log_OC.e(TAG, "Error while showing about dialog", e)
            ""
        }

    override fun setup() {
        val category = fragment.findThemedCategory(CATEGORY_KEY, viewThemeUtils) ?: return

        setupAboutAppPreference()

        category.bindOrRemove(fragment.findPreference<Preference>(LICENSE_KEY), isEnabled(R.bool.license_enabled)) {
            IntentUtil.startLinkIntent(activity, R.string.license_url)
        }?.setSummary(R.string.prefs_gpl_v2)

        val privacyEnabled = isEnabled(R.bool.privacy_enabled) &&
            URLUtil.isValidUrl(activity.getString(R.string.privacy_url))
        category.bindOrRemove(fragment.findPreference<Preference>(PRIVACY_KEY), privacyEnabled) {
            openPrivacyPolicy(category, it)
        }

        category.bindOrRemove(
            fragment.findPreference<Preference>(SOURCE_CODE_KEY),
            isEnabled(R.bool.sourcecode_enabled)
        ) { IntentUtil.startLinkIntent(activity, R.string.sourcecode_url) }
    }

    private fun isEnabled(@BoolRes flag: Int): Boolean = activity.resources.getBoolean(flag)

    private fun setupAboutAppPreference() {
        val aboutApp = fragment.findPreference<Preference>(ABOUT_APP_KEY) ?: return
        val version = appVersion
        val buildNumber = activity.getString(R.string.buildNumber)

        aboutApp.title = activity.getString(R.string.about_android, activity.getString(R.string.app_name))
        aboutApp.summary = if (buildNumber.isEmpty()) {
            activity.getString(R.string.about_version, version)
        } else {
            activity.getString(R.string.about_version_with_build, version, buildNumber)
        }
    }

    private fun openPrivacyPolicy(category: PreferenceCategory, privacyPreference: Preference) {
        val privacyUrl = activity.getString(R.string.privacy_url).toUri()
        val fileName = privacyUrl.lastPathSegment
        if (fileName == null) {
            Log_OC.e(TAG, "Could not parse privacy url")
            category.removePreference(privacyPreference)
            return
        }

        if (MimeTypeUtil.isPDF(MimeTypeUtil.getBestMimeTypeByFilename(fileName))) {
            val intent = Intent(Intent.ACTION_VIEW, privacyUrl)
            IntentUtil.startIntentIfAppAvailable(intent, activity, R.string.no_pdf_app_available)
            return
        }

        activity.startExternalSiteWebView(activity.getString(R.string.privacy), privacyUrl.toString())
    }

    companion object {
        private const val TAG = "AboutSettingsCategory"
        private const val CATEGORY_KEY = "about"
        private const val ABOUT_APP_KEY = "about_app"
        private const val LICENSE_KEY = "license"
        private const val PRIVACY_KEY = "privacy"
        private const val SOURCE_CODE_KEY = "sourcecode"
    }
}
