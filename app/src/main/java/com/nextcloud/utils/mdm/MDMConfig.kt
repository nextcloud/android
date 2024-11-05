/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.mdm

import android.content.Context
import com.nextcloud.utils.extensions.getRestriction
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

        return shareViaLink && disableShareViaMDM
    }

    fun shareViaUser(context: Context): Boolean {
        val disableShareViaMDM = context.getRestriction(
            AppConfigKeys.DisableSharing,
            context.resources.getBoolean(R.bool.disable_sharing)
        )

        val shareViaUsers = context.resources.getBoolean(R.bool.share_with_users_feature)

        return shareViaUsers && disableShareViaMDM
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
}
