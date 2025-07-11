/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.mdm

import android.content.Context
import android.content.RestrictionsManager
import com.owncloud.android.R
import com.owncloud.android.utils.appConfig.AppConfigKeys

object MDMConfig {
    fun multiAccountSupport(context: Context): Boolean {
        val multiAccountSupport = context.resources.getBoolean(R.bool.multiaccount_support)

        val disableMultiAccountViaMDM = context.getRestriction(
            AppConfigKeys.DisableMultiAccount,
            context.resources.getBoolean(R.bool.disable_multiaccount)
        )

        return multiAccountSupport && !disableMultiAccountViaMDM
    }

    fun shareViaLink(context: Context): Boolean {
        val disableShareViaMDM = context.getRestriction(
            AppConfigKeys.DisableSharing,
            context.resources.getBoolean(R.bool.disable_sharing)
        )

        val shareViaLink = context.resources.getBoolean(R.bool.share_via_link_feature)

        return shareViaLink && !disableShareViaMDM
    }

    fun shareViaUser(context: Context): Boolean {
        val disableShareViaMDM = context.getRestriction(
            AppConfigKeys.DisableSharing,
            context.resources.getBoolean(R.bool.disable_sharing)
        )

        val shareViaUsers = context.resources.getBoolean(R.bool.share_with_users_feature)

        return shareViaUsers && !disableShareViaMDM
    }

    fun sendFilesSupport(context: Context): Boolean {
        val disableShareViaMDM = context.getRestriction(
            AppConfigKeys.DisableSharing,
            context.resources.getBoolean(R.bool.disable_sharing)
        )

        val sendFilesToOtherApp = "on".equals(context.getString(R.string.send_files_to_other_apps), ignoreCase = true)

        return sendFilesToOtherApp && !disableShareViaMDM
    }

    fun sharingSupport(context: Context): Boolean {
        val disableShareViaMDM = context.getRestriction(
            AppConfigKeys.DisableSharing,
            context.resources.getBoolean(R.bool.disable_sharing)
        )

        val sendFilesToOtherApp = "on".equals(context.getString(R.string.send_files_to_other_apps), ignoreCase = true)

        val shareViaUsers = context.resources.getBoolean(R.bool.share_with_users_feature)

        val shareViaLink = context.resources.getBoolean(R.bool.share_via_link_feature)

        return sendFilesToOtherApp && shareViaLink && shareViaUsers && !disableShareViaMDM
    }

    fun clipBoardSupport(context: Context): Boolean {
        val disableClipboardSupport = context.getRestriction(
            AppConfigKeys.DisableClipboard,
            context.resources.getBoolean(R.bool.disable_clipboard)
        )

        return !disableClipboardSupport
    }

    fun externalSiteSupport(context: Context): Boolean {
        val disableMoreExternalSiteViaMDM = context.getRestriction(
            AppConfigKeys.DisableMoreExternalSite,
            context.resources.getBoolean(R.bool.disable_more_external_site)
        )

        val showExternalLinks = context.resources.getBoolean(R.bool.show_external_links)

        return showExternalLinks && !disableMoreExternalSiteViaMDM
    }

    fun showIntro(context: Context): Boolean {
        val disableIntroViaMDM =
            context.getRestriction(AppConfigKeys.DisableIntro, context.resources.getBoolean(R.bool.disable_intro))

        val isProviderOrOwnInstallationVisible = context.resources.getBoolean(R.bool.show_provider_or_own_installation)

        return isProviderOrOwnInstallationVisible && !disableIntroViaMDM
    }

    fun isLogEnabled(context: Context): Boolean {
        val disableLogViaMDM =
            context.getRestriction(AppConfigKeys.DisableLog, context.resources.getBoolean(R.bool.disable_log))

        val loggerEnabled = context.resources.getBoolean(R.bool.logger_enabled)

        return loggerEnabled && !disableLogViaMDM
    }

    fun getBaseUrl(context: Context): String = context.getRestriction(AppConfigKeys.BaseUrl, "")

    fun getHost(context: Context): String =
        context.getRestriction(AppConfigKeys.ProxyHost, context.getString(R.string.proxy_host))

    fun getPort(context: Context): Int =
        context.getRestriction(AppConfigKeys.ProxyPort, context.resources.getInteger(R.integer.proxy_port))

    fun enforceProtection(context: Context): Boolean =
        context.getRestriction(AppConfigKeys.EnforceProtection, context.resources.getBoolean(R.bool.enforce_protection))

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> Context.getRestriction(appConfigKey: AppConfigKeys, defaultValue: T): T {
        val restrictionsManager = getSystemService(Context.RESTRICTIONS_SERVICE) as? RestrictionsManager
        val appRestrictions = restrictionsManager?.getApplicationRestrictions() ?: return defaultValue

        return when (defaultValue) {
            is String -> appRestrictions.getString(appConfigKey.key, defaultValue) as T? ?: defaultValue
            is Int -> appRestrictions.getInt(appConfigKey.key, defaultValue) as T? ?: defaultValue
            is Boolean -> appRestrictions.getBoolean(appConfigKey.key, defaultValue) as T? ?: defaultValue
            else -> defaultValue
        }
    }
}
